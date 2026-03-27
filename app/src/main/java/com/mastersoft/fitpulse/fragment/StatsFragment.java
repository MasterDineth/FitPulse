package com.mastersoft.fitpulse.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mastersoft.fitpulse.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private FirebaseFirestore db;
    private String userId;
    private SharedPreferences stepPrefs;

    // Daily Goals
    private static final int GOAL_STEPS = 10000;
    private static final int GOAL_CALORIES = 700;
    private static final int GOAL_ACTIVE_MINS = 60;
    private static final double GOAL_DISTANCE = 7.6; // Approx 10k steps in km

    // Views - Stats
    private TextView tvStepsValue, tvCaloriesValue, tvActiveTime, tvDistance;
    private LinearProgressIndicator progressSteps, progressCalories, progressActiveTime, progressDistance;

    // Views - Health
    private TextView tvWeight, tvHeight, tvBmi, tvBodyFat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
        stepPrefs = requireContext().getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        bindViews(view);
        setupToolbar(view);

        // Feature implementations
        loadDailyStats();
        loadHealthData();
        updateWeeklyGraph(view);

        // Update button redirects to ProfileFragment
        View btnUpdate = view.findViewById(R.id.btnUpdateHealthData);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_profile));
        }
    }

    private void bindViews(View v) {
        // Text Values
        tvStepsValue = v.findViewById(R.id.tvStepsValue);
        tvCaloriesValue = v.findViewById(R.id.tvCaloriesValue);
        tvActiveTime = v.findViewById(R.id.tvActiveTime);
        tvDistance = v.findViewById(R.id.tvDistance);

        // Progress Indicators
        progressSteps = v.findViewById(R.id.progressSteps);
        progressCalories = v.findViewById(R.id.progressCalories);
        progressActiveTime = v.findViewById(R.id.progressActiveTime);
        progressDistance = v.findViewById(R.id.progressDistance);

        // Health
        tvWeight = v.findViewById(R.id.tvWeight);
        tvHeight = v.findViewById(R.id.tvHeight);
        tvBmi = v.findViewById(R.id.tvBmi);
        tvBodyFat = v.findViewById(R.id.tvBodyFat);
    }

    // ── FEATURE 1: DAILY STATS (STEPS + FIREBASE WORKOUTS) ───────────────────

    private void loadDailyStats() {
        // 1. Get Steps from local storage
        int steps = stepPrefs.getInt("savedStepsToday", 0);
        if (tvStepsValue != null)
            tvStepsValue.setText(String.format(Locale.getDefault(), "%,d", steps));
        if (progressSteps != null)
            progressSteps.setProgressCompat(Math.min(100, (int) ((steps / (float) GOAL_STEPS) * 100)), true);

        // 2. Calculate Distance (Steps * 0.00076 km average stride)
        double distance = steps * 0.00076;
        if (tvDistance != null)
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f", distance));
        if (progressDistance != null)
            progressDistance.setProgressCompat(Math.min(100, (int) ((distance / GOAL_DISTANCE) * 100)), true);

        // 3. Query Firestore for today's specific workout calories and active time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();

        if (userId == null) return;

        // Query by user ONLY to avoid needing a Composite Index in Firestore.
        // We filter the dates locally to ensure the query always succeeds.
        db.collection("workoutHistory")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    double workoutCalories = 0;
                    double activeMinutes = 0;

                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        Timestamp docTimestamp = doc.getTimestamp("date");

                        // Check if the workout happened today
                        if (docTimestamp != null && !docTimestamp.toDate().before(todayStart)) {
                            workoutCalories += doc.getDouble("caloriesBurned") != null ? doc.getDouble("caloriesBurned") : 0;
                            activeMinutes += doc.getDouble("totalMinutes") != null ? doc.getDouble("totalMinutes") : 0;
                        }
                    }

                    // Total Calories = (Steps * 0.04 factor) + Workout Calories
                    int totalCalories = (int) ((steps * 0.04) + workoutCalories);
                    int totalActiveTime = (int) activeMinutes;

                    if (tvCaloriesValue != null)
                        tvCaloriesValue.setText(String.valueOf(totalCalories));
                    if (progressCalories != null)
                        progressCalories.setProgressCompat(Math.min(100, (int) ((totalCalories / (float) GOAL_CALORIES) * 100)), true);

                    if (tvActiveTime != null) tvActiveTime.setText(String.valueOf(totalActiveTime));
                    if (progressActiveTime != null)
                        progressActiveTime.setProgressCompat(Math.min(100, (int) ((totalActiveTime / (float) GOAL_ACTIVE_MINS) * 100)), true);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching workout history", e));
    }

    // ── FEATURE 2: HEALTH DATA & BMI CALCULATION ─────────────────────────────

    private void loadHealthData() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String weightStr = doc.getString("weight");
                        String heightStr = doc.getString("height");

                        if (weightStr != null && heightStr != null && !weightStr.isEmpty() && !heightStr.isEmpty()) {
                            if (tvWeight != null) tvWeight.setText(weightStr + " kg");
                            if (tvHeight != null) tvHeight.setText(heightStr + " cm");

                            try {
                                calculateAndDisplayBmi(Double.parseDouble(weightStr), Double.parseDouble(heightStr));
                            } catch (NumberFormatException e) {
                                displayNoData();
                            }
                        } else {
                            displayNoData();
                        }
                    } else {
                        displayNoData();
                    }
                });
    }

    private void calculateAndDisplayBmi(double weight, double heightCm) {
        if (heightCm <= 0) return;
        double heightM = heightCm / 100.0;
        double bmi = weight / (heightM * heightM);

        String category;
        if (bmi < 18.5) category = "Underweight";
        else if (bmi < 25) category = "Normal";
        else if (bmi < 30) category = "Overweight";
        else category = "Obese";

        if (tvBmi != null)
            tvBmi.setText(String.format(Locale.getDefault(), "%.1f • %s", bmi, category));
    }

    private void displayNoData() {
        if (tvWeight != null) tvWeight.setText("No data");
        if (tvHeight != null) tvHeight.setText("No data");
        if (tvBmi != null) tvBmi.setText("No data");
    }

    // ── FEATURE 3: WEEKLY STEP GRAPH ─────────────────────────────────────────

    private void updateWeeklyGraph(View root) {
        // Logic for weekly step chart initialization
    }

    // ── UTILITIES ────────────────────────────────────────────────────────────

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.statsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void hideBottomNav() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.VISIBLE);
        }
    }
}