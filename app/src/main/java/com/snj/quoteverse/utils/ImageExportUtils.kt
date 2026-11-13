package com.snj.quoteverse.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.snj.quoteverse.data.entity.QuoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Renders high-resolution (1080x1350) editorial quote posters for saving to device gallery
 * or sharing directly via Android Sharesheet.
 */
object ImageExportUtils {

    private const val IMAGE_WIDTH = 1080
    private const val IMAGE_HEIGHT = 1350
    private const val HORIZONTAL_PADDING = 110f

    suspend fun saveQuoteAsImage(
        context: Context,
        quote: QuoteEntity,
        style: GradientStyle,
        fontOption: FontOption = FontOption.SERIF,
        fontSizeMultiplier: Float = 1.0f
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val bitmap = renderQuoteBitmap(quote, style, fontOption, fontSizeMultiplier)
        try {
            val uri = insertBitmapToMediaStore(context, bitmap, quote.id)
            Result.success(uri)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun createQuoteImageForShare(
        context: Context,
        quote: QuoteEntity,
        style: GradientStyle,
        fontOption: FontOption = FontOption.SERIF,
        fontSizeMultiplier: Float = 1.0f
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val bitmap = renderQuoteBitmap(quote, style, fontOption, fontSizeMultiplier)
        try {
            val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val shareFile = File(shareDir, "QuoteVerse_${quote.id}.png")
            FileOutputStream(shareFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            bitmap.recycle()
        }
    }

    fun shareQuoteImage(context: Context, imageUri: Uri, quote: QuoteEntity) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(
                Intent.EXTRA_TEXT,
                "“${quote.quoteText}” — ${quote.author}\n\nShared via QuoteVerse • Daily Inspiration"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Quote Image"))
    }

    private fun renderQuoteBitmap(
        quote: QuoteEntity,
        style: GradientStyle,
        fontOption: FontOption = FontOption.SERIF,
        fontSizeMultiplier: Float = 1.0f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Base editorial gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                IMAGE_WIDTH.toFloat(),
                IMAGE_HEIGHT.toFloat(),
                style.startColor,
                style.endColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat(), bgPaint)

        // Subtle luminous orbs for visual depth
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                IMAGE_WIDTH * 0.85f,
                IMAGE_HEIGHT * 0.16f,
                460f,
                intArrayOf(withAlpha(style.accentColor, 46), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(IMAGE_WIDTH * 0.85f, IMAGE_HEIGHT * 0.16f, 460f, orbPaint)

        val lowerOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                IMAGE_WIDTH * 0.15f,
                IMAGE_HEIGHT * 0.86f,
                420f,
                intArrayOf(withAlpha(style.textColor, 26), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(IMAGE_WIDTH * 0.15f, IMAGE_HEIGHT * 0.86f, 420f, lowerOrbPaint)

        // Category pill at top center
        val categoryLabel = quote.category.uppercase()
        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accentColor
            textSize = 28f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillTextWidth = categoryPaint.measureText(categoryLabel)
        val pillHalfWidth = (pillTextWidth / 2f) + 46f
        val pillRect = RectF(
            (IMAGE_WIDTH / 2f) - pillHalfWidth,
            135f,
            (IMAGE_WIDTH / 2f) + pillHalfWidth,
            205f
        )
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(style.accentColor, 36)
        }
        canvas.drawRoundRect(pillRect, 35f, 35f, pillBgPaint)
        canvas.drawText(categoryLabel, IMAGE_WIDTH / 2f, 180f, categoryPaint)

        // Decorative quote mark
        val glyphPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(style.accentColor, 180)
            textSize = 140f
            typeface = Typeface.create(fontOption.typeface ?: Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", IMAGE_WIDTH / 2f, 365f, glyphPaint)

        // Main quote body using StaticLayout
        val contentWidth = (IMAGE_WIDTH - (HORIZONTAL_PADDING * 2)).toInt()
        val baseFontSize = when {
            quote.quoteText.length > 180 -> 44f
            quote.quoteText.length > 110 -> 52f
            else -> 60f
        }
        val quoteFontSize = (baseFontSize * fontSizeMultiplier).coerceIn(36f, 80f)
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.textColor
            textSize = quoteFontSize
            typeface = fontOption.typeface ?: Typeface.DEFAULT
        }
        val quoteLayout = StaticLayout.Builder
            .obtain(quote.quoteText, 0, quote.quoteText.length, quotePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(14f, 1.18f)
            .build()

        val quoteBlockTop = ((IMAGE_HEIGHT - quoteLayout.height) / 2f) - 20f
        canvas.save()
        canvas.translate(HORIZONTAL_PADDING, quoteBlockTop.coerceAtLeast(390f))
        quoteLayout.draw(canvas)
        canvas.restore()

        // Divider + Author
        val afterQuoteY = (quoteBlockTop.coerceAtLeast(390f) + quoteLayout.height + 64f)
            .coerceAtMost(IMAGE_HEIGHT - 220f)
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.accentColor
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            (IMAGE_WIDTH / 2f) - 54f,
            afterQuoteY,
            (IMAGE_WIDTH / 2f) + 54f,
            afterQuoteY,
            dividerPaint
        )

        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(style.textColor, 235)
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText("— ${quote.author}", IMAGE_WIDTH / 2f, afterQuoteY + 66f, authorPaint)

        // Subtle footer branding
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(style.textColor, 145)
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.14f
        }
        canvas.drawText("QUOTEVERSE", IMAGE_WIDTH / 2f, IMAGE_HEIGHT - 88f, brandPaint)

        return bitmap
    }

    private fun insertBitmapToMediaStore(context: Context, bitmap: Bitmap, quoteId: Long): Uri {
        val fileName = "QuoteVerse_${quoteId}_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/QuoteVerse"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collectionUri, values)
            ?: throw IOException("Failed to create MediaStore entry for quote poster.")

        try {
            resolver.openOutputStream(itemUri).use { stream ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IOException("Failed to write PNG stream to MediaStore.")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            return itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            throw e
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        val clampedAlpha = alpha.coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (clampedAlpha shl 24)
    }
}
