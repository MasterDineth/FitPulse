package com.mastersoft.fitpulse.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.adapter.WorkoutAdapter;
import com.mastersoft.fitpulse.model.Workout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryFragment extends Fragment {

    private TextView tvSessionCount, tvTotalCals, tvAvgTime;
    private RecyclerView recyclerViewHistory;
    private WorkoutAdapter adapter;
    private List<Workout> workoutList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        tvTotalCals = view.findViewById(R.id.tvTotalCals);
        tvAvgTime = view.findViewById(R.id.tvAvgTime);
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        workoutList = new ArrayList<>();
        adapter = new WorkoutAdapter(workoutList);
        recyclerViewHistory.setAdapter(adapter);

        fetchWorkoutData();

        return view;
    }

    private void fetchWorkoutData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("workoutHistory")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        workoutList.clear();

                        int monthlySessions = 0;
                        int totalCalories = 0;
                        int totalTime = 0;

                        Calendar currentCal = Calendar.getInstance();
                        int currentMonth = currentCal.get(Calendar.MONTH);
                        int currentYear = currentCal.get(Calendar.YEAR);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Workout workout = document.toObject(Workout.class);
                            workoutList.add(workout);

                            // Calculate stats for the current month
                            if (workout.getDate() != null) {
                                Calendar workoutCal = Calendar.getInstance();
                                workoutCal.setTime(workout.getDate());

                                if (workoutCal.get(Calendar.MONTH) == currentMonth &&
                                        workoutCal.get(Calendar.YEAR) == currentYear) {

                                    monthlySessions++;
                                    totalCalories += workout.getCaloriesBurned();
                                    totalTime += workout.getTotalMinutes();
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateStatsUI(monthlySessions, totalCalories, totalTime);

                    } else {
                        Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateStatsUI(int sessions, int calories, int totalTime) {
        tvSessionCount.setText(String.valueOf(sessions));

        // Format calories with comma for thousands
        tvTotalCals.setText(String.format("%,d", calories));

        // Calculate average time
        int avgTime = sessions > 0 ? (totalTime / sessions) : 0;
        tvAvgTime.setText(String.valueOf(avgTime));
    }
}