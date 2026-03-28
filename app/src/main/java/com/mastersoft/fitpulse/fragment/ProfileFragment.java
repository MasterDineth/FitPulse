package com.mastersoft.fitpulse.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.activity.SignInActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // Local Storage
    private static final String PREFS_NAME = "UserProfileCache";
    private SharedPreferences sharedPreferences;

    // Edit mode flags
    private boolean isEditingPersonal = false;
    private boolean isEditingBilling = false;
    private boolean isEditingFitness = false;

    // Views
    private TextInputEditText etFullName, etEmail, etPhone, etDob;
    private TextView tvProfileName, tvProfileSubtitle;
    private View layoutEditActions;
    private TextInputEditText etAddress;
    private AutoCompleteTextView actvCity, actvCountry;
    private View layoutBillingActions;
    private TextInputEditText etWeight, etHeight;
    private AutoCompleteTextView actvFitnessGoal, actvActivityLevel, actvWorkoutType;
    private View layoutFitnessActions;
    private TextView chipBmi;
    private ShapeableImageView ivAvatar;

    // Photo picker launcher
    private ActivityResultLauncher<Intent> photoPickerLauncher;

    // Dropdown option arrays
    // Hardcoded strings to reduce unnessasory network calls
    private static final String[] GOALS = {"Muscle Gain", "Weight Loss", "Endurance", "Flexibility", "General Fitness"};
    private static final String[] ACTIVITY_LEVELS = {"Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extremely Active"};
    private static final String[] WORKOUT_TYPES = {"Strength Training", "Cardio", "HIIT", "Yoga", "CrossFit", "Mixed"};
    private static final String[] CITIES = {"Colombo", "Kandy", "Galle", "Gampaha", "Negombo", "Jaffna", "Kurunegala"};
    private static final String[] COUNTRIES = {"Sri Lanka", "India", "Australia", "United Kingdom", "United States"};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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

        // 1. Load from local data
        loadFromLocal();

        // load data form firebase if loacl storage empty
        if (!sharedPreferences.contains("username")) {
            loadUserDataFromFirestore();
        }

        setupPersonalInfoEdit(view);
        setupBillingEdit(view);
        setupFitnessEdit(view);
        setupPhotoPicker(view);
        setupNavigation(view);
        setupSignOut(view);
    }

    // Data loading (local || firestore)

    private void loadFromLocal() {
        if (tvProfileName != null)
            tvProfileName.setText(sharedPreferences.getString("username", "User"));

        // Fix: Use the correct key and a simple default fallback
        if (tvProfileSubtitle != null) {
            tvProfileSubtitle.setText(sharedPreferences.getString("dateRegistered", "Member"));
        }

        if (etFullName != null) etFullName.setText(sharedPreferences.getString("fullName", ""));
        if (etEmail != null) etEmail.setText(sharedPreferences.getString("email", ""));
        if (etDob != null) etDob.setText(sharedPreferences.getString("dob", ""));
        if (etPhone != null) etPhone.setText(sharedPreferences.getString("mobile", ""));
        if (etAddress != null) etAddress.setText(sharedPreferences.getString("address", ""));
        if (actvCity != null) actvCity.setText(sharedPreferences.getString("city", ""), false);
        if (actvCountry != null)
            actvCountry.setText(sharedPreferences.getString("country", ""), false);
        if (etWeight != null) etWeight.setText(sharedPreferences.getString("weight", ""));
        if (etHeight != null) etHeight.setText(sharedPreferences.getString("height", ""));
        if (actvFitnessGoal != null)
            actvFitnessGoal.setText(sharedPreferences.getString("fitnessGoal", ""), false);
        if (actvActivityLevel != null)
            actvActivityLevel.setText(sharedPreferences.getString("activityLevel", ""), false);
        if (actvWorkoutType != null)
            actvWorkoutType.setText(sharedPreferences.getString("workoutType", ""), false);
        updateBmiChip();
    }

    private void saveToLocal(Map<String, Object> data) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                editor.putString(entry.getKey(), entry.getValue().toString());
            }
        }
        editor.apply();
    }

    private void loadUserDataFromFirestore() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        syncFirestoreToLocal(documentSnapshot);
                        loadFromLocal(); // Refresh UI from the newly updated local cache
                    }
                })
                .addOnFailureListener(e -> Snackbar.make(requireView(), "Error syncing profile", Snackbar.LENGTH_LONG).show());
    }

    private void syncFirestoreToLocal(DocumentSnapshot doc) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", doc.getString("username"));
        data.put("email", doc.getString("email"));
        data.put("fullName", doc.getString("fullName"));
        data.put("dob", doc.getString("dob"));
        data.put("mobile", doc.getString("mobile"));
        data.put("address", doc.getString("address"));
        data.put("city", doc.getString("city"));
        data.put("country", doc.getString("country"));
        data.put("weight", doc.getString("weight"));
        data.put("height", doc.getString("height"));
        data.put("fitnessGoal", doc.getString("fitnessGoal"));
        data.put("activityLevel", doc.getString("activityLevel"));
        data.put("workoutType", doc.getString("workoutType"));


        Timestamp timestamp = doc.getTimestamp("dateRegistered");
        if (timestamp != null) {
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            data.put("dateRegistered", "Member · Since " + sdf.format(date));
        } else {
            data.put("dateRegistered", "Member");
        }

        saveToLocal(data);
    }

    private void saveSectionToFirestore(Map<String, Object> data, String sectionName) {
        if (userId == null) return;

        db.collection("users").document(userId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    saveToLocal(data); // Sync local cache immediately after cloud success
                    Snackbar.make(requireView(), sectionName + " saved", Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Snackbar.make(requireView(), "Update failed (Check connection)", Snackbar.LENGTH_SHORT).show());
    }

    // ── SECTION UPDATES ───────────────────────────────────────────────────────

    private void exitPersonalEdit(boolean save) {
        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("fullName", etFullName.getText().toString().trim());
            data.put("dob", etDob.getText().toString().trim());
            saveSectionToFirestore(data, "Personal info");
        } else {
            loadFromLocal(); // Revert to local cache if cancelled
        }
        isEditingPersonal = false;
        etFullName.setEnabled(false);
        etDob.setEnabled(false);
        toggleActionsVisibility(layoutEditActions, false);
    }

    private void exitBillingEdit(boolean save) {
        if (save) {
            Map<String, Object> data = new HashMap<>();
            data.put("address", etAddress.getText().toString().trim());
            data.put("city", actvCity.getText().toString().trim());
            data.put("country", actvCountry.getText().toString().trim());
            saveSectionToFirestore(data, "Billing data");
        } else {
            loadFromLocal();
        }
        isEditingBilling = false;
        setBillingFieldsEnabled(false);
        toggleActionsVisibility(layoutBillingActions, false);
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
        } else {
            loadFromLocal();
        }
        isEditingFitness = false;
        setFitnessFieldsEnabled(false);
        toggleActionsVisibility(layoutFitnessActions, false);
    }

    // signout

    private void setupSignOut(View view) {
        View btnSignOut = view.findViewById(R.id.btnSignOut);
        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Sign Out")
                            .setMessage("Are you sure you want to sign out? This will clear your offline data.")
                            .setPositiveButton("Sign Out", (dialog, which) -> {

                                //Clear Local User Profile Cache
                                sharedPreferences.edit().clear().apply();

                                // Clear Local Payment Cache
                                requireContext().getSharedPreferences("PaymentCache", Context.MODE_PRIVATE)
                                        .edit().clear().apply();

                                // Invalidate Firebase Session
                                mAuth.signOut();

                                // Redirect to Sign In Activity
                                Intent intent = new Intent(requireActivity(), SignInActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);

                            })
                            .setNegativeButton("Cancel", null)
                            .show());
        }
    }


    private void setupPersonalInfoEdit(View view) {
        view.findViewById(R.id.btnEditPersonal).setOnClickListener(v -> {
            isEditingPersonal = true;
            etFullName.setEnabled(true);
            etDob.setEnabled(true);
            toggleActionsVisibility(layoutEditActions, true);
        });
        view.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> exitPersonalEdit(false));
        view.findViewById(R.id.btnSavePersonal).setOnClickListener(v -> exitPersonalEdit(true));
        if (etDob != null) etDob.setOnClickListener(v -> {
            if (isEditingPersonal) showDatePicker();
        });
    }

    private void setupBillingEdit(View view) {
        view.findViewById(R.id.btnEditBilling).setOnClickListener(v -> {
            isEditingBilling = true;
            setBillingFieldsEnabled(true);
            toggleActionsVisibility(layoutBillingActions, true);
        });
        view.findViewById(R.id.btnCancelBilling).setOnClickListener(v -> exitBillingEdit(false));
        view.findViewById(R.id.btnSaveBilling).setOnClickListener(v -> exitBillingEdit(true));
    }

    private void setupFitnessEdit(View view) {
        view.findViewById(R.id.btnEditFitness).setOnClickListener(v -> {
            isEditingFitness = true;
            setFitnessFieldsEnabled(true);
            toggleActionsVisibility(layoutFitnessActions, true);
        });
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

    private void setBillingFieldsEnabled(boolean enabled) {
        etAddress.setEnabled(enabled);
        actvCity.setEnabled(enabled);
        actvCountry.setEnabled(enabled);
    }

    private void setFitnessFieldsEnabled(boolean enabled) {
        etWeight.setEnabled(enabled);
        etHeight.setEnabled(enabled);
        actvFitnessGoal.setEnabled(enabled);
        actvActivityLevel.setEnabled(enabled);
        actvWorkoutType.setEnabled(enabled);
    }

    private void updateBmiChip() {
        try {
            double w = Double.parseDouble(etWeight.getText().toString().trim());
            double h = Double.parseDouble(etHeight.getText().toString().trim());
            if (h <= 0) return;
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
        tvProfileSubtitle = view.findViewById(R.id.tvProfileSubtitle);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
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
        if (actvFitnessGoal != null)
            actvFitnessGoal.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, GOALS));
        if (actvActivityLevel != null)
            actvActivityLevel.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVELS));
        if (actvWorkoutType != null)
            actvWorkoutType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, WORKOUT_TYPES));
        if (actvCity != null)
            actvCity.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, CITIES));
        if (actvCountry != null)
            actvCountry.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, COUNTRIES));
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
                if (ivAvatar != null)
                    Glide.with(requireContext()).load(result.getData().getData()).circleCrop().into(ivAvatar);
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

    private void setupNavigation(View view) {
        // Back Button
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null)
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Settings Buttons
        View btnOpenSettings = view.findViewById(R.id.btnOpenSettings);
        View cardGoToSettings = view.findViewById(R.id.cardGoToSettings);

        View.OnClickListener navigateToSettings = v -> {
            try {
                // Ensure this ID matches
                Navigation.findNavController(v).navigate(R.id.nav_settings);
            } catch (IllegalArgumentException e) {
                Snackbar.make(view, "Settings routing not configured yet.", Snackbar.LENGTH_SHORT).show();
            }
        };

        if (btnOpenSettings != null) btnOpenSettings.setOnClickListener(navigateToSettings);
        if (cardGoToSettings != null) cardGoToSettings.setOnClickListener(navigateToSettings);
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }
}