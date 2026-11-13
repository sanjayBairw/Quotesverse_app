package com.snj.quoteverse.ui.details

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.snj.quoteverse.adapters.StyleSwatchAdapter
import com.snj.quoteverse.adapters.StyleSwatchUiModel
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.FragmentQuoteDetailsBinding
import com.snj.quoteverse.utils.CategoryProvider
import com.snj.quoteverse.utils.FontOption
import com.snj.quoteverse.utils.GradientStyle
import com.snj.quoteverse.utils.GradientStyleProvider
import com.snj.quoteverse.utils.ImageExportUtils
import com.snj.quoteverse.utils.QuoteActionUtils
import com.snj.quoteverse.utils.applyBottomSystemBarMargin
import com.snj.quoteverse.utils.applyTopSystemBarPadding
import com.snj.quoteverse.utils.copyQuoteWithFeedback
import com.snj.quoteverse.utils.showQuoteSnackbar
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.QuoteViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Full-screen immersive Quote Details presentation with customizable gradient styles,
 * typography options, dynamic font-size adjustment, favorite toggle, text copy,
 * social image sharing, and high-resolution 1080x1350 PNG export.
 */
class QuoteDetailsFragment : Fragment() {

    private var _binding: FragmentQuoteDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuoteViewModel by activityViewModels {
        val app = requireActivity().application as QuoteVerseApplication
        QuoteViewModelFactory(app.repository, app.preferencesManager, app.networkMonitor)
    }

