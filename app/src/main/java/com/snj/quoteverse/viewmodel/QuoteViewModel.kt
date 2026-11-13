package com.snj.quoteverse.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.snj.quoteverse.data.api.RateLimitExceededException
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.data.repository.QuoteRepository
import com.snj.quoteverse.utils.CategoryProvider
import com.snj.quoteverse.utils.QuotePreferences
import com.snj.quoteverse.utils.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val featuredQuote: QuoteEntity? = null,
    val isDailyPick: Boolean = true,
    val selectedCategory: String = CategoryProvider.KEY_ALL,
    val trendingQuotes: List<QuoteEntity> = emptyList(),
    val recentlyViewedQuotes: List<QuoteEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true
)

data class ExploreUiState(
    val searchQuery: String = "",
    val selectedCategory: String = CategoryProvider.KEY_ALL,
    val quotes: List<QuoteEntity> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val totalQuotes: Int = 0
)

sealed interface SyncState {
    object Idle : SyncState
    object Loading : SyncState
    data class Success(val newQuotesCount: Int, val message: String) : SyncState
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val canRetry: Boolean = true
    ) : SyncState
}

/**
 * Shared MVVM ViewModel coordinating Home, Explore, Favorites, Quote Details, Settings,
 * and offline-first synchronization with the ZenQuotes API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModel(
    private val repository: QuoteRepository,
    private val preferences: QuotePreferences,
    private val networkMonitor: com.snj.quoteverse.utils.NetworkMonitor? = null
) : ViewModel() {

    private val _selectedHomeCategory = MutableStateFlow(CategoryProvider.KEY_ALL)
    private val _featuredQuoteId = MutableStateFlow<Long?>(null)
    private val _isInitialized = MutableStateFlow(false)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedExploreCategory = MutableStateFlow(CategoryProvider.KEY_ALL)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _selectedStyleIndex = MutableStateFlow(preferences.getSelectedDetailStyle())
    val selectedStyleIndex: StateFlow<Int> = _selectedStyleIndex.asStateFlow()

    val favoriteQuotes: StateFlow<List<QuoteEntity>> = repository.favoriteQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteCountLiveData: LiveData<Int> = repository.favoriteCount.asLiveData()

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.getThemeMode())

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            preferences.isNotificationsEnabled()
        )

    val notificationTime: StateFlow<Pair<Int, Int>> = preferences.notificationTime
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            preferences.getNotificationHour() to preferences.getNotificationMinute()
        )

    val appFont: StateFlow<com.snj.quoteverse.utils.FontOption> = preferences.appFont
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.getAppFont())

    val detailFontSize: StateFlow<Float> = preferences.detailFontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferences.getDetailFontSize())

    val totalQuoteCount: StateFlow<Int> = repository.allQuotes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val homeBaseFlow = combine(
        repository.allQuotes,
        repository.dailyQuote,
        _featuredQuoteId,
        _selectedHomeCategory,
        _isInitialized
    ) { allQuotes, dailyQuote, featuredId, selectedCategory, initialized ->
        HomeBaseSnapshot(allQuotes, dailyQuote, featuredId, selectedCategory, initialized)
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        homeBaseFlow,
        repository.recentlyViewedQuotes,
        _isOnline
    ) { snapshot, recentQuotes, online ->
        val allQuotes = snapshot.allQuotes
        val activeDaily = snapshot.dailyQuote ?: allQuotes.firstOrNull()
        val featured = snapshot.featuredQuoteId?.let { id ->
            allQuotes.firstOrNull { it.id == id }
        } ?: activeDaily

        val filteredTrendingSource = if (snapshot.selectedCategory.equals(CategoryProvider.KEY_ALL, ignoreCase = true)) {
            // Pick diverse representative quotes across categories for the "Trending" feed
            allQuotes.groupBy { it.category }
                .values
                .flatMap { categoryQuotes -> categoryQuotes.take(2) }
                .filter { it.id != featured?.id }
                .take(15)
        } else {
            allQuotes.filter {
                it.category.equals(snapshot.selectedCategory, ignoreCase = true) && it.id != featured?.id
            }
        }

        HomeUiState(
            featuredQuote = featured,
            isDailyPick = featured != null && activeDaily != null && featured.id == activeDaily.id,
            selectedCategory = snapshot.selectedCategory,
            trendingQuotes = filteredTrendingSource,
            recentlyViewedQuotes = recentQuotes,
            isLoading = !snapshot.initialized && allQuotes.isEmpty(),
            isOnline = online
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val filteredExploreQuotes: Flow<List<QuoteEntity>> = combine(
        _searchQuery,
        _selectedExploreCategory
    ) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        repository.observeQuotes(query, category)
    }

    val exploreUiState: StateFlow<ExploreUiState> = combine(
        _searchQuery,
        _selectedExploreCategory,
        filteredExploreQuotes,
        repository.allQuotes
    ) { query, category, quotes, allQuotes ->
        val counts = allQuotes.groupingBy { it.category }.eachCount()
        ExploreUiState(
            searchQuery = query,
            selectedCategory = category,
            quotes = quotes,
            categoryCounts = counts,
            totalQuotes = allQuotes.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    init {
        if (networkMonitor != null) {
            viewModelScope.launch {
                networkMonitor.isOnline.collect { online ->
                    _isOnline.value = online
                }
            }
        }
        viewModelScope.launch {
            repository.ensureInitialized()
            _isInitialized.value = true

            // Soft-sync today's quote from ZenQuotes if network is available
            try {
                repository.syncTodayQuoteFromApi(force = false)
            } catch (_: Exception) {
                // Offline fallback already loaded seamlessly from Room
            }
        }
    }

    fun selectHomeCategory(category: String) {
        _selectedHomeCategory.value = category
    }

    /**
     * Generates a random quote from the rich local Room cache (seed quotes + fetched batches).
     * This avoids depleting the ZenQuotes 5 requests / 30s limit while providing instant offline UX.
     */
    fun generateNewRandomQuote() {
        viewModelScope.launch {
            val currentId = homeUiState.value.featuredQuote?.id
            val category = _selectedHomeCategory.value
            val randomQuote = repository.getRandomQuote(excludeId = currentId, category = category)
            if (randomQuote != null) {
                _featuredQuoteId.value = randomQuote.id
                repository.recordQuoteViewed(randomQuote.id)
            }
        }
    }

    /**
     * Fetches a batch of 50 quotes from ZenQuotes (/api/quotes),
     * deduplicates them, and caches new quotes in Room.
     */
    fun syncQuotesBatch() {
        if (_syncState.value is SyncState.Loading) return
        _syncState.value = SyncState.Loading

        viewModelScope.launch {
            val result = repository.fetchQuotesBatchFromApi()
            result.fold(
                onSuccess = { newCount ->
                    val message = if (newCount > 0) {
                        "Synced $newCount new quotes from ZenQuotes"
                    } else {
                        "All ZenQuotes batch quotes are already cached"
                    }
                    _syncState.value = SyncState.Success(newCount, message)
                },
                onFailure = { error ->
                    val isRateLimited = error is RateLimitExceededException
                    val message = if (isRateLimited) {
                        error.message ?: "ZenQuotes rate limit (5 req/30s) reached. Please wait."
                    } else {
                        "Network unavailable. Showing local quotes."
                    }
                    _syncState.value = SyncState.Error(message, isRateLimited = isRateLimited, canRetry = !isRateLimited)
                }
            )
        }
    }

    /**
     * Explicitly syncs today's quote from ZenQuotes (/api/today).
     */
    fun syncTodayQuote(force: Boolean = true) {
        if (_syncState.value is SyncState.Loading) return
        _syncState.value = SyncState.Loading

        viewModelScope.launch {
            val result = repository.syncTodayQuoteFromApi(force = force)
            result.fold(
                onSuccess = { quote ->
                    if (quote != null) {
                        _featuredQuoteId.value = quote.id
                    }
                    _syncState.value = SyncState.Success(
                        1,
                        "Quote of the Day updated from ZenQuotes"
                    )
                },
                onFailure = { error ->
                    val isRateLimited = error is RateLimitExceededException
                    val message = if (isRateLimited) {
                        error.message ?: "ZenQuotes rate limit reached. Please wait."
                    } else {
                        "Unable to reach ZenQuotes. Using offline daily quote."
                    }
                    _syncState.value = SyncState.Error(message, isRateLimited = isRateLimited, canRetry = !isRateLimited)
                }
            )
        }
    }

    /**
     * Fetches a single random quote directly from ZenQuotes (/api/random),
     * with automatic fallback to cached Room quotes if rate limited or offline.
     */
    fun fetchOnlineOrCachedRandomQuote() {
        viewModelScope.launch {
            val result = repository.fetchRandomQuoteFromApi()
            result.fold(
                onSuccess = { quote ->
                    _featuredQuoteId.value = quote.id
                    repository.recordQuoteViewed(quote.id)
                },
                onFailure = {
                    // Seamless offline / rate-limit fallback to local Room quotes
                    generateNewRandomQuote()
                }
            )
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectExploreCategory(category: String) {
        _selectedExploreCategory.value = category
    }

    fun toggleFavorite(quote: QuoteEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(quote)
        }
    }

    fun setFavorite(quoteId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(quoteId, isFavorite)
        }
    }

    fun observeQuoteById(quoteId: Long): Flow<QuoteEntity?> = repository.getQuoteById(quoteId)

    fun recordQuoteViewed(quoteId: Long) {
        repository.recordQuoteViewed(quoteId)
    }

    fun selectDetailStyle(styleIndex: Int) {
        _selectedStyleIndex.value = styleIndex
        preferences.setSelectedDetailStyle(styleIndex)
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
    }

    fun setAppFont(font: com.snj.quoteverse.utils.FontOption) {
        preferences.setAppFont(font)
    }

    fun setDetailFontSize(sizeSp: Float) {
        preferences.setDetailFontSize(sizeSp)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        preferences.setNotificationTime(hour, minute)
    }

    private data class HomeBaseSnapshot(
        val allQuotes: List<QuoteEntity>,
        val dailyQuote: QuoteEntity?,
        val featuredQuoteId: Long?,
        val selectedCategory: String,
        val initialized: Boolean
    )
}
