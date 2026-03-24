package com.mastersoft.fitpulse.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mastersoft.fitpulse.R;

public class ProfileFragment extends Fragment {

    // Edit mode flags
    private boolean isEditingPersonal = false;
    private boolean isEditingFitness  = false;

    // Views — personal
    private TextInputEditText etFullName, etPhone, etDob;
    private View layoutEditActions;

    // Views — fitness
    private TextInputEditText etWeight, etHeight;
    private AutoCompleteTextView actvFitnessGoal, actvActivityLevel, actvWorkoutType;
    private View layoutFitnessActions;
    private TextView chipBmi;

    // Photo picker launcher
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ShapeableImageView ivAvatar;

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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPhotoPickerLauncher();
    }

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
        hideBottomNav();
        bindViews(view);
        setupDropdowns();
        setupPersonalInfoEdit(view);
        setupFitnessEdit(view);
        setupPhotoPicker(view);
        setupNavigation(view);
        setupSignOut(view);
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

    // ── Photo picker ──────────────────────────────────────────────────────────

    private void registerPhotoPickerLauncher() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        loadAvatarFromUri(selectedUri);
                    }
                });
    }

    private void setupPhotoPicker(View view) {
        // Both the avatar card and the camera FAB open the picker
        View cardAvatar   = view.findViewById(R.id.cardAvatar);
        View fabChangePhoto = view.findViewById(R.id.fabChangePhoto);

        if (cardAvatar != null)   cardAvatar.setOnClickListener(v -> openPhotoPicker());
        if (fabChangePhoto != null) fabChangePhoto.setOnClickListener(v -> openPhotoPicker());
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoPickerLauncher.launch(
                Intent.createChooser(intent, "Select profile photo"));
    }

    private void loadAvatarFromUri(Uri uri) {
        if (ivAvatar == null || getContext() == null) return;
        Glide.with(requireContext())
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(ivAvatar);
        Snackbar.make(requireView(), "Profile photo updated", Snackbar.LENGTH_SHORT).show();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View view) {
        ivAvatar          = view.findViewById(R.id.ivAvatar);
        etFullName        = view.findViewById(R.id.etFullName);
        etPhone           = view.findViewById(R.id.etPhone);
        etDob             = view.findViewById(R.id.etDob);
        layoutEditActions = view.findViewById(R.id.layoutEditActions);

        etWeight             = view.findViewById(R.id.etWeight);
        etHeight             = view.findViewById(R.id.etHeight);
        actvFitnessGoal      = view.findViewById(R.id.actvFitnessGoal);
        actvActivityLevel    = view.findViewById(R.id.actvActivityLevel);
        actvWorkoutType      = view.findViewById(R.id.actvWorkoutType);
        layoutFitnessActions = view.findViewById(R.id.layoutFitnessActions);
        chipBmi              = view.findViewById(R.id.chipBmi);
    }

    // ── Dropdown adapters ─────────────────────────────────────────────────────

    private void setupDropdowns() {
        if (getContext() == null) return;
        actvFitnessGoal.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, GOALS));
        actvActivityLevel.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVELS));
        actvWorkoutType.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, WORKOUT_TYPES));
    }

    // ── Personal info edit ────────────────────────────────────────────────────

    private void setupPersonalInfoEdit(View view) {
        view.findViewById(R.id.btnEditPersonal).setOnClickListener(v -> enterPersonalEdit());
        view.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> exitPersonalEdit(false));
        view.findViewById(R.id.btnSavePersonal).setOnClickListener(v -> exitPersonalEdit(true));
        if (etDob != null) etDob.setOnClickListener(v -> { if (isEditingPersonal) showDatePicker(); });
    }

    private void enterPersonalEdit() {
        isEditingPersonal = true;
        etFullName.setEnabled(true);
        etPhone.setEnabled(true);
        etDob.setEnabled(true);
        layoutEditActions.setVisibility(View.VISIBLE);
        layoutEditActions.setAlpha(0f);
        layoutEditActions.animate().alpha(1f).setDuration(250).start();
    }

    private void exitPersonalEdit(boolean save) {
        if (save) Snackbar.make(requireView(), "Personal info updated", Snackbar.LENGTH_SHORT).show();
        isEditingPersonal = false;
        etFullName.setEnabled(false);
        etPhone.setEnabled(false);
        etDob.setEnabled(false);
        layoutEditActions.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> layoutEditActions.setVisibility(View.GONE)).start();
    }

    private void showDatePicker() {
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder
                        .datePicker()
                        .setTitleText("Date of Birth")
                        .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            String formatted = android.text.format.DateFormat
                    .format("MMM dd yyyy", new java.util.Date(selection)).toString();
            if (etDob != null) etDob.setText(formatted);
        });
        datePicker.show(getChildFragmentManager(), "DOB_PICKER");
    }

    // ── Fitness data edit ─────────────────────────────────────────────────────

    private void setupFitnessEdit(View view) {
        view.findViewById(R.id.btnEditFitness).setOnClickListener(v -> enterFitnessEdit());
        view.findViewById(R.id.btnCancelFitness).setOnClickListener(v -> exitFitnessEdit(false));
        view.findViewById(R.id.btnSaveFitness).setOnClickListener(v -> exitFitnessEdit(true));

        android.text.TextWatcher bmiWatcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) { updateBmiChip(); }
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
        if (save) Snackbar.make(requireView(), "Fitness data updated", Snackbar.LENGTH_SHORT).show();
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

    private void updateBmiChip() {
        try {
            double w = Double.parseDouble(etWeight.getText().toString().trim());
            double h = Double.parseDouble(etHeight.getText().toString().trim());
            if (h <= 0) return;
            double bmi = w / ((h / 100.0) * (h / 100.0));
            chipBmi.setText(String.format(java.util.Locale.getDefault(),
                    "%.1f · %s", bmi, bmiCategory(bmi)));
        } catch (NumberFormatException ignored) {}
    }

    private String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void setupNavigation(View view) {
        // Back button
        view.findViewById(R.id.btnBack).setOnClickListener(
                v -> Navigation.findNavController(v).popBackStack());

        // Settings shortcut button in header
        view.findViewById(R.id.btnOpenSettings).setOnClickListener(v -> navigateToSettings(v));

        // Settings quick-link card in body
        view.findViewById(R.id.cardGoToSettings).setOnClickListener(v -> navigateToSettings(v));
    }

    private void navigateToSettings(View view) {
        Navigation.findNavController(view).navigate(R.id.nav_settings);
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────

    private void setupSignOut(View view) {
        view.findViewById(R.id.btnSignOut).setOnClickListener(v ->
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out of FitPulse?")
                        .setPositiveButton("Sign Out", (dialog, which) -> {
                            // TODO: clear auth session, navigate to LoginActivity
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
}