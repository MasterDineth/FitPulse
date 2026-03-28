package com.mastersoft.fitpulse.fragment;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.receiver.NotificationReceiver;

import java.util.Calendar;

public class SettingsFragment extends Fragment {

    private static final String CHANNEL_ID = "FITPULSE_CHANNEL";
    private static final int REQ_WORKOUT = 101;
    private static final int REQ_CHECKIN = 102;

    private MaterialSwitch switchNotifications;
    private MaterialSwitch switchReminders;
    private MaterialSwitch switchCheckInReminders;
    private MaterialSwitch switchDarkMode;
    private MaterialSwitch switchAutoSync;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

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
        createNotificationChannel();
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

    private void bindViews(View view) {
        switchNotifications      = view.findViewById(R.id.switchNotifications);
        switchReminders          = view.findViewById(R.id.switchReminders);
        switchCheckInReminders   = view.findViewById(R.id.switchCheckInReminders);
        switchDarkMode           = view.findViewById(R.id.switchDarkMode);
        switchAutoSync           = view.findViewById(R.id.switchAutoSync);

        int nightMode = AppCompatDelegate.getDefaultNightMode();
        switchDarkMode.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);
        switchNotifications.setChecked(areNotificationsEnabled());
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FitPulse Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reminders and alerts for FitPulse");
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void registerNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        switchNotifications.setChecked(true);
                        sendConfirmationNotification("Push Notifications Enabled", "You will now receive alerts from FitPulse.", 1);
                        Snackbar.make(requireView(), "Notifications enabled", Snackbar.LENGTH_SHORT).show();
                    } else {
                        switchNotifications.setChecked(false);
                        showNotificationRationaleDialog();
                    }
                });
    }

    private void setupNotificationSwitches() {
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            } else {
                Snackbar.make(requireView(),
                                "To fully disable notifications, go to System Settings",
                                Snackbar.LENGTH_LONG)
                        .setAction("Open", v -> openAppNotificationSettings())
                        .show();
            }
        });

        // Workout Reminders (Scheduled for 7:00 AM daily)
        switchReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!areNotificationsEnabled()) {
                    btn.setChecked(false);
                    requestNotificationPermission();
                    return;
                }
                sendConfirmationNotification("Workout Reminders Enabled", "We'll remind you daily to crush your goals.", 2);
                scheduleDailyReminder(REQ_WORKOUT, "Time to Workout!", "Let's hit the gym and crush your goals for today.", 7, 0);
            } else {
                cancelDailyReminder(REQ_WORKOUT);
            }
        });

        // Check-In Reminders (Scheduled for 5:30 PM daily)
        switchCheckInReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!areNotificationsEnabled()) {
                    btn.setChecked(false);
                    requestNotificationPermission();
                    return;
                }
                sendConfirmationNotification("Check-in Reminders Enabled", "We'll remind you to log your gym visits.", 3);
                scheduleDailyReminder(REQ_CHECKIN, "Did you check in?", "Don't forget to scan your QR code at the gym desk today.", 17, 30);
            } else {
                cancelDailyReminder(REQ_CHECKIN);
            }
        });
    }

    //Scheduling and Sending Notifications

    private void sendConfirmationNotification(String title, String message, int notificationId) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pulse) // Ensure you have this icon or replace it
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    private void scheduleDailyReminder(int requestCode, String title, String message, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("notificationId", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule it for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
              alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    private void cancelDailyReminder(int requestCode) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }



    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                switchNotifications.setChecked(true);
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Enable Notifications")
                        .setMessage("FitPulse needs notification permission to send you workout reminders and gym check-in alerts.")
                        .setPositiveButton("Allow", (d, w) -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .setNegativeButton("Not now", (d, w) -> switchNotifications.setChecked(false))
                        .show();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            switchNotifications.setChecked(true);
        }
    }

    private boolean areNotificationsEnabled() {
        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }

    private void showNotificationRationaleDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permission Denied")
                .setMessage("You've denied notification permission. To receive workout reminders, enable notifications for FitPulse in your device settings.")
                .setPositiveButton("Open Settings", (d, w) -> openAppNotificationSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }


    private void setupAppearance() {
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));
    }

    private void setupPrivacyRows(View view) {
        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v -> {});
        view.findViewById(R.id.rowTerms).setOnClickListener(v -> {});
        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("FitPulse")
                        .setMessage("Version 1.0.0\n\nYour fitness, your rhythm.\n\n2024 FitPulse")
                        .setPositiveButton("OK", null)
                        .show());

        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            String[] languages = {"English", "Sinhala", "Tamil"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Language")
                    .setItems(languages, (d, which) -> {
                        TextView tvLanguage = view.findViewById(R.id.tvLanguageValue);
                        if (tvLanguage != null) tvLanguage.setText(languages[which]);
                    })
                    .show();
        });
    }

    private void setupDataRows(View view) {
        view.findViewById(R.id.rowClearCache).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Clear Cache")
                        .setMessage("This will delete temporary files. Your workout data will not be affected.")
                        .setPositiveButton("Clear", (d, w) ->
                                Snackbar.make(requireView(), "Cache cleared", Snackbar.LENGTH_SHORT).show())
                        .setNegativeButton("Cancel", null)
                        .show());

        switchAutoSync.setOnCheckedChangeListener((btn, isChecked) -> {});
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }
}