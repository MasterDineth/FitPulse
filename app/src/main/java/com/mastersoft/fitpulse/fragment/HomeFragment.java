package com.mastersoft.fitpulse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.activity.MainActivity;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // "See all →" in Workout History section → switch to History tab
        View tvSeeAllHistory = view.findViewById(R.id.tvSeeAllHistory);
        if (tvSeeAllHistory != null) {
            tvSeeAllHistory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateTo(R.id.nav_history);
                }
            });
        }

        // "See all →" in Schedules section — extend as needed
        View tvSeeAllSchedules = view.findViewById(R.id.tvSeeAllSchedules);
        if (tvSeeAllSchedules != null) {
            tvSeeAllSchedules.setOnClickListener(v -> {
                // TODO: navigate to Schedules screen
            });
        }

        // Check-In button
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> {
                // TODO: handle check-in action
            });
        }

        // Settings button
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                // TODO: open Settings screen
            });
        }
    }
}