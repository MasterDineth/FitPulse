package com.mastersoft.fitpulse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.model.Workout;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<Workout> workoutList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public WorkoutAdapter(List<Workout> workoutList) {
        this.workoutList = workoutList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);

        holder.tvWorkoutName.setText(workout.getWorkoutName() != null ? workout.getWorkoutName() : workout.getWorkoutType());
        if (workout.getDate() != null) {
            holder.tvWorkoutDate.setText(dateFormat.format(workout.getDate()));
        }


        holder.tvWorkoutDuration.setText(" " + (int) workout.getTotalMinutes() + " min");
        holder.tvWorkoutCalories.setText("  " + workout.getCaloriesBurned() + " kcal");

    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView tvWorkoutName, tvWorkoutDate, tvWorkoutDuration, tvWorkoutCalories;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkoutName = itemView.findViewById(R.id.tvWorkoutName);
            tvWorkoutDate = itemView.findViewById(R.id.tvWorkoutDate);
            tvWorkoutDuration = itemView.findViewById(R.id.tvWorkoutDuration);
            tvWorkoutCalories = itemView.findViewById(R.id.tvWorkoutCalories);
        }
    }
}