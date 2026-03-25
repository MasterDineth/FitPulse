package com.mastersoft.fitpulse.fragment;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.activity.MainActivity;
import com.mastersoft.fitpulse.adapter.WorkoutHistoryAdapter;
import com.mastersoft.fitpulse.model.WorkoutSession;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.mastersoft.fitpulse.model.StepCounterHelper;

public class HomeFragment extends Fragment implements SensorEventListener {

    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 100;
    private static final String TAG = "HomeFragment";

    // ── Daily step / calorie targets ─────────────────────────────────────────
    private static final int    STEP_GOAL     = 10_000;
    private static final int    CALORIE_GOAL  = 700;
    /**
     * MET-based estimate: average stride ≈ 0.762 m, 70 kg person.
     * Calories = steps × 0.04   (simplified from MET formula; override as needed)
     */
    private static final double CALORIES_PER_STEP = 0.04;

    // ── Sensor ────────────────────────────────────────────────────────────────
    private SensorManager sensorManager;
    private Sensor        stepCounterSensor;
    /** Raw cumulative total reported by TYPE_STEP_COUNTER since last reboot. */
    private float stepsAtBoot       = -1f;   // first reading captured this session
    private int   stepsThisSession  = 0;     // steps walked since fragment started

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
    private LinearLayout            llWorkoutHistory;   // dynamic host container

    // ── Workout history ───────────────────────────────────────────────────────
    private final List<WorkoutSession> workoutSessions = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

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

        // Firebase
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Sensor
        sensorManager    = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        bindViews(view);
        wireNavigation(view);

        // ── Feature implementations ───────────────────────────────────────────
        updateCalendarCard();
        loadUserGreeting();
        loadWorkoutHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerStepSensor();
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

