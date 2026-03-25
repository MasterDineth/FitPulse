package com.mastersoft.fitpulse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mastersoft.fitpulse.R;



public class SignInActivity extends AppCompatActivity {

    private TextInputLayout   tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private LinearProgressIndicator signInProgress;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupEntranceAnimations();
        setupClickListeners();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        tilEmail       = findViewById(R.id.tilEmail);
        tilPassword    = findViewById(R.id.tilPassword);
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        signInProgress = findViewById(R.id.signInProgress);
    }

    // ── Entrance animations ───────────────────────────────────────────────────

    private void setupEntranceAnimations() {
        // Blob fade-in
        View blobTL = findViewById(R.id.blobTopLeft);
        View blobBR = findViewById(R.id.blobBottomRight);
        if (blobTL != null) blobTL.animate().alpha(0.45f).setDuration(800).start();
        if (blobBR != null) blobBR.animate().alpha(0.35f).setDuration(900).setStartDelay(100).start();
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        // Sign In
        findViewById(R.id.btnSignIn).setOnClickListener(v -> attemptSignIn());

        // Forgot password
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        // Go to Sign Up
        findViewById(R.id.btnGoToSignUp).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    // ign-in logic

    private void attemptSignIn() {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Validation
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

        // Show loading
        setLoadingState(true);

        // TODO: replace with real auth call (Firebase Auth / your backend)
        // Firebase Auth Sign In
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        // On success — navigate to MainActivity and clear back stack
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        // If sign in fails, display a message to the user
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_LONG).show();
                    }
                });


        // Simulated 1.5s network delay:
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            setLoadingState(false);
//            // On success — navigate to MainActivity and clear back stack
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(intent);
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//        }, 1500);
    }

    private void setLoadingState(boolean loading) {
        signInProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnSignIn).setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
}