    private lateinit var swatchAdapter: StyleSwatchAdapter
    private var currentQuote: QuoteEntity? = null
    private var currentStyle: GradientStyle = GradientStyleProvider.styles.first()
    private var selectedFont: FontOption = FontOption.SERIF
    private var currentFontSizeSp: Float = 26f

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportCurrentQuoteAsImage()
        } else {
            showQuoteSnackbar(R.string.snackbar_storage_permission_denied)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuoteDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.detailsTopBar.applyTopSystemBarPadding()
        binding.detailsActionCard.applyBottomSystemBarMargin()

        val quoteId = arguments?.getLong(MainActivity.ARG_QUOTE_ID, -1L) ?: -1L
        if (quoteId <= 0L) {
            showQuoteSnackbar(R.string.details_quote_not_found)
            findNavController().navigateUp()
            return
        }

        if (savedInstanceState == null) {
            viewModel.recordQuoteViewed(quoteId)
        }

        setupStylePicker()
        setupTypographyControls()
        setupActions()
        observeQuoteAndSettings(quoteId)
    }

    private fun setupStylePicker() {
        swatchAdapter = StyleSwatchAdapter { style ->
            viewModel.selectDetailStyle(style.id)
        }
        binding.rvStyleSwatches.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = swatchAdapter
            itemAnimator = null
        }
    }

    private fun setupTypographyControls() {
        binding.btnFontSerif.typeface = FontOption.SERIF.typeface
        binding.btnFontSans.typeface = FontOption.SANS.typeface
        binding.btnFontMono.typeface = FontOption.MONOSPACE.typeface
        binding.btnFontCasual.typeface = FontOption.CASUAL.typeface

        binding.btnFontSerif.setOnClickListener { updateFont(FontOption.SERIF) }
        binding.btnFontSans.setOnClickListener { updateFont(FontOption.SANS) }
        binding.btnFontMono.setOnClickListener { updateFont(FontOption.MONOSPACE) }
        binding.btnFontCasual.setOnClickListener { updateFont(FontOption.CASUAL) }

        binding.btnFontSizeDown.setOnClickListener {
            val nextSize = (currentFontSizeSp - 2f).coerceAtLeast(18f)
            updateFontSize(nextSize)
        }

        binding.btnFontSizeUp.setOnClickListener {
            val nextSize = (currentFontSizeSp + 2f).coerceAtMost(36f)
            updateFontSize(nextSize)
        }
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.actionFavorite.setOnClickListener {
            val quote = currentQuote ?: return@setOnClickListener
            val willBeFavorite = !quote.isFavorite
            binding.ivDetailsFavorite.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(110)
                .withEndAction {
                    binding.ivDetailsFavorite.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                }
                .start()
            viewModel.toggleFavorite(quote)
            showQuoteSnackbar(
                if (willBeFavorite) R.string.snackbar_added_favorite else R.string.snackbar_removed_favorite
            )
        }

        binding.actionCopy.setOnClickListener {
            currentQuote?.let { copyQuoteWithFeedback(requireContext(), it) }
        }

        binding.actionShare.setOnClickListener {
            showShareOptionsDialog()
        }

        binding.actionSaveImage.setOnClickListener {
            handleSaveImageClick()
        }
    }

    private fun showShareOptionsDialog() {
        val quote = currentQuote ?: return
        val options = arrayOf(
            getString(R.string.dialog_share_as_image),
            getString(R.string.dialog_share_as_text)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_share_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareQuoteImagePoster()
                    1 -> QuoteActionUtils.shareQuote(requireContext(), quote)
                }
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun shareQuoteImagePoster() {
        val quote = currentQuote ?: return
        val style = currentStyle
        val font = selectedFont
        val sizeMultiplier = currentFontSizeSp / 26f

        binding.actionShare.isEnabled = false
        showQuoteSnackbar(R.string.share_image_preparing)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = ImageExportUtils.createQuoteImageForShare(
                context = requireContext(),
                quote = quote,
                style = style,
                fontOption = font,
                fontSizeMultiplier = sizeMultiplier
            )
            if (_binding != null) {
                binding.actionShare.isEnabled = true
                result.fold(
                    onSuccess = { uri ->
                        ImageExportUtils.shareQuoteImage(requireContext(), uri, quote)
                    },
                    onFailure = {
                        showQuoteSnackbar(R.string.share_image_error)
                    }
                )
            }
        }
    }

    private fun updateFont(font: FontOption) {
        selectedFont = font
        binding.tvDetailsQuoteText.typeface = font.typeface
        highlightFontButton(font)
        viewModel.setAppFont(font)
    }

    private fun highlightFontButton(font: FontOption) {
        val buttons = listOf(
            FontOption.SERIF to binding.btnFontSerif,
            FontOption.SANS to binding.btnFontSans,
            FontOption.MONOSPACE to binding.btnFontMono,
            FontOption.CASUAL to binding.btnFontCasual
        )

        for ((option, button) in buttons) {
            val isSelected = option == font
            if (isSelected) {
                button.setBackgroundResource(R.drawable.bg_pill_translucent)
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                button.background = null
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.qv_white_translucent_80))
            }
        }
    }

    private fun updateFontSize(sizeSp: Float) {
        currentFontSizeSp = sizeSp
        binding.tvDetailsQuoteText.textSize = sizeSp
        binding.tvFontSizeDisplay.text = "${sizeSp.toInt()}"
        viewModel.setDetailFontSize(sizeSp)
    }

    private fun observeQuoteAndSettings(quoteId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.observeQuoteById(quoteId),
                    viewModel.selectedStyleIndex,
                    viewModel.appFont,
                    viewModel.detailFontSize
                ) { quote, styleIndex, font, fontSize ->
                    Snapshot(quote, styleIndex, font, fontSize)
                }.collect { snapshot ->
                    val style = GradientStyleProvider.getById(snapshot.styleIndex)
                    currentStyle = style
                    binding.detailsRoot.setBackgroundResource(style.backgroundRes)

                    val swatches = GradientStyleProvider.styles.map { item ->
                        StyleSwatchUiModel(
                            style = item,
                            isSelected = item.id == style.id
                        )
                    }
                    swatchAdapter.submitList(swatches)

                    selectedFont = snapshot.font
                    binding.tvDetailsQuoteText.typeface = snapshot.font.typeface
                    highlightFontButton(snapshot.font)

                    currentFontSizeSp = snapshot.fontSize
                    binding.tvDetailsQuoteText.textSize = snapshot.fontSize
                    binding.tvFontSizeDisplay.text = "${snapshot.fontSize.toInt()}"

                    if (snapshot.quote != null) {
                        bindQuote(snapshot.quote)
                    } else if (currentQuote == null) {
                        showQuoteSnackbar(R.string.details_quote_not_found)
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun bindQuote(quote: QuoteEntity) {
        currentQuote = quote
        val categoryItem = CategoryProvider.findByKey(quote.category)
        binding.tvDetailsCategory.text = getString(categoryItem.labelRes).uppercase(Locale.getDefault())
        binding.tvDetailsQuoteText.text = quote.quoteText
        binding.tvDetailsQuoteText.typeface = selectedFont.typeface
        binding.tvDetailsAuthor.text = getString(R.string.author_prefix, quote.author)

        binding.ivDetailsFavorite.setImageResource(
            if (quote.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )
        val favTint = ContextCompat.getColor(
            requireContext(),
            if (quote.isFavorite) R.color.qv_favorite_active else android.R.color.white
        )
        binding.ivDetailsFavorite.setColorFilter(favTint)
    }

    private fun handleSaveImageClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        exportCurrentQuoteAsImage()
    }

    private fun exportCurrentQuoteAsImage() {
        val quote = currentQuote ?: return
        val style = currentStyle
        val font = selectedFont
        val sizeMultiplier = currentFontSizeSp / 26f

        binding.actionSaveImage.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val result = ImageExportUtils.saveQuoteAsImage(
                context = requireContext(),
                quote = quote,
                style = style,
                fontOption = font,
                fontSizeMultiplier = sizeMultiplier
            )
            if (_binding != null) {
                binding.actionSaveImage.isEnabled = true
                if (result.isSuccess) {
                    showQuoteSnackbar(R.string.snackbar_image_saved)
                } else {
                    showQuoteSnackbar(R.string.snackbar_image_save_failed)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class Snapshot(
        val quote: QuoteEntity?,
        val styleIndex: Int,
        val font: FontOption,
        val fontSize: Float
    )
}
