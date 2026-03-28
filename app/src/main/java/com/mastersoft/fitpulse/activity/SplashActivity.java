package com.mastersoft.fitpulse.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.mastersoft.fitpulse.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    // Total duration before launching MainActivity
    private static final int SPLASH_DURATION = 3000;

    // Animation timing offsets
    private static final int DELAY_BLOBS = 0;
    private static final int DELAY_ICON = 150;
    private static final int DELAY_APP_NAME = 450;
    private static final int DELAY_TAGLINE = 600;
    private static final int DELAY_CIRCULAR = 750;
    private static final int DELAY_LINEAR = 900;
    private static final int LINEAR_FILL_DURATION = 1800;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View blobTopLeft = findViewById(R.id.blobTopLeft);
        View blobBottomRight = findViewById(R.id.blobBottomRight);
        View iconContainer = findViewById(R.id.iconContainer);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);
        View progressContainer = findViewById(R.id.progressContainer);
        CircularProgressIndicator circularProgress = findViewById(R.id.circularProgress);
        LinearProgressIndicator linearProgress = findViewById(R.id.linearProgress);

        handler.postDelayed(() -> {
            animateBlobIn(blobTopLeft, 0, 0.55f, 700);
            animateBlobIn(blobBottomRight, 120, 0.45f, 800);
        }, DELAY_BLOBS);

        handler.postDelayed(() -> {
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(iconContainer, View.ALPHA, 0f, 1f);
            ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(iconContainer, View.SCALE_X, 0.55f, 1f);
            ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(iconContainer, View.SCALE_Y, 0.55f, 1f);

            alphaAnim.setDuration(350);
            alphaAnim.setInterpolator(new DecelerateInterpolator());
            scaleXAnim.setDuration(520);
            scaleXAnim.setInterpolator(new OvershootInterpolator(1.6f));
            scaleYAnim.setDuration(520);
            scaleYAnim.setInterpolator(new OvershootInterpolator(1.6f));

            AnimatorSet iconSet = new AnimatorSet();
            iconSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
            iconSet.start();

            iconSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    startIconPulse(iconContainer);
                }
            });
        }, DELAY_ICON);

        handler.postDelayed(() -> fadeSlideUp(tvAppName, 0, 420), DELAY_APP_NAME);

        handler.postDelayed(() -> fadeSlideUp(tvTagline, 0, 420), DELAY_TAGLINE);

        handler.postDelayed(() -> fadeSlideUp(progressContainer, 0, 400), DELAY_CIRCULAR);


        handler.postDelayed(() -> {
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(linearProgress, View.ALPHA, 0f, 1f);
            fadeIn.setDuration(300);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            fadeIn.start();


            ValueAnimator progressAnim = ValueAnimator.ofInt(0, 100);
            progressAnim.setDuration(LINEAR_FILL_DURATION);
            progressAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            progressAnim.addUpdateListener(anim ->
                    linearProgress.setProgressCompat((int) anim.getAnimatedValue(), true));
            progressAnim.start();

        }, DELAY_LINEAR);

        // ── Stage 6 : navigate to MainActivity ───────────────────────────────
        handler.postDelayed(this::launchMain, SPLASH_DURATION);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────


    private void fadeSlideUp(View view, float startY, long duration) {
        view.setTranslationY(startY == 0 ? 28f : startY);
        view.setAlpha(0f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f);
        alpha.setDuration(duration);
        alpha.setInterpolator(new DecelerateInterpolator(2f));

        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 28f, 0f);
        translateY.setDuration(duration);
        translateY.setInterpolator(new DecelerateInterpolator(2f));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, translateY);
        set.start();
    }


    private void animateBlobIn(View blob, long startDelay, float targetAlpha, long duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(blob, View.ALPHA, 0f, targetAlpha);
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }


    private void startIconPulse(View view) {
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.06f);
        scaleUp.setDuration(700);
        scaleUp.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.06f);
        scaleUpY.setDuration(700);
        scaleUpY.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.06f, 1f);
        scaleDown.setDuration(700);
        scaleDown.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.06f, 1f);
        scaleDownY.setDuration(700);
        scaleDownY.setInterpolator(new DecelerateInterpolator());

        AnimatorSet pulseUp = new AnimatorSet();
        pulseUp.playTogether(scaleUp, scaleUpY);

        AnimatorSet pulseDown = new AnimatorSet();
        pulseDown.playTogether(scaleDown, scaleDownY);

        AnimatorSet pulse = new AnimatorSet();
        pulse.playSequentially(pulseUp, pulseDown);
        pulse.setStartDelay(200);


        pulse.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pulse.start();
            }
        });
        pulse.start();
    }

    private void launchMain() {
        // Check Firebase Auth
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User is already logged in
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            // No user found
            startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}