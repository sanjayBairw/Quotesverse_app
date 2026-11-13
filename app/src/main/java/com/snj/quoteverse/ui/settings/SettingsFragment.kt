package com.snj.quoteverse.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.snj.quoteverse.BuildConfig
import com.snj.quoteverse.QuoteVerseApplication
import com.snj.quoteverse.R
import com.snj.quoteverse.databinding.FragmentSettingsBinding
import com.snj.quoteverse.utils.NotificationScheduler
import com.snj.quoteverse.utils.QuoteActionUtils
import com.snj.quoteverse.utils.ThemeManager
import com.snj.quoteverse.utils.ThemeMode
import com.snj.quoteverse.utils.applyTopSystemBarPadding
import com.snj.quoteverse.utils.showQuoteSnackbar
import com.snj.quoteverse.viewmodel.QuoteViewModel
import com.snj.quoteverse.viewmodel.QuoteViewModelFactory
import com.snj.quoteverse.viewmodel.SyncState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Settings screen managing app theme, daily quote notification schedule,
 * ZenQuotes API synchronization, attribution, and app metadata.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuoteViewModel by activityViewModels {
        val app = requireActivity().application as QuoteVerseApplication
        QuoteViewModelFactory(app.repository, app.preferencesManager, app.networkMonitor)
    }

    private var isUpdatingControlsProgrammatically = false
    private var pendingTestNotification = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingTestNotification) {
                pendingTestNotification = false
                sendTestNotificationNow()
            } else {
                enableNotifications()
            }
        } else {
            pendingTestNotification = false
            isUpdatingControlsProgrammatically = true
            binding.switchNotifications.isChecked = false
            isUpdatingControlsProgrammatically = false
            viewModel.setNotificationsEnabled(false)
            showQuoteSnackbar(R.string.snackbar_notification_permission_denied)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyTopSystemBarPadding()

        binding.tvAppVersion.text = BuildConfig.VERSION_NAME
        setupThemeToggle()
        setupFontPreference()
        setupNotificationControls()
        setupZenQuotesSyncSection()
        setupAboutDialogs()
        observeSettingsState()
        observeSyncState()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.notificationsEnabled.value &&
            NotificationScheduler.hasNotificationPermission(requireContext())
        ) {
            val (hour, minute) = viewModel.notificationTime.value
            NotificationScheduler.scheduleDailyQuote(requireContext(), hour, minute)
        }
    }

    private fun setupThemeToggle() {
        binding.toggleThemeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isUpdatingControlsProgrammatically) return@addOnButtonCheckedListener
            val selectedMode = when (checkedId) {
                R.id.btnThemeLight -> ThemeMode.LIGHT
                R.id.btnThemeDark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            viewModel.setThemeMode(selectedMode)
            ThemeManager.applyTheme(selectedMode)
        }
    }

    private fun setupFontPreference() {
        binding.cardFontPreferences.setOnClickListener {
            val options = com.snj.quoteverse.utils.FontOption.entries.toTypedArray()
            val optionNames = options.map { getString(it.labelRes) }.toTypedArray()
            val currentFontIndex = options.indexOf(viewModel.appFont.value).coerceAtLeast(0)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_font_dialog_title)
                .setSingleChoiceItems(optionNames, currentFontIndex) { dialog, which ->
                    val chosen = options[which]
                    viewModel.setAppFont(chosen)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.action_close, null)
                .show()
        }
    }

    private fun setupNotificationControls() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingControlsProgrammatically) return@setOnCheckedChangeListener
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationScheduler.hasNotificationPermission(requireContext())
                ) {
                    pendingTestNotification = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnCheckedChangeListener
                }
                enableNotifications()
            } else {
                viewModel.setNotificationsEnabled(false)
                NotificationScheduler.cancelDailyQuote(requireContext())
                showQuoteSnackbar(R.string.snackbar_notification_disabled)
            }
        }

        binding.rowNotificationTime.setOnClickListener {
            showTimePicker()
        }

        binding.rowTestNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationScheduler.hasNotificationPermission(requireContext())
            ) {
                pendingTestNotification = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@setOnClickListener
            }
            sendTestNotificationNow()
        }
    }

    private fun setupZenQuotesSyncSection() {
        binding.rowZenQuotesAttribution.setOnClickListener {
            showAttributionDialog()
        }

        binding.rowSyncBatch.setOnClickListener {
            viewModel.syncQuotesBatch()
        }

        binding.rowSyncToday.setOnClickListener {
            viewModel.syncTodayQuote(force = true)
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

    private fun enableNotifications() {
        val (hour, minute) = viewModel.notificationTime.value
        viewModel.setNotificationsEnabled(true)
        NotificationScheduler.scheduleDailyQuote(requireContext(), hour, minute)
        showQuoteSnackbar(
            getString(
                R.string.snackbar_notification_scheduled,
                QuoteActionUtils.formatTime12Hour(hour, minute)
            )
        )
        promptExactAlarmPermissionIfNeeded()
    }

    private fun promptExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !NotificationScheduler.canScheduleExactAlarms(requireContext())
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.exact_alarm_dialog_title)
                .setMessage(R.string.exact_alarm_dialog_message)
                .setPositiveButton(R.string.action_open_settings) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivity(intent)
                }
                .setNegativeButton(R.string.action_close, null)
                .show()
        }
    }

    private fun sendTestNotificationNow() {
        val quote = viewModel.homeUiState.value.featuredQuote
        if (quote != null) {
            val posted = NotificationScheduler.showDailyQuoteNotification(requireContext(), quote)
            showQuoteSnackbar(
                if (posted) R.string.snackbar_test_notification_sent
                else R.string.snackbar_notification_permission_denied
            )
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val app = requireActivity().application as QuoteVerseApplication
                val daily = app.repository.getDailyQuoteOnce()
                if (daily != null && _binding != null) {
                    val posted = NotificationScheduler.showDailyQuoteNotification(requireContext(), daily)
                    showQuoteSnackbar(
                        if (posted) R.string.snackbar_test_notification_sent
                        else R.string.snackbar_notification_permission_denied
                    )
                }
            }
        }
    }

    private fun showTimePicker() {
        val (currentHour, currentMinute) = viewModel.notificationTime.value
        val clockFormat = if (DateFormat.is24HourFormat(requireContext())) {
            TimeFormat.CLOCK_24H
        } else {
            TimeFormat.CLOCK_12H
        }

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(R.string.settings_time_picker_title)
            .build()

        picker.addOnPositiveButtonClickListener {
            val newHour = picker.hour
            val newMinute = picker.minute
            viewModel.setNotificationTime(newHour, newMinute)
            if (viewModel.notificationsEnabled.value) {
                NotificationScheduler.scheduleDailyQuote(requireContext(), newHour, newMinute)
                showQuoteSnackbar(
                    getString(
                        R.string.snackbar_notification_scheduled,
                        QuoteActionUtils.formatTime12Hour(newHour, newMinute)
                    )
                )
                promptExactAlarmPermissionIfNeeded()
            }
        }

        picker.show(childFragmentManager, TIME_PICKER_TAG)
    }

    private fun setupAboutDialogs() {
        binding.rowAboutApp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_about_title)
                .setMessage(R.string.about_dialog_message)
                .setPositiveButton(R.string.action_close, null)
                .show()
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_privacy_title)
                .setMessage(R.string.privacy_dialog_message)
                .setPositiveButton(R.string.action_close, null)
                .show()
        }
    }

    private fun observeSettingsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.themeMode,
                    viewModel.notificationsEnabled,
                    viewModel.notificationTime,
                    viewModel.totalQuoteCount
                ) { themeMode, notificationsEnabled, notificationTime, totalQuotes ->
                    SettingsSnapshot(themeMode, notificationsEnabled, notificationTime, totalQuotes)
                }.collect { snapshot ->
                    isUpdatingControlsProgrammatically = true

                    val targetButtonId = when (snapshot.themeMode) {
                        ThemeMode.SYSTEM -> R.id.btnThemeSystem
                        ThemeMode.LIGHT -> R.id.btnThemeLight
                        ThemeMode.DARK -> R.id.btnThemeDark
                    }
                    if (binding.toggleThemeGroup.checkedButtonId != targetButtonId) {
                        binding.toggleThemeGroup.check(targetButtonId)
                    }

                    if (binding.switchNotifications.isChecked != snapshot.notificationsEnabled) {
                        binding.switchNotifications.isChecked = snapshot.notificationsEnabled
                    }

                    isUpdatingControlsProgrammatically = false

                    val (hour, minute) = snapshot.notificationTime
                    binding.tvNotificationTimeValue.text = QuoteActionUtils.formatTime12Hour(hour, minute)
                    binding.tvAboutSubtitle.text = getString(
                        R.string.settings_about_subtitle,
                        snapshot.totalQuotes
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appFont.collect { font ->
                    binding.tvCurrentFontName.setText(font.labelRes)
                    binding.tvCurrentFontName.typeface = font.typeface
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
                            binding.tvSyncBatchAction.text = getString(R.string.action_sync) + "…"
                            binding.tvSyncTodayAction.text = getString(R.string.action_sync) + "…"
                            binding.rowSyncBatch.isEnabled = false
                            binding.rowSyncToday.isEnabled = false
                        }
                        is SyncState.Success -> {
                            binding.tvSyncBatchAction.setText(R.string.action_sync_now)
                            binding.tvSyncTodayAction.setText(R.string.action_sync)
                            binding.rowSyncBatch.isEnabled = true
                            binding.rowSyncToday.isEnabled = true
                            showQuoteSnackbar(state.message)
                            viewModel.resetSyncState()
                        }
                        is SyncState.Error -> {
                            binding.tvSyncBatchAction.setText(R.string.action_sync_now)
                            binding.tvSyncTodayAction.setText(R.string.action_sync)
                            binding.rowSyncBatch.isEnabled = true
                            binding.rowSyncToday.isEnabled = true
                            showQuoteSnackbar(state.message)
                            viewModel.resetSyncState()
                        }
                        is SyncState.Idle -> {
                            binding.tvSyncBatchAction.setText(R.string.action_sync_now)
                            binding.tvSyncTodayAction.setText(R.string.action_sync)
                            binding.rowSyncBatch.isEnabled = true
                            binding.rowSyncToday.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class SettingsSnapshot(
        val themeMode: ThemeMode,
        val notificationsEnabled: Boolean,
        val notificationTime: Pair<Int, Int>,
        val totalQuotes: Int
    )

    companion object {
        private const val TIME_PICKER_TAG = "daily_quote_time_picker"
    }
}
