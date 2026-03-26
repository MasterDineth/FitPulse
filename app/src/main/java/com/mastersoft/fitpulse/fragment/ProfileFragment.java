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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mastersoft.fitpulse.R;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // Edit mode flags
    private boolean isEditingPersonal = false;
    private boolean isEditingBilling = false;
    private boolean isEditingFitness = false;

    // Views — personal
    private TextInputEditText etFullName, etPhone, etDob;

    private TextView tvProfileName;
    private View layoutEditActions;

    // Views — billing
    private TextInputEditText etAddress;
    private AutoCompleteTextView actvCity, actvCountry;
    private View layoutBillingActions;

    // Views — fitness
    private TextInputEditText etWeight, etHeight;
    private AutoCompleteTextView actvFitnessGoal, actvActivityLevel, actvWorkoutType;
    private View layoutFitnessActions;
    private TextView chipBmi;

    // Photo picker launcher
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ShapeableImageView ivAvatar;

    // Dropdown option arrays
    private static final String[] GOALS = {"Muscle Gain", "Weight Loss", "Endurance", "Flexibility", "General Fitness"};
    private static final String[] ACTIVITY_LEVELS = {"Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active"};
    private static final String[] WORKOUT_TYPES = {"Strength Training", "Cardio", "HIIT", "Yoga", "CrossFit", "Mixed"};
    private static final String[] CITIES = {"Colombo", "Kandy", "Galle", "Gampaha", "Negombo", "Jaffna", "Kurunegala"};
    private static final String[] COUNTRIES = {"Sri Lanka", "India", "Australia", "United Kingdom", "United States"};

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
        registerPhotoPickerLauncher();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        bindViews(view);
        setupDropdowns();
        loadUserData(); // Fetch initial data from Firestore

        setupPersonalInfoEdit(view);
        setupBillingEdit(view);
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

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadUserData() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        if (tvProfileName != null) {
                            tvProfileName.setText(documentSnapshot.getString("username"));
                        }

                        // Personal
                        etFullName.setText(documentSnapshot.getString("fullName"));
                        etDob.setText(documentSnapshot.getString("dob"));
                        etPhone.setText(documentSnapshot.getString("mobile")); // Mapping 'mobile' field from DB

                        // Billing
                        etAddress.setText(documentSnapshot.getString("address"));
                        actvCity.setText(documentSnapshot.getString("city"), false);
                        actvCountry.setText(documentSnapshot.getString("country"), false);

                        // Fitness
                        etWeight.setText(documentSnapshot.getString("weight"));
                        etHeight.setText(documentSnapshot.getString("height"));
                        actvFitnessGoal.setText(documentSnapshot.getString("fitnessGoal"), false);
                        actvActivityLevel.setText(documentSnapshot.getString("activityLevel"), false);
                        actvWorkoutType.setText(documentSnapshot.getString("workoutType"), false);

                        updateBmiChip();
                    }
                })
                .addOnFailureListener(e -> Snackbar.make(requireView(), "Error loading profile", Snackbar.LENGTH_LONG).show());
    }

    // ── Update Logic (Modular) ────────────────────────────────────────────────

    private void saveSectionToFirestore(Map<String, Object> data, String sectionName) {
        if (userId == null) return;

        db.collection("users").document(userId)
                .update(data) // Surgical update: only affects keys present in 'data'
                .addOnSuccessListener(aVoid -> Snackbar.make(requireView(), sectionName + " updated", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(requireView(), "Failed to save " + sectionName, Snackbar.LENGTH_SHORT).show());
    }

    // ── Personal Info ─────────────────────────────────────────────────────────

    private void setupPersonalInfoEdit(View view) {
        view.findViewById(R.id.btnEditPersonal).setOnClickListener(v -> enterPersonalEdit());
        view.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> exitPersonalEdit(false));
        view.findViewById(R.id.btnSavePersonal).setOnClickListener(v -> exitPersonalEdit(true));
        if (etDob != null) etDob.setOnClickListener(v -> {
            if (isEditingPersonal) showDatePicker();
        });
    }

    private void enterPersonalEdit() {
        isEditingPersonal = true;
        etFullName.setEnabled(true);
        etDob.setEnabled(true);
        toggleActionsVisibility(layoutEditActions, true);
    }

    private void exitPersonalEdit(boolean save) {
        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("fullName", etFullName.getText().toString().trim());
            data.put("dob", etDob.getText().toString().trim());
            saveSectionToFirestore(data, "Personal info");
        }
        isEditingPersonal = false;
        etFullName.setEnabled(false);
        etDob.setEnabled(false);
        toggleActionsVisibility(layoutEditActions, false);
    }

    // ── Billing Info ──────────────────────────────────────────────────────────

    private void setupBillingEdit(View view) {
        view.findViewById(R.id.btnEditBilling).setOnClickListener(v -> enterBillingEdit());
        view.findViewById(R.id.btnCancelBilling).setOnClickListener(v -> exitBillingEdit(false));
        view.findViewById(R.id.btnSaveBilling).setOnClickListener(v -> exitBillingEdit(true));
    }

    private void enterBillingEdit() {
        isEditingBilling = true;
        setBillingFieldsEnabled(true);
        toggleActionsVisibility(layoutBillingActions, true);
    }

    private void exitBillingEdit(boolean save) {
        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", etAddress.getText().toString().trim());
            data.put("city", actvCity.getText().toString().trim());
            data.put("country", actvCountry.getText().toString().trim());
            saveSectionToFirestore(data, "Billing data");
        }
        isEditingBilling = false;
        setBillingFieldsEnabled(false);
        toggleActionsVisibility(layoutBillingActions, false);
    }

    private void setBillingFieldsEnabled(boolean enabled) {
        etAddress.setEnabled(enabled);
        actvCity.setEnabled(enabled);
        actvCountry.setEnabled(enabled);
    }

    // ── Fitness Info ──────────────────────────────────────────────────────────

    private void setupFitnessEdit(View view) {
        view.findViewById(R.id.btnEditFitness).setOnClickListener(v -> enterFitnessEdit());
        view.findViewById(R.id.btnCancelFitness).setOnClickListener(v -> exitFitnessEdit(false));
        view.findViewById(R.id.btnSaveFitness).setOnClickListener(v -> exitFitnessEdit(true));

        android.text.TextWatcher bmiWatcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable s) {
                updateBmiChip();
            }
        };
        etWeight.addTextChangedListener(bmiWatcher);
        etHeight.addTextChangedListener(bmiWatcher);
    }

    private void enterFitnessEdit() {
        isEditingFitness = true;
        setFitnessFieldsEnabled(true);
        toggleActionsVisibility(layoutFitnessActions, true);
    }

    private void exitFitnessEdit(boolean save) {
        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("weight", etWeight.getText().toString().trim());
            data.put("height", etHeight.getText().toString().trim());
            data.put("fitnessGoal", actvFitnessGoal.getText().toString().trim());
            data.put("activityLevel", actvActivityLevel.getText().toString().trim());
            data.put("workoutType", actvWorkoutType.getText().toString().trim());
            saveSectionToFirestore(data, "Fitness data");
        }
        isEditingFitness = false;
        setFitnessFieldsEnabled(false);
        toggleActionsVisibility(layoutFitnessActions, false);
    }

    private void setFitnessFieldsEnabled(boolean enabled) {
        etWeight.setEnabled(enabled);
        etHeight.setEnabled(enabled);
        actvFitnessGoal.setEnabled(enabled);
        actvActivityLevel.setEnabled(enabled);
        actvWorkoutType.setEnabled(enabled);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateBmiChip() {
        try {
            double w = Double.parseDouble(etWeight.getText().toString().trim());
            double h = Double.parseDouble(etHeight.getText().toString().trim());
            if (h <= 0) return;
            // BMI Formula: weight / height^2
            double bmi = w / ((h / 100.0) * (h / 100.0));
            if (chipBmi != null) {
                chipBmi.setText(String.format(java.util.Locale.getDefault(), "%.1f · %s", bmi, bmiCategory(bmi)));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    private void toggleActionsVisibility(View view, boolean visible) {
        if (view == null) return;
        if (visible) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(250).start();
        } else {
            view.animate().alpha(0f).setDuration(200).withEndAction(() -> view.setVisibility(View.GONE)).start();
        }
    }

    private void bindViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        etFullName = view.findViewById(R.id.etFullName);
        etPhone = view.findViewById(R.id.etPhone);
        etDob = view.findViewById(R.id.etDob);
        layoutEditActions = view.findViewById(R.id.layoutEditActions);

        etWeight = view.findViewById(R.id.etWeight);
        etHeight = view.findViewById(R.id.etHeight);
        actvFitnessGoal = view.findViewById(R.id.actvFitnessGoal);
        actvActivityLevel = view.findViewById(R.id.actvActivityLevel);
        actvWorkoutType = view.findViewById(R.id.actvWorkoutType);
        layoutFitnessActions = view.findViewById(R.id.layoutFitnessActions);
        chipBmi = view.findViewById(R.id.chipBmi);

        etAddress = view.findViewById(R.id.etAddress);
        actvCity = view.findViewById(R.id.actvCity);
        actvCountry = view.findViewById(R.id.actvCountry);
        layoutBillingActions = view.findViewById(R.id.layoutBillingActions);
    }

    private void setupDropdowns() {
        if (getContext() == null) return;
        if (actvFitnessGoal != null) actvFitnessGoal.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, GOALS));
        if (actvActivityLevel != null) actvActivityLevel.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVELS));
        if (actvWorkoutType != null) actvWorkoutType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, WORKOUT_TYPES));
        if (actvCity != null) actvCity.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, CITIES));
        if (actvCountry != null) actvCountry.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, COUNTRIES));
    }

    private void showDatePicker() {
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker().setTitleText("Date of Birth").build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            String formatted = android.text.format.DateFormat.format("MMM dd yyyy", new java.util.Date(selection)).toString();
            etDob.setText(formatted);
        });
        datePicker.show(getChildFragmentManager(), "DOB_PICKER");
    }

    private void setupSignOut(View view) {
        View btnSignOut = view.findViewById(R.id.btnSignOut);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sign Out")
                            .setMessage("Are you sure you want to sign out?")
                            .setPositiveButton("Sign Out", (dialog, which) -> {
                                mAuth.signOut();
                                // Navigate to login
                            })
                            .setNegativeButton("Cancel", null)
                            .show());
        }
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

    private void registerPhotoPickerLauncher() {
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                loadAvatarFromUri(result.getData().getData());
            }
        });
    }

    private void setupPhotoPicker(View view) {
        View cardAvatar = view.findViewById(R.id.cardAvatar);
        View fab = view.findViewById(R.id.fabChangePhoto);
        if (cardAvatar != null) cardAvatar.setOnClickListener(v -> openPhotoPicker());
        if (fab != null) fab.setOnClickListener(v -> openPhotoPicker());
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoPickerLauncher.launch(Intent.createChooser(intent, "Select profile photo"));
    }

    private void loadAvatarFromUri(Uri uri) {
        if (ivAvatar != null) {
            Glide.with(requireContext()).load(uri).circleCrop().into(ivAvatar);
        }
    }

    private void setupNavigation(View view) {
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        }
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
}