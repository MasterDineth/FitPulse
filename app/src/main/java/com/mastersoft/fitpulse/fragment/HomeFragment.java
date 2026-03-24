package com.mastersoft.fitpulse.fragment;

import com.mastersoft.fitpulse.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

        // ── Avatar / profile image → open ProfileFragment ────────────────────
        View ivAvatar = view.findViewById(R.id.ivAvatar);
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> navigateToProfile(v));
        }

        // ── Check-In button → open CheckInFragment ───────────────────────────
        View btnCheckIn = view.findViewById(R.id.btnCheckIn);
        if (btnCheckIn != null) {
            btnCheckIn.setOnClickListener(v -> navigateToCheckIn());
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

        // open settings fragment
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> navigateToSettings());
        }

        // open stats fragment
        View seeAllStats = view.findViewById(R.id.seeAllStats);
        if (seeAllStats != null) {
            seeAllStats.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateTo(R.id.nav_stats);
                }
            });
        }
        //open see tutorials
        View seeAllTutorials = view.findViewById(R.id.seeAllTutorials);
        navigateTo(seeAllTutorials,R.id.nav_tutorials);

        //open all schedules
        View seeAllSchedules = view.findViewById(R.id.tvSeeAllSchedules);
        navigateTo(seeAllSchedules,R.id.nav_schedule);

    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToProfile(View sharedElement) {
        Navigation.findNavController(sharedElement).navigate(R.id.nav_profile);
    }

    private void navigateToCheckIn() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_checkin);
    }

    private void navigateToSettings() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_settings);
    }

    private void navigateTo(View view,int fragment){

        if (view != null) {
            view.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateTo(fragment);
                }
            });
        }
    }

}