        // The month label does not have an id in the XML — we locate it by its
        // sibling relationship: it is the first TextView inside the calendar card's
        // inner LinearLayout.  We assign it a tag so we can update it safely.
        View calendarCard = root.findViewById(R.id.cardCheckIn);
        if (calendarCard != null) {
            tvMonthName = calendarCard.findViewWithTag("tvMonthName");
            if (tvMonthName == null) {
                // Locate by position: the calendar inner card → LinearLayout → child 0
                // Structure: cardCheckIn > LinearLayout[h] > cardView > LinearLayout[v]
                //            > TextView(month)[0], TextView(date)[1], TextView(day)[2]
                // We'll use the parent of tvDateNumber
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation wiring  (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private void wireNavigation(View view) {
        // Avatar → Profile
        View ivAvatar = view.findViewById(R.id.ivAvatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> navigateToProfile(v));
        }

        // Check-In button
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> navigateToCheckIn());
        }

        // See all → History tab
        View tvSeeAllHistory = view.findViewById(R.id.tvSeeAllHistory);
        if (tvSeeAllHistory != null) {
            tvSeeAllHistory.setOnClickListener(v -> switchTab(R.id.nav_history));
        }

        // Settings
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> navigateToSettings());
        }

        // See all → Stats tab
        navigateTo(view.findViewById(R.id.seeAllStats), R.id.nav_stats);

        // See all → Tutorials tab
        navigateTo(view.findViewById(R.id.seeAllTutorials), R.id.nav_tutorials);

        // See all → Schedule tab
        navigateTo(view.findViewById(R.id.tvSeeAllSchedules), R.id.nav_schedule);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 1 — Calendar card
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Populates the small calendar widget inside the Check-In card with the
     * real current month (abbreviated), day-of-month (digit) and
     * day-of-week (abbreviated).
     *
     * Layout reference:
     *   tvMonthName  — e.g. "Mar"
     *   tvDateNumber — e.g. "25"
     *   tvDayName    — e.g. "Wed"
     */
    private void updateCalendarCard() {
        Calendar now = Calendar.getInstance();

        // Day number  (1 – 31)
        if (tvDateNumber != null) {
            tvDateNumber.setText(String.valueOf(now.get(Calendar.DAY_OF_MONTH)));
        }

        // Abbreviated month name  ("Jan", "Feb", … "Dec")
        if (tvMonthName != null) {
            String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(now.getTime());
            tvMonthName.setText(month);
        }

        // Abbreviated day name  ("Mon", "Tue", … "Sun")
        if (tvDayName != null) {
            String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(now.getTime());
            tvDayName.setText(day);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 2 — Pedometer / step counter + calorie calculation
    // ─────────────────────────────────────────────────────────────────────────

    private void registerStepSensor() {
        if (stepCounterSensor != null && sensorManager != null) {
            sensorManager.registerListener(
                    this,
                    stepCounterSensor,
                    SensorManager.SENSOR_DELAY_UI
            );
        } else {
            // Device has no step-counter sensor — show 0 gracefully
            updateStepDisplay(0);
            Log.w(TAG, "TYPE_STEP_COUNTER sensor not available on this device.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        float totalSinceReboot = event.values[0];

        // Capture baseline on first reading this session
        if (stepsAtBoot < 0) {
            stepsAtBoot = totalSinceReboot;
        }

        stepsThisSession = (int) (totalSinceReboot - stepsAtBoot);
        updateStepDisplay(stepsThisSession);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* no-op */ }

    /**
     * Pushes step count and derived calorie burn into the UI.
     *
     * @param steps number of steps walked since the fragment registered the sensor
     */
    private void updateStepDisplay(int steps) {
        if (!isAdded()) return;

        // Format with thousands separator
        String stepsFormatted = String.format(Locale.getDefault(), "%,d", steps);
        if (tvSteps != null) tvSteps.setText(stepsFormatted);

        // Progress bar  (0 – 100)
        int stepProgress = Math.min(100, (int) ((steps / (float) STEP_GOAL) * 100));
        if (progressSteps != null) progressSteps.setProgress(stepProgress);

        // Calories  (rounded to nearest integer)
        int calories = (int) Math.round(steps * CALORIES_PER_STEP);
        if (tvCalories != null) tvCalories.setText(String.valueOf(calories));

        // Calories progress bar
        int calProgress = Math.min(100, (int) ((calories / (float) CALORIE_GOAL) * 100));
        if (progressCalories != null) progressCalories.setProgress(calProgress);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 3 — Workout history from Firestore
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Queries the authenticated user's workout history collection in Firestore.
     *
     * Expected Firestore structure:
     *   users/{uid}/workoutHistory/{docId}
     *     - name        : String   — workout name
     *     - date        : Timestamp or String — workout date
     *     - durationMin : long     — duration in minutes
     *     - type        : String   — "upper" | "lower" | "cardio" | "fullbody" | …
     *
     * The 3 most-recent sessions are rendered in the history list.
     */
    private void loadWorkoutHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No authenticated user — skipping workout history load.");
            return;
        }

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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load workout history", e);
                });
    }

    /**
     * Dynamically inflates a workout-history row for each session and appends
     * it to {@code llWorkoutHistory}.  The XML already contains three static
     * placeholder cards; those are removed first so we start with a clean slate
     * and only real data is shown.
     *
     * If you prefer to keep the static placeholders as skeletons while data
     * loads, remove the {@code llWorkoutHistory.removeAllViews()} call and
     * instead hide / show the existing children.
     */
    private void renderWorkoutHistory() {
        if (llWorkoutHistory == null) return;
        llWorkoutHistory.removeAllViews();

        if (workoutSessions.isEmpty()) {
            // Show a friendly "no history yet" message
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
    //  Feature 4 — Greeting with logged-in user's name
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the user's display name in this priority order:
     *   1. Firestore profile document  (users/{uid}.displayName)  — most accurate
     *   2. FirebaseAuth displayName    — set during sign-up / Google sign-in
     *   3. Email local part            — fallback when no display name is stored
     *   4. "User"                      — absolute last resort
     */
    private void loadUserGreeting() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            setGreetingName("User");
            return;
        }

        // Try Firestore first for the richest profile data
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;

                    String name = null;
                    if (snapshot.exists()) {
                        name = snapshot.getString("displayName");
                        if (name == null || name.isEmpty()) {
                            name = snapshot.getString("firstName"); // alternate field
                        }
                    }

                    // Fallback chain
                    if (name == null || name.isEmpty()) {
                        name = firebaseUser.getDisplayName();
                    }
                    if (name == null || name.isEmpty()) {
                        String email = firebaseUser.getEmail();
                        if (email != null && email.contains("@")) {
                            name = email.substring(0, email.indexOf('@'));
                        }
                    }
                    if (name == null || name.isEmpty()) {
                        name = "User";
                    }

                    setGreetingName(name);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user profile", e);
                    // Graceful degradation: use whatever FirebaseAuth has
                    String fallback = firebaseUser.getDisplayName();
                    if (fallback == null || fallback.isEmpty()) fallback = "User";
                    setGreetingName(fallback);
                });
    }

    /** Applies the resolved name to the greeting TextView. */
    private void setGreetingName(String name) {
        if (tvUserName != null) {
            tvUserName.setText(name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void navigateToProfile(View sharedElement) {
        Navigation.findNavController(sharedElement).navigate(R.id.nav_profile);
    }

    private void navigateToCheckIn() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_checkin);
    }

    private void navigateToSettings() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_settings);
    }

    /** Switches the bottom-nav tab via MainActivity. */
    private void switchTab(int navId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(navId);
        }
    }

    /** Convenience: attaches a tab-switch listener to any view by nav-graph id. */
    private void navigateTo(View view, int navId) {
        if (view != null) {
            view.setOnClickListener(v -> switchTab(navId));
        }
    }

    //request permission




}