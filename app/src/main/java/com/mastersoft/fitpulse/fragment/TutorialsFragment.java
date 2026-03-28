package com.mastersoft.fitpulse.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.data.TutorialManager;
import com.mastersoft.fitpulse.model.Tutorial;

import java.util.ArrayList;
import java.util.List;

public class TutorialsFragment extends Fragment {

    private RecyclerView rvTutorials;
    private TutorialAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorials, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hideBottomNav();
        setupToolbar(view);
        setupFeaturedCard(view);
        setupSearch(view);

        // Setup RecyclerView with a 2 column grid
        rvTutorials = view.findViewById(R.id.rvTutorials);
        rvTutorials.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new TutorialAdapter();
        rvTutorials.setAdapter(adapter);

        // Fetch the data from our global TutorialManager
        List<Tutorial> tutorials = TutorialManager.getInstance().getAllTutorials();
        adapter.setTutorials(tutorials);
    }



    private void setupSearch(View view) {
        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {

                adapter.filter(s.toString().trim());
            }
        });
    }

    private void setupFeaturedCard(View view) {
        View cardFeatured = view.findViewById(R.id.cardFeatured);
        View btnWatch     = view.findViewById(R.id.btnWatchFeatured);


        String featuredUrl = "https://www.youtube.com/results?search_query=full+body+power+workout";

        if (cardFeatured != null) cardFeatured.setOnClickListener(v -> openYouTube(featuredUrl));
        if (btnWatch != null) btnWatch.setOnClickListener(v -> openYouTube(featuredUrl));
    }

    private void openYouTube(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }



    private class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.ViewHolder> {

        private List<Tutorial> originalList = new ArrayList<>();
        private List<Tutorial> filteredList = new ArrayList<>();

        // Aesthetic color palette for the cards
        private final String[] colors = {"#E3F2FD", "#FFF9E6", "#FCE4EC", "#E8F5E9", "#F3E5F5"};

        public void setTutorials(List<Tutorial> tutorials) {
            this.originalList = new ArrayList<>(tutorials);
            this.filteredList = new ArrayList<>(tutorials);
            notifyDataSetChanged();
        }

        public void filter(String query) {
            filteredList.clear();
            if (query.isEmpty()) {
                filteredList.addAll(originalList);
            } else {
                String q = query.toLowerCase();
                for (Tutorial t : originalList) {
                    if (t.getTitle().toLowerCase().contains(q) || t.getLevel().toLowerCase().contains(q)) {
                        filteredList.add(t);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutorial, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tutorial tutorial = filteredList.get(position);

            holder.tvEmoji.setText(tutorial.getEmoji());
            holder.tvTitle.setText(tutorial.getTitle());
            holder.tvLevel.setText(tutorial.getLevel());
            holder.tvDuration.setText("▶ " + tutorial.getDuration());

            // Assign a dynamic background color
            holder.card.setCardBackgroundColor(Color.parseColor(colors[position % colors.length]));

            // Redirect to YouTube url
            holder.card.setOnClickListener(v -> openYouTube(tutorial.getVideoUrl()));
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView tvEmoji, tvTitle, tvLevel, tvDuration;

            ViewHolder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardTutorial);
                tvEmoji = itemView.findViewById(R.id.tvEmoji);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvLevel = itemView.findViewById(R.id.tvLevel);
                tvDuration = itemView.findViewById(R.id.tvDuration);
            }
        }
    }


    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.tutorialsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void hideBottomNav() {
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() != null) {
            View nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setVisibility(View.VISIBLE);
        }
    }

    public static TutorialsFragment newInstance() {
        return new TutorialsFragment();
    }
}