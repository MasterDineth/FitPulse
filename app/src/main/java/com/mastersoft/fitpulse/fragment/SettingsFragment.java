package com.mastersoft.fitpulse.fragment;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.mastersoft.fitpulse.R;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchNotifications;
    private MaterialSwitch switchReminders;
    private MaterialSwitch switchCheckInReminders;
    private MaterialSwitch switchDarkMode;
    private MaterialSwitch switchAutoSync;

    // Launcher for the POST_NOTIFICATIONS runtime permission (Android 13+)
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerNotificationPermissionLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        bindViews(view);
        setupToolbar(view);
        setupNotificationSwitches();
        setupAppearance();
        setupPrivacyRows(view);
        setupDataRows(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNav();
    }

    // ── Bottom nav visibility ─────────────────────────────────────────────────

    private void hideBottomNav() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.GONE);
        }
    }

    private void showBottomNav() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.VISIBLE);
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View view) {
        switchNotifications      = view.findViewById(R.id.switchNotifications);
        switchReminders          = view.findViewById(R.id.switchReminders);
        switchCheckInReminders   = view.findViewById(R.id.switchCheckInReminders);
        switchDarkMode           = view.findViewById(R.id.switchDarkMode);
        switchAutoSync           = view.findViewById(R.id.switchAutoSync);

        // Reflect current dark mode state
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        switchDarkMode.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);

        // Reflect current notification permission state
        switchNotifications.setChecked(areNotificationsEnabled());
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    // ── Notification permission launcher ─────────────────────────────────────

    private void registerNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        switchNotifications.setChecked(true);
                        Snackbar.make(requireView(),
                                "Notifications enabled", Snackbar.LENGTH_SHORT).show();
                    } else {
                        // Permission denied — keep switch off and explain
                        switchNotifications.setChecked(false);
                        showNotificationRationaleDialog();
                    }
                });
    }

    // ── Notification switches ─────────────────────────────────────────────────

    private void setupNotificationSwitches() {

        // Main push notifications switch — requests permission if needed
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            } else {
                // User turned off — guide them to system settings to fully disable
                Snackbar.make(requireView(),
                                "To fully disable notifications, go to System Settings",
                                Snackbar.LENGTH_LONG)
                        .setAction("Open", v -> openAppNotificationSettings())
                        .show();
            }
        });

        // Workout reminders sub-switch — only functional if main switch is on
        switchReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !areNotificationsEnabled()) {
                btn.setChecked(false);
                requestNotificationPermission();
            } else {
                // TODO: schedule / cancel AlarmManager for daily workout reminder
            }
        });

        // Check-In reminders sub-switch
        switchCheckInReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !areNotificationsEnabled()) {
                btn.setChecked(false);
                requestNotificationPermission();
            } else {
                // TODO: schedule / cancel check-in notification
            }
        });
    }

    /**
     * Requests POST_NOTIFICATIONS on Android 13+.
     * On older Android, notifications are on by default — just enable the channel.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                // Already granted
                switchNotifications.setChecked(true);
            } else if (shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS)) {
                // Show rationale first, then request
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Enable Notifications")
                        .setMessage("FitPulse needs notification permission to send you " +
                                "workout reminders and gym check-in alerts.")
                        .setPositiveButton("Allow", (d, w) ->
                                notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS))
                        .setNegativeButton("Not now", (d, w) ->
                                switchNotifications.setChecked(false))
                        .show();
            } else {
                // First time or previously denied without rationale
                notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android < 13: notifications allowed by default
            switchNotifications.setChecked(true);
        }
    }

    /** Checks whether the app currently has notification permission / channel enabled. */
    private boolean areNotificationsEnabled() {
        NotificationManager nm = (NotificationManager)
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }

    /** Shows a dialog explaining how to enable notifications from System Settings. */
    private void showNotificationRationaleDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permission Denied")
                .setMessage("You've denied notification permission. To receive workout reminders, " +
                        "enable notifications for FitPulse in your device settings.")
                .setPositiveButton("Open Settings", (d, w) -> openAppNotificationSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Opens the app's notification settings page in the system Settings app. */
    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    private void setupAppearance() {
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO));
    }

    // ── Privacy & Legal rows ──────────────────────────────────────────────────

    private void setupPrivacyRows(View view) {
        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v -> {
            // TODO: open WebView or browser intent with privacy policy URL
        });

        view.findViewById(R.id.rowTerms).setOnClickListener(v -> {
            // TODO: open Terms of Service
        });

        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("FitPulse")
                        .setMessage("Version 1.0.0\n\nYour fitness, your rhythm.\n\n© 2024 FitPulse")
                        .setPositiveButton("OK", null)
                        .show());

        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            String[] languages = {"English", "Sinhala", "Tamil"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Language")
                    .setItems(languages, (d, which) -> {
                        TextView tvLanguage = view.findViewById(R.id.tvLanguageValue);
                        if (tvLanguage != null) tvLanguage.setText(languages[which]);
                        // TODO: apply locale change
                    })
                    .show();
        });
    }

    // ── Data & Storage rows ───────────────────────────────────────────────────

    private void setupDataRows(View view) {
        view.findViewById(R.id.rowClearCache).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Clear Cache")
                        .setMessage("This will delete temporary files. Your workout data will not be affected.")
                        .setPositiveButton("Clear", (d, w) ->
                                Snackbar.make(requireView(), "Cache cleared",
                                        Snackbar.LENGTH_SHORT).show())
                        .setNegativeButton("Cancel", null)
                        .show());

        switchAutoSync.setOnCheckedChangeListener((btn, isChecked) -> {
            // TODO: toggle background sync worker
        });
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}