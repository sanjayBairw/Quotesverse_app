package com.snj.quoteverse.utils

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.snj.quoteverse.R

/**
 * Visual background style used for quote cards, full-screen detail previews, and exported posters.
 */
data class GradientStyle(
    val id: Int,
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val backgroundRes: Int,
    @param:ColorInt val startColor: Int,
    @param:ColorInt val endColor: Int,
    @param:ColorInt val textColor: Int,
    @param:ColorInt val accentColor: Int
)

/**
 * Curated palette of editorial gradients anchored by QuoteVerse's Deep Purple, Lavender, and Gold.
 */
object GradientStyleProvider {

    val styles: List<GradientStyle> = listOf(
        GradientStyle(
            id = 0,
            nameRes = R.string.style_royal_velvet,
            backgroundRes = R.drawable.bg_gradient_royal_velvet,
            startColor = 0xFF6750A4.toInt(),
            endColor = 0xFF332266.toInt(),
            textColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFF4C95D.toInt()
        ),
        GradientStyle(
            id = 1,
            nameRes = R.string.style_twilight_lavender,
            backgroundRes = R.drawable.bg_gradient_twilight_lavender,
            startColor = 0xFF5A4296.toInt(),
            endColor = 0xFF9374E8.toInt(),
            textColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFF4C95D.toInt()
        ),
        GradientStyle(
            id = 2,
            nameRes = R.string.style_obsidian_gold,
            backgroundRes = R.drawable.bg_gradient_obsidian_gold,
            startColor = 0xFF1A1822.toInt(),
            endColor = 0xFF3A2B52.toInt(),
            textColor = 0xFFF7F4FF.toInt(),
            accentColor = 0xFFF4C95D.toInt()
        ),
        GradientStyle(
            id = 3,
            nameRes = R.string.style_rose_dusk,
            backgroundRes = R.drawable.bg_gradient_rose_dusk,
            startColor = 0xFF5D2E60.toInt(),
            endColor = 0xFF9A4E7A.toInt(),
            textColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFF4C95D.toInt()
        ),
        GradientStyle(
            id = 4,
            nameRes = R.string.style_cosmic_indigo,
            backgroundRes = R.drawable.bg_gradient_cosmic_indigo,
            startColor = 0xFF1E2749.toInt(),
            endColor = 0xFF553D94.toInt(),
            textColor = 0xFFFFFFFF.toInt(),
            accentColor = 0xFFB69DF8.toInt()
        ),
        GradientStyle(
            id = 5,
            nameRes = R.string.style_warm_amber,
            backgroundRes = R.drawable.bg_gradient_warm_amber,
            startColor = 0xFF352634.toInt(),
            endColor = 0xFF7B504A.toInt(),
            textColor = 0xFFFFFAF2.toInt(),
            accentColor = 0xFFF4C95D.toInt()
        )
    )

    fun getById(id: Int): GradientStyle {
        return styles.getOrElse(id.coerceIn(0, styles.lastIndex)) { styles.first() }
    }
}

