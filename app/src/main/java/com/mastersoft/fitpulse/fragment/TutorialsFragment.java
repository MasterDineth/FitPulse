package com.mastersoft.fitpulse.fragment;

import com.mastersoft.fitpulse.R;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class TutorialsFragment extends Fragment {

    // Tutorial metadata: {title, level, duration}
    private static final String[][] TUTORIALS = {
            {"Perfect Pull-Up Form",  "Beginner",     "8 min"},
            {"Deadlift Masterclass",  "Advanced",     "15 min"},
            {"Core Strength 101",     "Intermediate", "12 min"},
            {"Cardio HIIT Circuit",   "Intermediate", "20 min"},
            {"Squat Variations",      "Beginner",     "10 min"},
            {"Shoulder Mobility",     "All Levels",   "7 min"},
    };

    private static final int[] CARD_IDS = {
            R.id.cardTutorial1,
            R.id.cardTutorial2,
            R.id.cardTutorial3,
            R.id.cardTutorial4,
            R.id.cardTutorial5,
            R.id.cardTutorial6,
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorials, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        setupToolbar(view);
        setupSearch(view);
        setupFeaturedCard(view);
        setupTutorialCards(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNav();
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.tutorialsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void setupSearch(View view) {
        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase();
                filterTutorials(view, query);
            }
        });
    }

    private void filterTutorials(View view, String query) {
        for (int i = 0; i < CARD_IDS.length; i++) {
            View card = view.findViewById(CARD_IDS[i]);
            if (card == null) continue;
            boolean matches = query.isEmpty()
                    || TUTORIALS[i][0].toLowerCase().contains(query)
                    || TUTORIALS[i][1].toLowerCase().contains(query);
            card.setVisibility(matches ? View.VISIBLE : View.GONE);
        }
    }

    private void setupFeaturedCard(View view) {
        View cardFeatured = view.findViewById(R.id.cardFeatured);
        View btnWatch     = view.findViewById(R.id.btnWatchFeatured);
        if (cardFeatured != null) cardFeatured.setOnClickListener(v -> openTutorial("Full Body Power"));
        if (btnWatch     != null) btnWatch.setOnClickListener(v -> openTutorial("Full Body Power"));
    }

    private void setupTutorialCards(View view) {
        for (int i = 0; i < CARD_IDS.length; i++) {
            View card = view.findViewById(CARD_IDS[i]);
            if (card == null) continue;
            final String title = TUTORIALS[i][0];
            card.setOnClickListener(v -> openTutorial(title));
        }
    }

    private void openTutorial(String title) {
        // TODO: launch video player Activity / Fragment with the tutorial content
        Snackbar.make(requireView(),
                "Opening: " + title, Snackbar.LENGTH_SHORT).show();
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

    public static TutorialsFragment newInstance() {
        return new TutorialsFragment();
    }
}