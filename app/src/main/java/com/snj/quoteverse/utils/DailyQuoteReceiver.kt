package com.snj.quoteverse.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snj.quoteverse.QuoteVerseApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager (or device boot) to display the Daily Quote
 * notification offline and schedule the next day's exact alarm.
 */
class DailyQuoteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = PreferencesManager(context)
        if (!preferences.isNotificationsEnabled()) return

        val hour = preferences.getNotificationHour()
        val minute = preferences.getNotificationMinute()

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.scheduleDailyQuote(context, hour, minute)
            return
        }

        // Reschedule tomorrow's exact alarm immediately so daily reminders continue reliably.
        NotificationScheduler.scheduleDailyQuote(context, hour, minute)

        if (!NotificationScheduler.hasNotificationPermission(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? QuoteVerseApplication
                val quote = app?.repository?.getDailyQuoteOnce()
                if (quote != null) {
                    NotificationScheduler.showDailyQuoteNotification(context, quote)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
