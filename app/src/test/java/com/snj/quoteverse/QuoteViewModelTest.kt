package com.snj.quoteverse

import com.snj.quoteverse.data.api.ZenQuotesRateLimiter
import com.snj.quoteverse.data.repository.QuoteRepository
import com.snj.quoteverse.utils.ThemeMode
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(): QuoteViewModel {
        val dao = FakeQuoteDao()
        val prefs = FakeQuotePreferences()
        val repo = QuoteRepository(dao, prefs)
        return QuoteViewModel(repo, prefs)
    }

    @Test
    fun homeUiStateLoadsDailyQuoteAndTrendingQuotes() = runTest {
        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeUiState.collect()
        }

        val state = viewModel.homeUiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.featuredQuote)
        assertTrue(state.trendingQuotes.isNotEmpty())
        job.cancel()
    }

    @Test
    fun generateNewRandomQuoteChangesFeaturedQuoteAndRecordsRecent() = runTest {
        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeUiState.collect()
        }

        val initialId = viewModel.homeUiState.value.featuredQuote?.id
        assertNotNull(initialId)

        viewModel.generateNewRandomQuote()
        val updatedState = viewModel.homeUiState.value
        assertNotNull(updatedState.featuredQuote)
        assertNotEquals(initialId, updatedState.featuredQuote?.id)
        assertTrue(updatedState.recentlyViewedQuotes.any { it.id == updatedState.featuredQuote?.id })
        job.cancel()
    }

    @Test
    fun exploreFiltersByCategoryAndSearchQuery() = runTest {
        val viewModel = createViewModel()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.exploreUiState.collect()
        }

        viewModel.selectExploreCategory("Study")
        val studyState = viewModel.exploreUiState.value
        assertTrue(studyState.quotes.isNotEmpty())
        assertTrue(studyState.quotes.all { it.category == "Study" })

        viewModel.updateSearchQuery("Gandhi")
        val filteredState = viewModel.exploreUiState.value
        assertTrue(filteredState.quotes.isNotEmpty())
        assertTrue(
            filteredState.quotes.all {
                it.category == "Study" && it.author.contains("Gandhi", ignoreCase = true)
            }
        )
        job.cancel()
    }

    @Test
    fun toggleFavoriteUpdatesFavoritesFlowAndSettingsPersistPreferences() = runTest {
        val viewModel = createViewModel()
        val homeJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeUiState.collect()
        }
        val favJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favoriteQuotes.collect()
        }

        val featured = viewModel.homeUiState.value.featuredQuote!!
        assertFalse(featured.isFavorite)

        viewModel.toggleFavorite(featured)
        assertTrue(viewModel.favoriteQuotes.value.any { it.id == featured.id })

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        viewModel.setNotificationTime(7, 30)
        assertEquals(7 to 30, viewModel.notificationTime.value)

        viewModel.setAppFont(com.snj.quoteverse.utils.FontOption.MONOSPACE)
        assertEquals(com.snj.quoteverse.utils.FontOption.MONOSPACE, viewModel.appFont.value)

        viewModel.setDetailFontSize(30f)
        assertEquals(30f, viewModel.detailFontSize.value)

        homeJob.cancel()
        favJob.cancel()
    }

    @Test
    fun syncQuotesBatchUpdatesSyncStateAndQuoteCount() = runTest {
        val dao = FakeQuoteDao()
        val prefs = FakeQuotePreferences()
        val api = FakeZenQuotesApiService()
        val rateLimiter = ZenQuotesRateLimiter()
        val repo = QuoteRepository(dao, prefs, api, rateLimiter)
        val viewModel = QuoteViewModel(repo, prefs)

        val syncJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.syncState.collect()
        }

        viewModel.syncQuotesBatch()
        assertTrue(viewModel.syncState.value is SyncState.Success)
        val success = viewModel.syncState.value as SyncState.Success
        assertEquals(50, success.newQuotesCount)

        viewModel.resetSyncState()
        assertEquals(SyncState.Idle, viewModel.syncState.value)

        syncJob.cancel()
    }

    @Test
    fun fetchOnlineOrCachedRandomQuoteUpdatesFeaturedQuote() = runTest {
        val dao = FakeQuoteDao()
        val prefs = FakeQuotePreferences()
        val api = FakeZenQuotesApiService().apply {
            randomQuotesToReturn = listOf(
                com.snj.quoteverse.data.api.ZenQuoteDto(
                    quoteText = "Wisdom begins in wonder.",
                    author = "Socrates"
                )
            )
        }
        val rateLimiter = ZenQuotesRateLimiter()
        val repo = QuoteRepository(dao, prefs, api, rateLimiter)
        val viewModel = QuoteViewModel(repo, prefs)

        val homeJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeUiState.collect()
        }

        viewModel.fetchOnlineOrCachedRandomQuote()
        val state = viewModel.homeUiState.value
        assertEquals("Wisdom begins in wonder.", state.featuredQuote?.quoteText)
        assertEquals("Socrates", state.featuredQuote?.author)

        homeJob.cancel()
    }
}

