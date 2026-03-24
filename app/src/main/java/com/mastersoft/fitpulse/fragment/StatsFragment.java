package com.mastersoft.fitpulse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.mastersoft.fitpulse.R;

public class StatsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        setupToolbar(view);

        // Update health data button
        View btnUpdate = view.findViewById(R.id.btnUpdateHealthData);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v ->
                    Snackbar.make(requireView(), "Syncing health data…", Snackbar.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNav();
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.statsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
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

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }
}