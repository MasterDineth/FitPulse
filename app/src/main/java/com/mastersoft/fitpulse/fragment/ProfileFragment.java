package com.mastersoft.fitpulse.fragment;
import android.app.UiModeManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import com.mastersoft.fitpulse.R;

public class ProfileFragment extends Fragment {

    // Edit mode flags
    private boolean isEditingPersonal = false;
    private boolean isEditingFitness  = false;

    // Personal info views
    private TextInputEditText etFullName, etEmail, etPhone, etDob;
    private View layoutEditActions;

    // Fitness data views
    private TextInputEditText etWeight, etHeight;
    private AutoCompleteTextView actvFitnessGoal, actvActivityLevel, actvWorkoutType;
    private View layoutFitnessActions;
    private TextView chipBmi;

    // Settings views
    private MaterialSwitch switchNotifications, switchReminders, switchDarkMode;

    // Dropdown option arrays
    private static final String[] GOALS = {
            "Muscle Gain", "Weight Loss", "Endurance", "Flexibility", "General Fitness"
    };
    private static final String[] ACTIVITY_LEVELS = {
            "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active"
    };
    private static final String[] WORKOUT_TYPES = {
            "Strength Training", "Cardio", "HIIT", "Yoga", "CrossFit", "Mixed"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupDropdowns();
        setupPersonalInfoEdit(view);
        setupFitnessEdit(view);
        setupSettings(view);
        setupNavigation(view);
        setupSignOut(view);
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View view) {
        etFullName       = view.findViewById(R.id.etFullName);
        etEmail          = view.findViewById(R.id.etEmail);
        etPhone          = view.findViewById(R.id.etPhone);
        etDob            = view.findViewById(R.id.etDob);
        layoutEditActions = view.findViewById(R.id.layoutEditActions);

        etWeight          = view.findViewById(R.id.etWeight);
        etHeight          = view.findViewById(R.id.etHeight);
        actvFitnessGoal   = view.findViewById(R.id.actvFitnessGoal);
        actvActivityLevel = view.findViewById(R.id.actvActivityLevel);
        actvWorkoutType   = view.findViewById(R.id.actvWorkoutType);
        layoutFitnessActions = view.findViewById(R.id.layoutFitnessActions);
        chipBmi           = view.findViewById(R.id.chipBmi);

        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchReminders     = view.findViewById(R.id.switchReminders);
        switchDarkMode      = view.findViewById(R.id.switchDarkMode);

        // Reflect current dark mode state
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        switchDarkMode.setChecked(nightMode == AppCompatDelegate.MODE_NIGHT_YES);
    }

    // ── Dropdown adapters ─────────────────────────────────────────────────────

