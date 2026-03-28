package com.mastersoft.fitpulse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mastersoft.fitpulse.R;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilMobile, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etMobile, etPassword, etConfirmPassword;
    private LinearProgressIndicator signUpProgress;
    private View layoutPasswordStrength;
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private TextView tvPasswordStrengthLabel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupPasswordStrengthWatcher();
        setupClickListeners();
    }


    private void bindViews() {
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        signUpProgress = findViewById(R.id.signUpProgress);
        layoutPasswordStrength = findViewById(R.id.layoutPasswordStrength);
        strengthBar1 = findViewById(R.id.strengthBar1);
        strengthBar2 = findViewById(R.id.strengthBar2);
        strengthBar3 = findViewById(R.id.strengthBar3);
        strengthBar4 = findViewById(R.id.strengthBar4);
        tvPasswordStrengthLabel = findViewById(R.id.tvPasswordStrengthLabel);
    }

    // Password Strength

    private void setupPasswordStrengthWatcher() {
        etPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                String pw = s.toString();
                if (pw.isEmpty()) {
                    layoutPasswordStrength.setVisibility(View.GONE);
                } else {
                    layoutPasswordStrength.setVisibility(View.VISIBLE);
                    updateStrengthBars(calculateStrength(pw));
                }
            }
        });
    }


    private int calculateStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}].*")) score++;
        return Math.max(1, score);
    }

    private void updateStrengthBars(int strength) {
        View[] bars = {strengthBar1, strengthBar2, strengthBar3, strengthBar4};
        int[] colors = {
                com.google.android.material.R.color.design_default_color_error,        // 1 weak
                com.google.android.material.R.color.design_default_color_error,        // 2 fair
                com.google.android.material.R.color.design_default_color_on_secondary, // 3 good
                com.google.android.material.R.color.design_default_color_secondary,    // 4 strong
        };
        String[] labels = {"Weak", "Fair", "Good", "Strong"};

        for (int i = 0; i < bars.length; i++) {
            bars[i].setBackgroundColor(i < strength
                    ? ContextCompat.getColor(this, colors[strength - 1])
                    : ContextCompat.getColor(this, com.google.android.material.R.color.m3_ref_palette_neutral_variant90));
        }
        tvPasswordStrengthLabel.setText(labels[strength - 1]);
        tvPasswordStrengthLabel.setTextColor(ContextCompat.getColor(this, colors[strength - 1]));
    }


    private void setupClickListeners() {
        // Create Account
        findViewById(R.id.btnCreateAccount).setOnClickListener(v -> attemptSignUp());

        findViewById(R.id.tvGoToSignIn).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    //Signup

    private void attemptSignUp() {
        // Clear previous errors
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilMobile.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String username = getText(etUsername);
        String email = getText(etEmail);
        String mobile = getText(etMobile);
        String password = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        // Validation
        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Username is required");
            tilUsername.requestFocus();
            return;
        }
        if (username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            tilUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            tilEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            tilEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(mobile)) {
            tilMobile.setError("Mobile number is required");
            tilMobile.requestFocus();
            return;
        }
        if (!android.util.Patterns.PHONE.matcher(mobile).matches() || mobile.length() < 9) {
            tilMobile.setError("Enter a valid mobile number");
            tilMobile.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            tilPassword.requestFocus();
            return;
        }
        if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            tilPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Passwords do not match");
            tilConfirmPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        // Firebase User Registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save userdata after account is created
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserProfile(user.getUid(), username, email, mobile);
                        }
                    } else {
                        // Auth failed
                        setLoadingState(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserProfile(String uid, String username, String email, String mobile) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("mobile", mobile);

        // Add the server timestamp for registration date
        userMap.put("dateRegistered", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    // Both Auth and Database write are successful
                    setLoadingState(false);
                    showSuccessAndRedirect(email);
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(SignUpActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void showSuccessAndRedirect(String email) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Account Created!")
                .setMessage("Your FitPulse account has been created successfully.\n\nSign in to start your fitness journey!")
                .setPositiveButton("Sign In Now", (dialog, which) -> {
                    // Navigate to SignIn and clear back stack so pressing back doesn't return here
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void setLoadingState(boolean loading) {
        signUpProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnCreateAccount).setEnabled(!loading);
        etUsername.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etMobile.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}