package com.snj.quoteverse.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.snj.quoteverse.MainActivity
import com.snj.quoteverse.QuoteVerseApplication
import com.snj.quoteverse.R
import com.snj.quoteverse.adapters.CategoryAdapter
import com.snj.quoteverse.adapters.CategoryUiModel
import com.snj.quoteverse.adapters.QuoteAdapter
import com.snj.quoteverse.adapters.RecentQuoteAdapter
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.FragmentHomeBinding
import com.snj.quoteverse.utils.CategoryProvider
import com.snj.quoteverse.utils.QuoteActionUtils
import com.snj.quoteverse.utils.applyTopSystemBarPadding
import com.snj.quoteverse.utils.copyQuoteWithFeedback
import com.snj.quoteverse.utils.showQuoteSnackbar
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.QuoteViewModelFactory
import com.snj.quoteverse.viewmodel.SyncState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Home screen presenting the greeting header, Daily Quote card, categories,
 * recently viewed picks, trending quotes, and ZenQuotes API sync.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuoteViewModel by activityViewModels {
        val app = requireActivity().application as QuoteVerseApplication
        QuoteViewModelFactory(app.repository, app.preferencesManager, app.networkMonitor)
    }

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recentAdapter: RecentQuoteAdapter
    private lateinit var trendingAdapter: QuoteAdapter

    private var currentFeaturedQuote: QuoteEntity? = null
    private var lastAnimatedQuoteId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.homeScrollView.applyTopSystemBarPadding()
        binding.tvGreeting.setText(QuoteActionUtils.getGreetingResId())

        setupRecyclerViews()
        setupDailyQuoteActions()
        setupSyncAndAttribution()
        observeUiState()
        observeSyncState()
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectHomeCategory(category.key)
        }
        binding.rvHomeCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
        }

        recentAdapter = RecentQuoteAdapter(
            onQuoteClick = { openQuoteDetails(it.id) },
            onFavoriteClick = { handleFavoriteToggle(it) },
            fontProvider = { viewModel.appFont.value }
        )
        binding.rvRecentlyViewed.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        trendingAdapter = QuoteAdapter(
            onQuoteClick = { openQuoteDetails(it.id) },
            onFavoriteClick = { handleFavoriteToggle(it) },
            onCopyClick = { copyQuoteWithFeedback(requireContext(), it) },
            onShareClick = { QuoteActionUtils.shareQuote(requireContext(), it) },
            fontProvider = { viewModel.appFont.value }
        )
        binding.rvTrendingQuotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trendingAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupDailyQuoteActions() {
        binding.cardDailyQuote.setOnClickListener {
            currentFeaturedQuote?.let { openQuoteDetails(it.id) }
        }
        binding.btnNewQuote.setOnClickListener {
            binding.btnNewQuote.animate()
                .rotationBy(360f)
                .setDuration(350)
                .start()
            viewModel.fetchOnlineOrCachedRandomQuote()
        }
        binding.btnDailyFavorite.setOnClickListener { button ->
            val quote = currentFeaturedQuote ?: return@setOnClickListener
            button.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(110)
                .withEndAction {
                    button.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                }
                .start()
            handleFavoriteToggle(quote)
        }
        binding.btnDailyCopy.setOnClickListener {
            currentFeaturedQuote?.let { copyQuoteWithFeedback(requireContext(), it) }
        }
        binding.btnDailyShare.setOnClickListener {
            currentFeaturedQuote?.let { QuoteActionUtils.shareQuote(requireContext(), it) }
        }
    }

    private fun setupSyncAndAttribution() {
        binding.btnHomeSync.setOnClickListener {
            viewModel.syncQuotesBatch()
        }

        binding.btnOfflineRetry.setOnClickListener {
            viewModel.syncQuotesBatch()
        }

        binding.tvHomeAttribution.setOnClickListener {
            showAttributionDialog()
        }
    }

    private fun showAttributionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.attribution_dialog_title)
            .setMessage(R.string.attribution_dialog_message)
            .setPositiveButton(R.string.action_visit_website) { _, _ ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zenquotes.io/"))
                startActivity(browserIntent)
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeUiState.collect { state ->
                    bindFeaturedQuote(state.featuredQuote, state.isDailyPick)

                    val categoryModels = CategoryProvider.filterCategories.map { item ->
                        CategoryUiModel(
                            item = item,
                            isSelected = item.key.equals(state.selectedCategory, ignoreCase = true)
                        )
                    }
                    categoryAdapter.submitList(categoryModels)

                    binding.bannerOffline.visibility =
                        if (state.isOnline) View.GONE else View.VISIBLE

                    binding.sectionRecentlyViewed.visibility =
                        if (state.recentlyViewedQuotes.isEmpty()) View.GONE else View.VISIBLE
                    recentAdapter.submitList(state.recentlyViewedQuotes)

                    trendingAdapter.submitList(state.trendingQuotes)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appFont.collect { font ->
                    binding.tvDailyQuoteText.typeface = font.typeface
                    trendingAdapter.notifyDataSetChanged()
                    recentAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun observeSyncState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncState.collect { state ->
                    when (state) {
                        is SyncState.Loading -> {
                            binding.ivHomeSyncIcon.visibility = View.GONE
                            binding.pbHomeSync.visibility = View.VISIBLE
                            binding.tvHomeSyncText.text = getString(R.string.action_sync) + "…"
                            binding.btnHomeSync.isEnabled = false
                        }
                        is SyncState.Success -> {
                            binding.ivHomeSyncIcon.visibility = View.VISIBLE
                            binding.pbHomeSync.visibility = View.GONE
                            binding.tvHomeSyncText.setText(R.string.action_sync)
                            binding.btnHomeSync.isEnabled = true
                            showQuoteSnackbar(state.message)
                            viewModel.resetSyncState()
                        }
                        is SyncState.Error -> {
                            binding.ivHomeSyncIcon.visibility = View.VISIBLE
                            binding.pbHomeSync.visibility = View.GONE
                            binding.tvHomeSyncText.setText(R.string.action_sync)
                            binding.btnHomeSync.isEnabled = true
                            val snackbar = com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                state.message,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            )
                            if (state.canRetry) {
                                snackbar.setAction(R.string.action_retry) {
                                    viewModel.syncQuotesBatch()
                                }
                            }
                            snackbar.show()
                            viewModel.resetSyncState()
                        }
                        is SyncState.Idle -> {
                            binding.ivHomeSyncIcon.visibility = View.VISIBLE
                            binding.pbHomeSync.visibility = View.GONE
                            binding.tvHomeSyncText.setText(R.string.action_sync)
                            binding.btnHomeSync.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun bindFeaturedQuote(quote: QuoteEntity?, isDailyPick: Boolean) {
        currentFeaturedQuote = quote
        if (quote == null) return

        val shouldAnimateText = lastAnimatedQuoteId != null && lastAnimatedQuoteId != quote.id
        lastAnimatedQuoteId = quote.id

        binding.tvDailyBadge.setText(
            if (isDailyPick) R.string.badge_quote_of_the_day else R.string.badge_curated_pick
        )
        val formattedDate = java.text.SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(java.util.Date())
        binding.tvDailyDate.text = formattedDate

        val categoryItem = CategoryProvider.findByKey(quote.category)
        binding.tvDailyCategory.text = getString(categoryItem.labelRes).uppercase(Locale.getDefault())
        binding.tvDailyQuoteText.typeface = viewModel.appFont.value.typeface

        if (shouldAnimateText) {
            binding.tvDailyQuoteText.alpha = 0f
            binding.tvDailyAuthor.alpha = 0f
            binding.tvDailyQuoteText.text = quote.quoteText
            binding.tvDailyAuthor.text = getString(R.string.author_prefix, quote.author)
            binding.tvDailyQuoteText.animate().alpha(1f).setDuration(240).start()
            binding.tvDailyAuthor.animate().alpha(1f).setDuration(240).start()
        } else {
            binding.tvDailyQuoteText.text = quote.quoteText
            binding.tvDailyAuthor.text = getString(R.string.author_prefix, quote.author)
        }

        binding.btnDailyFavorite.setImageResource(
            if (quote.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )
        val favTint = ContextCompat.getColor(
            requireContext(),
            if (quote.isFavorite) R.color.qv_favorite_active else android.R.color.white
        )
        binding.btnDailyFavorite.setColorFilter(favTint)
    }

    private fun handleFavoriteToggle(quote: QuoteEntity) {
        val willBeFavorite = !quote.isFavorite
        viewModel.toggleFavorite(quote)
        showQuoteSnackbar(
            if (willBeFavorite) R.string.snackbar_added_favorite else R.string.snackbar_removed_favorite
        )
    }

    private fun openQuoteDetails(quoteId: Long) {
        findNavController().navigate(
            R.id.action_to_quoteDetails,
            bundleOf(MainActivity.ARG_QUOTE_ID to quoteId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
