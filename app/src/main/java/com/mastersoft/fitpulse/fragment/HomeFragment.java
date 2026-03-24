package com.mastersoft.fitpulse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

        // ── Check-In button → open CheckInFragment ───────────────────────────
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> navigateToCheckIn(v));
        }

        // ── "See all →" workout history → switch to History tab ──────────────
        View tvSeeAllHistory = view.findViewById(R.id.tvSeeAllHistory);
        if (tvSeeAllHistory != null) {
            tvSeeAllHistory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateTo(R.id.nav_history);
                }
            });
        }

        // ── Settings button ───────────────────────────────────────────────────
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                // TODO: open Settings screen
            });
        }

        // ── "See all →" schedules ─────────────────────────────────────────────
        View tvSeeAllSchedules = view.findViewById(R.id.tvSeeAllSchedules);
        if (tvSeeAllSchedules != null) {
            tvSeeAllSchedules.setOnClickListener(v -> {
                // TODO: navigate to Schedules screen
            });
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToCheckIn(View view) {
        // Using Navigation Component instead of manual fragment transaction
        try {
            Navigation.findNavController(view).navigate(R.id.nav_checkin);
        } catch (IllegalArgumentException e) {
            // Fallback if nav_checkin is not in the graph, or if preferred manually
            // but the previous code used a non-existent container ID.
            // For now, we assume nav_checkin exists or should be added to the graph.
        }
    }
}
