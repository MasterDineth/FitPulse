package com.mastersoft.fitpulse.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.mastersoft.fitpulse.R;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout   tilEmail;
    private TextInputEditText etEmail;
    private LinearProgressIndicator resetProgress;

    private View cardEmailEntry;
    private View cardSuccess;
    private TextView tvSuccessMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        bindViews();
        setupClickListeners();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        tilEmail        = findViewById(R.id.tilEmail);
        etEmail         = findViewById(R.id.etEmail);
        resetProgress   = findViewById(R.id.resetProgress);
        cardEmailEntry  = findViewById(R.id.cardEmailEntry);
        cardSuccess     = findViewById(R.id.cardSuccess);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        // Send reset link
        findViewById(R.id.btnSendResetLink).setOnClickListener(v -> attemptSendResetLink());

        // Open Email App
        findViewById(R.id.btnOpenEmailApp).setOnClickListener(v -> openEmailApp());

        // Resend link
        View tvResend = findViewById(R.id.tvResendLink);
        if (tvResend != null) {
            tvResend.setOnClickListener(v -> {
                // Swap back to email entry state so user can re-trigger
                transitionToEmailState();
                Snackbar.make(tvResend,
                        "Enter your email to resend the link", Snackbar.LENGTH_SHORT).show();
            });
        }

        // Back to Sign In (shown in both states)
        View tvBackToSignIn = findViewById(R.id.tvBackToSignIn);
        if (tvBackToSignIn != null) {
            tvBackToSignIn.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    // ── Reset logic ───────────────────────────────────────────────────────────

    private void attemptSendResetLink() {
        tilEmail.setError(null);

        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";

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

        // TODO: replace with real password reset call (Firebase / your backend)
        // Simulated 1.5s network delay:
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setLoadingState(false);
            transitionToSuccessState(email);
        }, 1500);
    }

    // ── State transitions ─────────────────────────────────────────────────────

    /** Fades out the email entry card and fades in the success card. */
    private void transitionToSuccessState(String email) {
        // Update success message with the real email
        tvSuccessMessage.setText(
                "Password reset link successfully sent to\n" + email);

        // Animate out the form card
        cardEmailEntry.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    cardEmailEntry.setVisibility(View.GONE);

                    // Animate in the success card
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

    /** Fades back to the email entry card (for Resend flow). */
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

    // ── Open Email App ────────────────────────────────────────────────────────

    /**
     * Fires an Intent that opens the system email client so the user can
     * immediately find the reset email without leaving the flow.
     */
    private void openEmailApp() {
        Intent emailIntent = new Intent(Intent.ACTION_MAIN);
        emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (emailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(emailIntent);
        } else {
            // Fallback: open Gmail via package name if it is installed
            Intent gmailIntent = getPackageManager()
                    .getLaunchIntentForPackage("com.google.android.gm");
            if (gmailIntent != null) {
                startActivity(gmailIntent);
            } else {
                // Ultimate fallback — open email via URI scheme
                Intent uriIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("mailto:"));
                uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (uriIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(uriIntent);
                } else {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "No email app found. Please check your inbox manually.",
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    private void setLoadingState(boolean loading) {
        resetProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        View btnSend = findViewById(R.id.btnSendResetLink);
        if (btnSend != null) btnSend.setEnabled(!loading);
        etEmail.setEnabled(!loading);
    }
}