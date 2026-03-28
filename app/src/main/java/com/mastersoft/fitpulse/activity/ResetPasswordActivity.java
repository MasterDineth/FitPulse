package com.mastersoft.fitpulse.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.mastersoft.fitpulse.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private LinearProgressIndicator resetProgress;

    private View cardEmailEntry;
    private View cardSuccess;
    private TextView tvSuccessMessage;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupClickListeners();
    }


    private void bindViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        resetProgress = findViewById(R.id.resetProgress);
        cardEmailEntry = findViewById(R.id.cardEmailEntry);
        cardSuccess = findViewById(R.id.cardSuccess);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
    }


    private void setupClickListeners() {
        // Send reset link
        findViewById(R.id.btnSendResetLink).setOnClickListener(v -> attemptSendResetLink());

        // Open Email App
        findViewById(R.id.btnOpenEmailApp).setOnClickListener(v -> openEmailApp());

        // Resend link
        View tvResend = findViewById(R.id.tvResendLink);
        if (tvResend != null) {
            tvResend.setOnClickListener(v -> {
                transitionToEmailState();
                Snackbar.make(tvResend, "Enter your email to resend the link", Snackbar.LENGTH_SHORT).show();
            });
        }


        View tvBackToSignIn = findViewById(R.id.tvBackToSignIn);
        if (tvBackToSignIn != null) {
            tvBackToSignIn.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    // pass reset

    private void attemptSendResetLink() {
        tilEmail.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email address is required");
            tilEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            tilEmail.requestFocus();
            return;
        }

        // Show loading
        setLoadingState(true);

        // Firebase Password Reset
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        transitionToSuccessState(email);
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Snackbar.make(findViewById(android.R.id.content), errorMsg, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void transitionToSuccessState(String email) {
        tvSuccessMessage.setText("Password reset link successfully sent to\n" + email);

        cardEmailEntry.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    cardEmailEntry.setVisibility(View.GONE);

                    cardSuccess.setAlpha(0f);
                    cardSuccess.setVisibility(View.VISIBLE);
                    cardSuccess.animate()
                            .alpha(1f)
                            .translationYBy(0f)
                            .setDuration(350)
                            .start();
                })
                .start();
    }

    private void transitionToEmailState() {
        cardSuccess.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    cardSuccess.setVisibility(View.GONE);
                    etEmail.setText("");
                    tilEmail.setError(null);

                    cardEmailEntry.setAlpha(0f);
                    cardEmailEntry.setVisibility(View.VISIBLE);
                    cardEmailEntry.animate().alpha(1f).setDuration(300).start();
                })
                .start();
    }

    //Open Email App

    private void openEmailApp() {
        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (emailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(emailIntent);
        } else {
            Intent gmailIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (gmailIntent != null) {
                startActivity(gmailIntent);
            } else {
                Intent uriIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (uriIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(uriIntent);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No email app found. Please check your inbox manually.", Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void setLoadingState(boolean loading) {
        resetProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        View btnSend = findViewById(R.id.btnSendResetLink);
        if (btnSend != null) btnSend.setEnabled(!loading);
        etEmail.setEnabled(!loading);
    }
}