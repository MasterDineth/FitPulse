package com.mastersoft.fitpulse.fragment;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.activity.MainActivity;
import com.mastersoft.fitpulse.model.WorkoutSession;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "HomeFragment";

    // ── Daily step / calorie targets ─────────────────────────────────────────
    private static final int    STEP_GOAL     = 10_000;
    private static final int    CALORIE_GOAL  = 700;
    private static final double CALORIES_PER_STEP = 0.04;

    // ── Sensor & Local Storage ────────────────────────────────────────────────
    private SensorManager sensorManager;
    private Sensor        stepCounterSensor;
    private SharedPreferences stepPrefs;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView                tvUserName;
    private TextView                tvDateNumber;
    private TextView                tvDayName;
    private TextView                tvMonthName;
    private TextView                tvSteps;
    private TextView                tvCalories;
    private LinearProgressIndicator progressSteps;
    private LinearProgressIndicator progressCalories;
    private LinearLayout            llWorkoutHistory;

    // ── Workout history ───────────────────────────────────────────────────────
    private final List<WorkoutSession> workoutSessions = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the permission launcher in onCreate
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        registerStepSensor();
                    } else {
                        Log.w(TAG, "Activity recognition permission denied.");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase & Storage
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        stepPrefs = requireActivity().getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);

        // Sensor
        sensorManager    = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        bindViews(view);
        wireNavigation(view);

        // Feature implementations
        updateCalendarCard();
        loadUserGreeting();
        loadWorkoutHistory();

        // Load immediately from local storage so the UI isn't blank while waiting for the user to take a step
        loadInitialStepData();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndRequestPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View binding
    // ─────────────────────────────────────────────────────────────────────────

    private void bindViews(View root) {
        tvUserName        = root.findViewById(R.id.tvUserName);
        tvDateNumber      = root.findViewById(R.id.tvDateNumber);
        tvDayName         = root.findViewById(R.id.tvDayName);
        tvSteps           = root.findViewById(R.id.tvSteps);
        tvCalories        = root.findViewById(R.id.tvCalories);
        progressSteps     = root.findViewById(R.id.progressSteps);
        progressCalories  = root.findViewById(R.id.progressCalories);
        llWorkoutHistory  = root.findViewById(R.id.llWorkoutHistory);

        View calendarCard = root.findViewById(R.id.cardCheckIn);
        if (calendarCard != null) {
            tvMonthName = calendarCard.findViewWithTag("tvMonthName");
            if (tvMonthName == null) {
                if (tvDateNumber != null) {
                    ViewGroup parent = (ViewGroup) tvDateNumber.getParent();
                    if (parent != null && parent.getChildCount() > 0) {
                        View first = parent.getChildAt(0);
                        if (first instanceof TextView) {
                            tvMonthName = (TextView) first;
                        }
                    }
                }
            }
        }
    }

    private void wireNavigation(View view) {
        View ivAvatar = view.findViewById(R.id.ivAvatar);
        if (ivAvatar != null) ivAvatar.setOnClickListener(this::navigateToProfile);

        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) btnCheckIn.setOnClickListener(v -> navigateToCheckIn());

        View tvSeeAllHistory = view.findViewById(R.id.tvSeeAllHistory);
        if (tvSeeAllHistory != null) tvSeeAllHistory.setOnClickListener(v -> switchTab(R.id.nav_history));

        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> navigateToSettings());

        navigateTo(view.findViewById(R.id.seeAllStats), R.id.nav_stats);
        navigateTo(view.findViewById(R.id.seeAllTutorials), R.id.nav_tutorials);
        navigateTo(view.findViewById(R.id.tvSeeAllSchedules), R.id.nav_schedule);
    }

    private void updateCalendarCard() {
        Calendar now = Calendar.getInstance();
        if (tvDateNumber != null) tvDateNumber.setText(String.valueOf(now.get(Calendar.DAY_OF_MONTH)));
        if (tvMonthName != null) tvMonthName.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(now.getTime()));
        if (tvDayName != null) tvDayName.setText(new SimpleDateFormat("EEE", Locale.getDefault()).format(now.getTime()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 2 — Pedometer / Step Tracking (Robust Syncing)
    // ─────────────────────────────────────────────────────────────────────────

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            } else {
                registerStepSensor();
            }
        } else {
            // Permission automatically granted on Android 9 and below
            registerStepSensor();
        }
    }

    private void registerStepSensor() {
        if (stepCounterSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.w(TAG, "TYPE_STEP_COUNTER sensor not available on this device.");
        }
    }

    private void loadInitialStepData() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String savedDate = stepPrefs.getString("lastSavedDate", "");

        if (todayDate.equals(savedDate)) {
            // Load today's steps if they exist
            updateStepDisplay(stepPrefs.getInt("savedStepsToday", 0));
        } else {
            // It's a new day, initialize UI at 0
            updateStepDisplay(0);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        float rawSteps = event.values[0];
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        String savedDate = stepPrefs.getString("lastSavedDate", "");
        int savedStepsToday = stepPrefs.getInt("savedStepsToday", 0);
        float lastSensorValue = stepPrefs.getFloat("lastSensorValue", rawSteps);

        if (!savedDate.equals(todayDate)) {
            // It's a new day! Reset daily accumulation
            savedStepsToday = 0;
            lastSensorValue = rawSteps;
        } else if (rawSteps < lastSensorValue) {
            // The device was rebooted today, resetting the hardware sensor to 0.
            // We keep our accumulated steps, but reset the baseline tracker.
            lastSensorValue = rawSteps;
        }

        // Calculate steps taken since the last sensor update
        int delta = (int) (rawSteps - lastSensorValue);
        savedStepsToday += delta;
        lastSensorValue = rawSteps;

        // Sync continuously to local storage to prevent data loss
        stepPrefs.edit()
                .putString("lastSavedDate", todayDate)
                .putInt("savedStepsToday", savedStepsToday)
                .putFloat("lastSensorValue", lastSensorValue)
                .apply();

        updateStepDisplay(savedStepsToday);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateStepDisplay(int steps) {
        if (!isAdded()) return;

        String stepsFormatted = String.format(Locale.getDefault(), "%,d", steps);
        if (tvSteps != null) tvSteps.setText(stepsFormatted);

        int stepProgress = Math.min(100, (int) ((steps / (float) STEP_GOAL) * 100));
        if (progressSteps != null) progressSteps.setProgress(stepProgress);

        int calories = (int) Math.round(steps * CALORIES_PER_STEP);
        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

        int calProgress = Math.min(100, (int) ((calories / (float) CALORIE_GOAL) * 100));
        if (progressCalories != null) progressCalories.setProgress(calProgress);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 3 — Workout history
    // ─────────────────────────────────────────────────────────────────────────

    private void loadWorkoutHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("workoutHistory")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!isAdded()) return;

                    workoutSessions.clear();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        WorkoutSession session = doc.toObject(WorkoutSession.class);
                        session.setDocumentId(doc.getId());
                        workoutSessions.add(session);
                    }
                    renderWorkoutHistory();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load workout history", e));
    }

    private void renderWorkoutHistory() {
        if (llWorkoutHistory == null) return;
        llWorkoutHistory.removeAllViews();

        if (workoutSessions.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.no_workout_history);
            empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            empty.setPadding(0, 8, 0, 8);
            llWorkoutHistory.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (WorkoutSession session : workoutSessions) {
            View row = inflater.inflate(R.layout.item_workout_history, llWorkoutHistory, false);

            TextView tvName     = row.findViewById(R.id.tvWorkoutName);
            TextView tvDate     = row.findViewById(R.id.tvWorkoutDate);
            TextView tvDuration = row.findViewById(R.id.tvWorkoutDuration);

            if (tvName     != null) tvName.setText(session.getName());
            if (tvDate     != null) tvDate.setText(session.getFormattedDate());
            if (tvDuration != null) tvDuration.setText(session.getDurationMin() + " min");

            llWorkoutHistory.addView(row);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 4 — User Greeting
    // ─────────────────────────────────────────────────────────────────────────

    private void loadUserGreeting() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            setGreetingName("User");
            return;
        }

        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    String name = null;
                    if (snapshot.exists()) {
                        name = snapshot.getString("username");
                        if (name == null || name.isEmpty()) name = snapshot.getString("firstName");
                    }

                    if (name == null || name.isEmpty()) name = firebaseUser.getDisplayName();
                    if (name == null || name.isEmpty()) {
                        String email = firebaseUser.getEmail();
                        if (email != null && email.contains("@")) name = email.substring(0, email.indexOf('@'));
                    }
                    if (name == null || name.isEmpty()) name = "User";

                    setGreetingName(name);
                })
                .addOnFailureListener(e -> {
                    String fallback = firebaseUser.getDisplayName();
                    if (fallback == null || fallback.isEmpty()) fallback = "User";
                    setGreetingName(fallback);
                });
    }

    private void setGreetingName(String name) {
        if (tvUserName != null) {
            tvUserName.setText(name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation logic
    // ─────────────────────────────────────────────────────────────────────────

    private void switchTab(int navId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(navId);
        }
    }

    private void navigateTo(View view, int navId) {
        if (view != null) view.setOnClickListener(v -> switchTab(navId));
    }

    private void navigateToProfile(View sharedElement) {
        Navigation.findNavController(sharedElement).navigate(R.id.nav_profile);
    }

    private void navigateToCheckIn() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_checkin);
    }

    private void navigateToSettings() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_settings);
    }
}