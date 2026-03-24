package com.mastersoft.fitpulse.fragment;
import com.mastersoft.fitpulse.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialElevationScale;
import com.google.android.material.transition.MaterialSharedAxis;
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

        // ── Settings button → open ProfileFragment ────────────────────────────
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> navigateToProfile(v));
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

    /**
     * Opens ProfileFragment with a MaterialElevationScale transition so the
     * avatar card appears to "expand" into the full profile screen.
     */
    private void navigateToProfile(View sharedElement) {
        ProfileFragment profileFragment = ProfileFragment.newInstance();

        // Exit current fragment with scale-down
        setExitTransition(new MaterialElevationScale(false));
        setReenterTransition(new MaterialElevationScale(true));

        // Profile enters with scale-up
        profileFragment.setEnterTransition(new MaterialElevationScale(true));
        profileFragment.setReturnTransition(new MaterialElevationScale(false));

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, profileFragment, "profile")
                .addToBackStack("profile")
                .commit();
    }

    /**
     * Opens CheckInFragment with a forward Z-axis push transition.
     */
    private void navigateToCheckIn() {
        CheckInFragment checkInFragment = CheckInFragment.newInstance();

        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        checkInFragment.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        checkInFragment.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, checkInFragment, "checkin")
                .addToBackStack("checkin")
                .commit();
    }
}
