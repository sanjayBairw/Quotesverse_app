package com.snj.quoteverse.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.snj.quoteverse.MainActivity
import com.snj.quoteverse.QuoteVerseApplication
import com.snj.quoteverse.R
import com.snj.quoteverse.adapters.CategoryAdapter
import com.snj.quoteverse.adapters.CategoryUiModel
import com.snj.quoteverse.adapters.QuoteAdapter
import com.snj.quoteverse.adapters.RecentQuoteAdapter
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.FragmentExploreBinding
import com.snj.quoteverse.utils.CategoryProvider
import com.snj.quoteverse.utils.QuoteActionUtils
import com.snj.quoteverse.utils.applyTopSystemBarPadding
import com.snj.quoteverse.utils.copyQuoteWithFeedback
import com.snj.quoteverse.utils.showQuoteSnackbar
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.QuoteViewModelFactory
import kotlinx.coroutines.launch

/**
 * Explore screen supporting live search by quote or author and filtering across all 10 categories.
 */
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuoteViewModel by activityViewModels {
        val app = requireActivity().application as QuoteVerseApplication
        QuoteViewModelFactory(app.repository, app.preferencesManager)
    }

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var quoteAdapter: QuoteAdapter
    private lateinit var recentAdapter: RecentQuoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyTopSystemBarPadding()

        setupSearchBar()
        setupRecyclerViews()
        setupEmptyStateAction()
        observeExploreState()
    }

    private fun setupSearchBar() {
        val initialQuery = viewModel.exploreUiState.value.searchQuery
        if (binding.etSearch.text?.toString() != initialQuery) {
            binding.etSearch.setText(initialQuery)
        }
        binding.btnClearSearch.visibility = if (initialQuery.isNotEmpty()) View.VISIBLE else View.GONE

        binding.etSearch.doAfterTextChanged { editable ->
            val text = editable?.toString().orEmpty()
            binding.btnClearSearch.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
            viewModel.updateSearchQuery(text)
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectExploreCategory(category.key)
        }
        binding.rvExploreCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
        }

        recentAdapter = com.snj.quoteverse.adapters.RecentQuoteAdapter(
            onQuoteClick = { openQuoteDetails(it.id) },
            onFavoriteClick = { handleFavoriteToggle(it) },
            fontProvider = { viewModel.appFont.value }
        )
        binding.rvExploreRecent.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        quoteAdapter = QuoteAdapter(
            onQuoteClick = { openQuoteDetails(it.id) },
            onFavoriteClick = { handleFavoriteToggle(it) },
            onCopyClick = { copyQuoteWithFeedback(requireContext(), it) },
            onShareClick = { QuoteActionUtils.shareQuote(requireContext(), it) },
            fontProvider = { viewModel.appFont.value }
        )
        binding.rvExploreQuotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quoteAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupEmptyStateAction() {
        binding.btnResetFilters.setOnClickListener {
            binding.etSearch.text?.clear()
            viewModel.selectExploreCategory(CategoryProvider.KEY_ALL)
        }
    }

    private fun observeExploreState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exploreUiState.collect { state ->
                    val categoryModels = CategoryProvider.filterCategories.map { item ->
                        val count = if (item.key == CategoryProvider.KEY_ALL) {
                            state.totalQuotes
                        } else {
                            state.categoryCounts[item.key] ?: 0
                        }
                        CategoryUiModel(
                            item = item,
                            isSelected = item.key.equals(state.selectedCategory, ignoreCase = true),
                            count = count
                        )
                    }
                    categoryAdapter.submitList(categoryModels)
                    quoteAdapter.submitList(state.quotes)

                    val count = state.quotes.size
                    binding.tvExploreCount.text = if (count == 1) {
                        getString(R.string.quote_count_single)
                    } else {
                        getString(R.string.quote_count_format, count)
                    }

                    val isEmpty = state.quotes.isEmpty()
                    binding.exploreScrollView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.emptyExploreContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE

                    val hasQuery = state.searchQuery.isNotBlank()
                    binding.tvExploreSectionTitle.text = when {
                        hasQuery -> "Results for \"${state.searchQuery}\""
                        state.selectedCategory.equals(CategoryProvider.KEY_ALL, ignoreCase = true) -> "Trending & Timeless Quotes"
                        else -> "Trending in ${state.selectedCategory}"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeUiState.collect { homeState ->
                    val query = viewModel.exploreUiState.value.searchQuery
                    val showRecent = query.isBlank() && homeState.recentlyViewedQuotes.isNotEmpty()
                    binding.sectionExploreRecentlyViewed.visibility = if (showRecent) View.VISIBLE else View.GONE
                    recentAdapter.submitList(homeState.recentlyViewedQuotes)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appFont.collect {
                    quoteAdapter.notifyDataSetChanged()
                    recentAdapter.notifyDataSetChanged()
                }
            }
        }
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

