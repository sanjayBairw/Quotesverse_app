package com.snj.quoteverse.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.snj.quoteverse.MainActivity
import com.snj.quoteverse.QuoteVerseApplication
import com.snj.quoteverse.R
import com.snj.quoteverse.adapters.QuoteAdapter
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.FragmentFavoritesBinding
import com.snj.quoteverse.utils.QuoteActionUtils
import com.snj.quoteverse.utils.applyTopSystemBarPadding
import com.snj.quoteverse.utils.copyQuoteWithFeedback
import com.snj.quoteverse.utils.showQuoteSnackbar
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.QuoteViewModelFactory
import kotlinx.coroutines.launch

/**
 * Favorites screen displaying locally persisted favorite quotes with undoable removal
 * and an illustrated empty state.
 */
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuoteViewModel by activityViewModels {
        val app = requireActivity().application as QuoteVerseApplication
        QuoteViewModelFactory(app.repository, app.preferencesManager)
    }

    private lateinit var favoritesAdapter: QuoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyTopSystemBarPadding()

        setupRecyclerView()
        setupEmptyState()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = QuoteAdapter(
            onQuoteClick = { openQuoteDetails(it.id) },
            onFavoriteClick = { removeFavoriteWithUndo(it) },
            onCopyClick = { copyQuoteWithFeedback(requireContext(), it) },
            onShareClick = { QuoteActionUtils.shareQuote(requireContext(), it) },
            fontProvider = { viewModel.appFont.value }
        )
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun setupEmptyState() {
        binding.btnExploreQuotes.setOnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            if (bottomNav != null) {
                bottomNav.selectedItemId = R.id.exploreFragment
            } else {
                findNavController().navigate(R.id.exploreFragment)
            }
        }
    }

    private fun observeFavorites() {
        viewModel.favoriteCountLiveData.observe(viewLifecycleOwner) { count ->
            binding.tvFavoritesSubtitle.text = when (count) {
                0 -> getString(R.string.favorites_count_zero)
                1 -> getString(R.string.favorites_count_single)
                else -> getString(R.string.favorites_count_multiple, count)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteQuotes.collect { quotes ->
                    favoritesAdapter.submitList(quotes)
                    val isEmpty = quotes.isEmpty()
                    binding.rvFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.emptyFavoritesContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appFont.collect {
                    favoritesAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun removeFavoriteWithUndo(quote: QuoteEntity) {
        viewModel.setFavorite(quote.id, false)
        showQuoteSnackbar(
            messageRes = R.string.snackbar_removed_favorite,
            actionTextRes = R.string.action_undo
        ) {
            viewModel.setFavorite(quote.id, true)
        }
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

