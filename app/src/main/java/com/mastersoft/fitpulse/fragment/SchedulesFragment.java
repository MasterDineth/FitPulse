package com.mastersoft.fitpulse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.mastersoft.fitpulse.R;

public class SchedulesFragment extends Fragment {

    // Workout data per category — {title, subtitle, exercises[]}
    private static final String[][] CATEGORY_DATA = {
            {"Upper Body", "4 exercises · ~45 min"},
            {"Lower Body", "5 exercises · ~50 min"},
            {"Core",       "4 exercises · ~30 min"},
            {"Cardio",     "3 exercises · ~40 min"},
            {"Full Body",  "6 exercises · ~60 min"},
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
        hideBottomNav();
        setupToolbar(view);
        setupChipGroup(view);
        setupStartWorkout(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNav();
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.schedulesToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void setupChipGroup(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupCategories);
        TextView tvTitle    = view.findViewById(R.id.tvTodayFocusTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvTodayFocusSubtitle);

        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            int index = 0;
            if (checkedId == R.id.chipLowerBody)  index = 1;
            else if (checkedId == R.id.chipCore)  index = 2;
            else if (checkedId == R.id.chipCardio) index = 3;
            else if (checkedId == R.id.chipFullBody) index = 4;

            if (tvTitle    != null) tvTitle.setText(CATEGORY_DATA[index][0]);
            if (tvSubtitle != null) tvSubtitle.setText(CATEGORY_DATA[index][1]);
        });
    }

    private void setupStartWorkout(View view) {
        View btnStart = view.findViewById(R.id.btnStartWorkout);
        if (btnStart != null) {
            btnStart.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Start Workout?")
                            .setMessage("Ready to begin your session? Your timer will start.")
                            .setPositiveButton("Let's Go!", (d, w) ->
                                    Snackbar.make(requireView(),
                                            "Workout started! 💪", Snackbar.LENGTH_SHORT).show())
                            .setNegativeButton("Not yet", null)
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

    public static SchedulesFragment newInstance() {
        return new SchedulesFragment();
    }
}