package com.snj.quoteverse.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.snj.quoteverse.R
import com.snj.quoteverse.data.entity.QuoteEntity
import java.util.Calendar
import java.util.Locale

/**
 * Shared helpers for formatting, copying, and sharing quotes and time-of-day greetings.
 */
object QuoteActionUtils {

    fun formatQuoteForShare(quote: QuoteEntity): String {
        return "\"${quote.quoteText}\"\n\n— ${quote.author}\n\n#QuoteVerse #${quote.category}"
    }

    fun formatQuoteForClipboard(quote: QuoteEntity): String {
        return "\"${quote.quoteText}\" — ${quote.author}"
    }

    fun copyQuote(context: Context, quote: QuoteEntity): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        val clip = ClipData.newPlainText(
            context.getString(R.string.clipboard_label_quote),
            formatQuoteForClipboard(quote)
        )
        clipboard.setPrimaryClip(clip)
        return true
    }

    fun shareQuote(context: Context, quote: QuoteEntity) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_quote_subject))
            putExtra(Intent.EXTRA_TEXT, formatQuoteForShare(quote))
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_quote_chooser_title)
        )
        context.startActivity(chooser)
    }

    fun getGreetingResId(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Int {
        return when (hourOfDay) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val normalizedHour = hour.coerceIn(0, 23)
        val normalizedMinute = minute.coerceIn(0, 59)
        val period = if (normalizedHour < 12) "AM" else "PM"
        val displayHour = when {
            normalizedHour == 0 -> 12
            normalizedHour > 12 -> normalizedHour - 12
            else -> normalizedHour
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, normalizedMinute, period)
    }
}