    private void setupDropdowns() {
        if (getContext() == null) return;

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, GOALS);
        actvFitnessGoal.setAdapter(goalAdapter);

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVELS);
        actvActivityLevel.setAdapter(activityAdapter);

        ArrayAdapter<String> workoutAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, WORKOUT_TYPES);
        actvWorkoutType.setAdapter(workoutAdapter);
    }

    // ── Personal info edit ────────────────────────────────────────────────────

    private void setupPersonalInfoEdit(View view) {
        view.findViewById(R.id.btnEditPersonal).setOnClickListener(v -> enterPersonalEdit());
        view.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> exitPersonalEdit(false));
        view.findViewById(R.id.btnSavePersonal).setOnClickListener(v -> exitPersonalEdit(true));

        // Date picker on DOB tap
        view.findViewById(R.id.tilDob).setOnClickListener(v -> showDatePicker());
        if (etDob != null) etDob.setOnClickListener(v -> showDatePicker());
    }

    private void enterPersonalEdit() {
        isEditingPersonal = true;
        setPersonalFieldsEnabled(true);
        layoutEditActions.setVisibility(View.VISIBLE);
        layoutEditActions.setAlpha(0f);
        layoutEditActions.animate().alpha(1f).setDuration(250).start();
    }

    private void exitPersonalEdit(boolean save) {
        if (save) {
            // TODO: persist changes to your data layer / ViewModel
            Snackbar.make(requireView(), "Personal info updated", Snackbar.LENGTH_SHORT).show();
        }
        isEditingPersonal = false;
        setPersonalFieldsEnabled(false);
        layoutEditActions.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> layoutEditActions.setVisibility(View.GONE)).start();
    }

    private void setPersonalFieldsEnabled(boolean enabled) {
        etFullName.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        // etEmail and etDob are conditionally editable — keep email read-only always
        etDob.setEnabled(enabled);
    }

    private void showDatePicker() {
        if (!isEditingPersonal) return;
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder
                        .datePicker()
                        .setTitleText("Date of Birth")
                        .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            String formatted = android.text.format.DateFormat.format(
                    "MMM dd yyyy", new java.util.Date(selection)).toString();
            if (etDob != null) etDob.setText(formatted);
        });
        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    // ── Fitness data edit ─────────────────────────────────────────────────────

    private void setupFitnessEdit(View view) {
        view.findViewById(R.id.btnEditFitness).setOnClickListener(v -> enterFitnessEdit());
        view.findViewById(R.id.btnCancelFitness).setOnClickListener(v -> exitFitnessEdit(false));
        view.findViewById(R.id.btnSaveFitness).setOnClickListener(v -> exitFitnessEdit(true));

        // Live BMI update while editing
        android.text.TextWatcher bmiWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { updateBmiChip(); }
        };
        etWeight.addTextChangedListener(bmiWatcher);
        etHeight.addTextChangedListener(bmiWatcher);
    }

    private void enterFitnessEdit() {
        isEditingFitness = true;
        setFitnessFieldsEnabled(true);
        layoutFitnessActions.setVisibility(View.VISIBLE);
        layoutFitnessActions.setAlpha(0f);
        layoutFitnessActions.animate().alpha(1f).setDuration(250).start();
    }

    private void exitFitnessEdit(boolean save) {
        if (save) {
            Snackbar.make(requireView(), "Fitness data updated", Snackbar.LENGTH_SHORT).show();
        }
        isEditingFitness = false;
        setFitnessFieldsEnabled(false);
        layoutFitnessActions.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> layoutFitnessActions.setVisibility(View.GONE)).start();
    }

    private void setFitnessFieldsEnabled(boolean enabled) {
        etWeight.setEnabled(enabled);
        etHeight.setEnabled(enabled);
        actvFitnessGoal.setEnabled(enabled);
        actvFitnessGoal.setFocusable(enabled);
        actvActivityLevel.setEnabled(enabled);
        actvActivityLevel.setFocusable(enabled);
        actvWorkoutType.setEnabled(enabled);
        actvWorkoutType.setFocusable(enabled);
    }

    /** Recalculates BMI from current weight/height fields and updates the chip. */
    private void updateBmiChip() {
        try {
            double weightKg = Double.parseDouble(etWeight.getText().toString().trim());
            double heightCm = Double.parseDouble(etHeight.getText().toString().trim());
            if (heightCm <= 0) return;
            double heightM = heightCm / 100.0;
            double bmi = weightKg / (heightM * heightM);
            String category = bmiCategory(bmi);
            chipBmi.setText(String.format(java.util.Locale.getDefault(),
                    "%.1f · %s", bmi, category));
        } catch (NumberFormatException ignored) { /* incomplete input */ }
    }

    private String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    // ── App settings ──────────────────────────────────────────────────────────

    private void setupSettings(View view) {
        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Notifications — persist with SharedPreferences
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            // TODO: enable/disable notification channels
        });

        // Workout reminders
        switchReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            // TODO: schedule/cancel AlarmManager reminders
        });

        // Language row
        view.findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            // TODO: show language selection dialog
        });

        // Privacy Policy row
        view.findViewById(R.id.rowPrivacyPolicy).setOnClickListener(v -> {
            // TODO: open WebView or browser Intent
        });

        // About row
        view.findViewById(R.id.rowAbout).setOnClickListener(v -> {
            // TODO: open About dialog or fragment
        });
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    private void setupNavigation(View view) {
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Camera FAB — pick photo
        view.findViewById(R.id.fabChangePhoto).setOnClickListener(v -> {
            // TODO: launch image picker intent
            Snackbar.make(requireView(), "Photo picker coming soon", Snackbar.LENGTH_SHORT).show();
        });
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    private void setupSignOut(View view) {
        view.findViewById(R.id.btnSignOut).setOnClickListener(v ->
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out of FitPulse?")
                        .setPositiveButton("Sign Out", (dialog, which) -> {
                            // TODO: clear auth session and navigate to LoginActivity
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
}