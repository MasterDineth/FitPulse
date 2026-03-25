package com.mastersoft.fitpulse.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.model.WorkoutSession;

import java.util.List;

/**
 * RecyclerView adapter for the Workout History list on HomeFragment.
 *
 * Each row uses {@code R.layout.item_workout_history} and displays:
 *   - Workout icon  (type-based drawable)
 *   - Workout name
 *   - Date          (formatted dd/MM/yy)
 *   - Duration      (e.g. "45 min")
 */
public class WorkoutHistoryAdapter
        extends RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder> {

    // ── Callback ──────────────────────────────────────────────────────────────
    public interface OnItemClickListener {
        void onItemClick(WorkoutSession session);
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final List<WorkoutSession> sessions;
    private final OnItemClickListener  listener;

    // ── Constructor ───────────────────────────────────────────────────────────
    public WorkoutHistoryAdapter(List<WorkoutSession> sessions,
                                 OnItemClickListener  listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    // ── RecyclerView.Adapter ──────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(sessions.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return sessions == null ? 0 : sessions.size();
    }

    // ── Update helper ─────────────────────────────────────────────────────────

    /** Replaces the data set and triggers a full rebind. */
    public void submitList(List<WorkoutSession> newSessions) {
        sessions.clear();
        sessions.addAll(newSessions);
        notifyDataSetChanged();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView ivIcon;
        private final TextView           tvName;
        private final TextView           tvDate;
        private final TextView           tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon     = itemView.findViewById(R.id.ivWorkoutIcon);
            tvName     = itemView.findViewById(R.id.tvWorkoutName);
            tvDate     = itemView.findViewById(R.id.tvWorkoutDate);
            tvDuration = itemView.findViewById(R.id.tvWorkoutDuration);
        }

        void bind(WorkoutSession session, OnItemClickListener listener) {
            // Text fields
            if (tvName     != null) tvName.setText(session.getName());
            if (tvDate     != null) tvDate.setText(session.getFormattedDate());
            if (tvDuration != null) tvDuration.setText(session.getDurationMin() + " min");

            // Dynamic icon + background based on workout type
            Context ctx       = itemView.getContext();
            Resources res     = ctx.getResources();
            String    pkg     = ctx.getPackageName();

            if (ivIcon != null) {
                int iconResId = res.getIdentifier(
                        session.getIconDrawableName(), "drawable", pkg);
                if (iconResId != 0) ivIcon.setImageResource(iconResId);

                int bgResId = res.getIdentifier(
                        session.getIconBackgroundDrawableName(), "drawable", pkg);
                if (bgResId != 0) ivIcon.setBackgroundResource(bgResId);
            }

            // Row click
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onItemClick(session));
            }
        }
    }
}