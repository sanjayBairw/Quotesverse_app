package com.snj.quoteverse.utils

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.snj.quoteverse.R

/**
 * Curated typography options for quote rendering across QuoteVerse.
 * Backed by bundled open-source typefaces (OFL) for 100% offline consistency.
 */
enum class FontOption(
    val key: String,
    @param:StringRes val labelRes: Int,
    @param:FontRes val fontResId: Int,
    val fontFamilyName: String
) {
    SERIF("serif", R.string.font_serif, R.font.font_serif, "serif"),
    SANS("sans", R.string.font_sans, R.font.font_sans, "sans-serif"),
    MONOSPACE("monospace", R.string.font_monospace, R.font.font_monospace, "monospace"),
    CASUAL("casual", R.string.font_casual, R.font.font_casual, "casual");

    @Volatile
    private var cachedTypeface: Typeface? = null

    val typeface: Typeface?
        get() = cachedTypeface ?: getFallbackTypeface()

    fun getTypeface(context: Context?): Typeface? {
        cachedTypeface?.let { return it }
        if (context == null) return getFallbackTypeface()
        return try {
            val tf = ResourcesCompat.getFont(context.applicationContext, fontResId) ?: getFallbackTypeface()
            cachedTypeface = tf
            tf
        } catch (_: Throwable) {
            getFallbackTypeface()
        }
    }

    private fun getFallbackTypeface(): Typeface? {
        return try {
            when (this) {
                SERIF -> Typeface.SERIF
                SANS -> Typeface.SANS_SERIF
                MONOSPACE -> Typeface.MONOSPACE
                CASUAL -> Typeface.create("casual", Typeface.NORMAL)
            }
        } catch (_: Throwable) {
            try { Typeface.DEFAULT } catch (_: Throwable) { null }
        }
    }

    companion object {
        val DEFAULT = SERIF

        fun init(context: Context) {
            val appCtx = context.applicationContext
            for (option in entries) {
                try {
                    option.cachedTypeface = ResourcesCompat.getFont(appCtx, option.fontResId)
                } catch (_: Throwable) {
                    // Falls back gracefully to system typefaces
                }
            }
        }

        fun fromKey(key: String?): FontOption {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DEFAULT
        }
    }
}
