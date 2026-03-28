package com.mastersoft.fitpulse.fragment;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.data.WorkoutHistoryRepository;
import com.mastersoft.fitpulse.data.WorkoutRepository;
import com.mastersoft.fitpulse.model.WorkoutPlan;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchedulesFragment extends Fragment {

    private List<WorkoutPlan> workoutPlans = new ArrayList<>();
    private WorkoutPlan currentPlan = null;

    private boolean isWorkoutRunning = false;
    private boolean isWorkoutPaused = false;
    private long elapsedSeconds = 0L;
    private long pauseStartMs = 0L;
    private long workoutStartMs = 0L;
    private long totalPausedMs = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private static final int ESTIMATED_DURATION_SEC = 45 * 60;

    private final boolean[] exerciseFinished = new boolean[6];

    private View rootView;
    private ChipGroup chipGroup;
    private TextView tvFocusTitle, tvFocusSubtitle;
    private View cardTimerBar;
    private TextView tvTimerLabel, tvElapsedTime;
    private LinearProgressIndicator timerProgressBar, loadingIndicator;
    private MaterialButton btnPauseResume, btnStopWorkout, btnStartWorkout;
    private View cardError;
    private TextView tvErrorMessage;

    private final TextView[] tvNames = new TextView[6];
    private final TextView[] tvDetails = new TextView[6];
    private final MaterialButton[] btnFinish = new MaterialButton[6];
    private final MaterialCardView[] badges = new MaterialCardView[6];

    private static final int[] NAME_IDS = {
            R.id.tvExercise1Name, R.id.tvExercise2Name, R.id.tvExercise3Name,
            R.id.tvExercise4Name, R.id.tvExercise5Name, R.id.tvExercise6Name
    };
    private static final int[] DETAIL_IDS = {
            R.id.tvExercise1Detail, R.id.tvExercise2Detail, R.id.tvExercise3Detail,
            R.id.tvExercise4Detail, R.id.tvExercise5Detail, R.id.tvExercise6Detail
    };
    private static final int[] FINISH_IDS = {
            R.id.btnFinish1, R.id.btnFinish2, R.id.btnFinish3,
            R.id.btnFinish4, R.id.btnFinish5, R.id.btnFinish6
    };
    private static final int[] BADGE_IDS = {
            R.id.badgeNum1, R.id.badgeNum2, R.id.badgeNum3,
            R.id.badgeNum4, R.id.badgeNum5, R.id.badgeNum6
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedules, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        hideBottomNav();
        bindViews(view);
        setupToolbar(view);
        setupButtons();
        loadWorkoutPlans(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        showBottomNav();
    }

    private void bindViews(View view) {
        chipGroup = view.findViewById(R.id.chipGroupCategories);
        tvFocusTitle = view.findViewById(R.id.tvTodayFocusTitle);
        tvFocusSubtitle = view.findViewById(R.id.tvTodayFocusSubtitle);
        cardTimerBar = view.findViewById(R.id.cardTimerBar);
        tvTimerLabel = view.findViewById(R.id.tvTimerWorkoutLabel);
        tvElapsedTime = view.findViewById(R.id.tvElapsedTime);
        timerProgressBar = view.findViewById(R.id.timerProgressBar);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        btnPauseResume = view.findViewById(R.id.btnPauseResume);
        btnStopWorkout = view.findViewById(R.id.btnStopWorkout);
        btnStartWorkout = view.findViewById(R.id.btnStartWorkout);
        cardError = view.findViewById(R.id.cardError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);

        for (int i = 0; i < 6; i++) {
            tvNames[i] = view.findViewById(NAME_IDS[i]);
            tvDetails[i] = view.findViewById(DETAIL_IDS[i]);
            btnFinish[i] = view.findViewById(FINISH_IDS[i]);
            badges[i] = view.findViewById(BADGE_IDS[i]);
        }
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.schedulesToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (isWorkoutRunning) {
                    confirmBackDuringWorkout();
                } else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void loadWorkoutPlans(boolean forceRefresh) {
        setLoading(true);
        cardError.setVisibility(View.GONE);

        WorkoutRepository.getInstance(requireContext())
                .getWorkoutPlans(forceRefresh, new WorkoutRepository.WorkoutPlansCallback() {
                    @Override
                    public void onCacheLoaded(List<WorkoutPlan> plans) {
                        workoutPlans = plans;
                        populateChips(plans);
                        if (!plans.isEmpty()) selectPlan(plans.get(0));
                        setLoading(false);
                    }

                    @Override
                    public void onNetworkRefreshed(List<WorkoutPlan> plans) {
                        workoutPlans = plans;
                        String currentName = currentPlan != null ? currentPlan.getName() : "";
                        populateChips(plans);

                        WorkoutPlan reselect = findPlanByName(currentName);
                        if (reselect == null && !plans.isEmpty()) reselect = plans.get(0);
                        if (reselect != null) selectPlan(reselect);
                        setLoading(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        if (workoutPlans.isEmpty()) {
                            tvErrorMessage.setText("Failed to load workouts: " + e.getMessage());
                            cardError.setVisibility(View.VISIBLE);
                        }
                    }
                });

        rootView.findViewById(R.id.btnRetry).setOnClickListener(v -> loadWorkoutPlans(true));
    }

    private void populateChips(List<WorkoutPlan> plans) {
        chipGroup.removeAllViews();
        boolean firstChip = true;
        for (WorkoutPlan plan : plans) {
            Chip chip = new Chip(requireContext());
            chip.setText(plan.getName());
            chip.setCheckable(true);
            chip.setChecked(firstChip);
            chip.setTag(plan);
            chip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_chip_background_color);
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    WorkoutPlan selected = (WorkoutPlan) btn.getTag();
                    if (selected != null) selectPlan(selected);
                }
            });
            chipGroup.addView(chip);
            firstChip = false;
        }
    }

    private void selectPlan(WorkoutPlan plan) {
        currentPlan = plan;
        String[] exercises = plan.getExercises();
        tvFocusTitle.setText(plan.getName());
        tvFocusSubtitle.setText(buildExerciseCountLabel(exercises));
        tvTimerLabel.setText(plan.getName());

        for (int i = 0; i < 6; i++) {
            String[] parsed = WorkoutPlan.parseExercise(exercises[i]);
            tvNames[i].setText(parsed[0]);
            tvDetails[i].setText(parsed[1]);
        }
    }

    private String buildExerciseCountLabel(String[] exercises) {
        int count = 0;
        for (String ex : exercises) if (ex != null && !ex.isEmpty()) count++;
        return count + " exercises · ~45 min";
    }

    private WorkoutPlan findPlanByName(String name) {
        for (WorkoutPlan plan : workoutPlans) {
            if (plan.getName().equals(name)) return plan;
        }
        return null;
    }

    private void setupButtons() {
        btnStartWorkout.setOnClickListener(v -> startWorkout());
        btnPauseResume.setOnClickListener(v -> togglePause());
        btnStopWorkout.setOnClickListener(v -> confirmStopWorkout());

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            btnFinish[i].setOnClickListener(v -> markExerciseDone(idx));
        }
    }

    private void startWorkout() {
        if (currentPlan == null) {
            Snackbar.make(requireView(), "No workout selected", Snackbar.LENGTH_SHORT).show();
            return;
        }
        isWorkoutRunning = true;
        isWorkoutPaused = false;
        elapsedSeconds = 0;
        totalPausedMs = 0;
        workoutStartMs = SystemClock.elapsedRealtime();

        for (int i = 0; i < 6; i++) exerciseFinished[i] = false;

        animateIn(cardTimerBar);
        btnStartWorkout.setVisibility(View.GONE);
        for (int i = 0; i < 6; i++) {
            btnFinish[i].setVisibility(View.VISIBLE);
            badges[i].setVisibility(View.GONE);
        }

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            chipGroup.getChildAt(i).setEnabled(false);
        }

        timerProgressBar.setMax(ESTIMATED_DURATION_SEC);
        timerProgressBar.setProgress(0);
        startTimerTick();

        sendWorkoutNotification("Workout Started", "You started " + currentPlan.getName() + "!", 1001);
    }

    private void startTimerTick() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isWorkoutRunning || isWorkoutPaused) return;

                long nowMs = SystemClock.elapsedRealtime();
                long activeMs = nowMs - workoutStartMs - totalPausedMs;
                elapsedSeconds = activeMs / 1000L;

                tvElapsedTime.setText(formatTime(elapsedSeconds));
                timerProgressBar.setProgressCompat((int) Math.min(elapsedSeconds, ESTIMATED_DURATION_SEC), true);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        isWorkoutRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
    }

    private void togglePause() {
        if (isWorkoutPaused) {
            long pauseDuration = SystemClock.elapsedRealtime() - pauseStartMs;
            totalPausedMs += pauseDuration;
            isWorkoutPaused = false;
            btnPauseResume.setText("Pause");
            btnPauseResume.setIconResource(R.drawable.ic_pause);
            startTimerTick();
            startPulseDot(rootView.findViewById(R.id.timerPulseDot));
        } else {
            pauseStartMs = SystemClock.elapsedRealtime();
            isWorkoutPaused = true;
            btnPauseResume.setText("Resume");
            btnPauseResume.setIconResource(R.drawable.ic_play);
            timerHandler.removeCallbacksAndMessages(null);
            rootView.findViewById(R.id.timerPulseDot).animate().alpha(0.3f).setDuration(300).start();
        }
    }

    private void confirmStopWorkout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("End Workout?")
                .setMessage("Save this session to your workout history and stop the timer?")
                .setPositiveButton("Save & End", (dialog, which) -> finishWorkout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finishWorkout() {
        stopTimer();

        if (currentPlan == null) return;

        String workoutType = currentPlan.getName();
        double userWeightKg = getUserWeightKg();

        WorkoutHistoryRepository.getInstance()
                .saveSession(workoutType, elapsedSeconds, userWeightKg,
                        new WorkoutHistoryRepository.SaveCallback() {
                            @Override
                            public void onSuccess(String documentId) {
                                showWorkoutSummaryDialog(workoutType, elapsedSeconds);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Snackbar.make(requireView(),
                                        "Saved locally — will sync when online",
                                        Snackbar.LENGTH_LONG).show();
                                resetWorkoutUI();
                            }
                        });
    }

    private void showWorkoutSummaryDialog(String type, long seconds) {
        int calories = (int) Math.round(5.0 * getUserWeightKg() * (seconds / 3600.0));

        sendWorkoutNotification("Workout Complete!", "You burned ~" + calories + " kcal. Great job!", 1002);

        String message = "Workout type: " + type + "\n"
                + "Duration: " + formatTime(seconds) + "\n"
                + "Calories burned: ~" + calories + " kcal\n\n"
                + "Great work!";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Session Complete!")
                .setMessage(message)
                .setPositiveButton("Done", (d, w) -> resetWorkoutUI())
                .setCancelable(false)
                .show();
    }

    private void resetWorkoutUI() {
        isWorkoutRunning = false;
        isWorkoutPaused = false;
        elapsedSeconds = 0;

        animateOut(cardTimerBar);
        btnStartWorkout.setVisibility(View.VISIBLE);
        btnPauseResume.setText("Pause");
        btnPauseResume.setIconResource(R.drawable.ic_pause);

        for (int i = 0; i < 6; i++) {
            btnFinish[i].setVisibility(View.GONE);
            badges[i].setVisibility(View.VISIBLE);
            resetFinishButton(i);
        }

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            chipGroup.getChildAt(i).setEnabled(true);
        }

        tvElapsedTime.setText("00:00");
        timerProgressBar.setProgress(0);
    }

    private void markExerciseDone(int index) {
        if (exerciseFinished[index]) {
            exerciseFinished[index] = false;
            resetFinishButton(index);
            return;
        }

        exerciseFinished[index] = true;

        View card = getExerciseCard(index);
        if (card instanceof MaterialCardView) {
            ((MaterialCardView) card).setCardBackgroundColor(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(),
                                    com.google.android.material.R.color.material_dynamic_tertiary90)));
        }

        btnFinish[index].setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(),
                                com.google.android.material.R.color.material_dynamic_tertiary40)));
        btnFinish[index].setIconTintResource(android.R.color.white);

        boolean allDone = true;
        for (boolean done : exerciseFinished)
            if (!done) {
                allDone = false;
                break;
            }
        if (allDone) {
            Snackbar.make(requireView(), "All exercises complete! 🎉 Tap End Workout to save.",
                            Snackbar.LENGTH_LONG)
                    .setAction("End Now", v -> confirmStopWorkout())
                    .show();
        }
    }

    private void resetFinishButton(int index) {
        if (getContext() == null) return;
        View card = getExerciseCard(index);
        if (card instanceof MaterialCardView) {
            ((MaterialCardView) card).setCardBackgroundColor(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(),
                                    com.google.android.material.R.color.material_dynamic_neutral95)));
        }
        btnFinish[index].setBackgroundTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(),
                                com.google.android.material.R.color.material_dynamic_neutral_variant90)));
        btnFinish[index].setIconTintResource(
                com.google.android.material.R.color.material_dynamic_neutral_variant40);
    }

    private View getExerciseCard(int index) {
        int[] cardIds = {
                R.id.cardExercise1, R.id.cardExercise2, R.id.cardExercise3,
                R.id.cardExercise4, R.id.cardExercise5, R.id.cardExercise6
        };
        return rootView.findViewById(cardIds[index]);
    }

    private void confirmBackDuringWorkout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Leave Workout?")
                .setMessage("Your current workout progress will be lost. Are you sure?")
                .setPositiveButton("Leave", (d, w) -> {
                    stopTimer();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Keep going", null)
                .show();
    }

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.setTranslationY(-20f);
        view.animate().alpha(1f).translationY(0f).setDuration(350).start();
        startPulseDot(rootView.findViewById(R.id.timerPulseDot));
    }

    private void animateOut(View view) {
        view.animate().alpha(0f).translationY(-20f).setDuration(250)
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setTranslationY(0f);
                }).start();
    }

    private void startPulseDot(View dot) {
        if (dot == null) return;
        ObjectAnimator pulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.2f);
        pulse.setDuration(700);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.start();
        dot.setTag(pulse);
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private double getUserWeightKg() {
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("fitpulse_user_profile", android.content.Context.MODE_PRIVATE);
        return Double.longBitsToDouble(prefs.getLong("weight_kg", Double.doubleToLongBits(70.0)));
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

    private void sendWorkoutNotification(String title, String message, int notificationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "FITPULSE_CHANNEL")
                .setSmallIcon(R.drawable.ic_pulse)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    public static SchedulesFragment newInstance() {
        return new SchedulesFragment();
    }
}