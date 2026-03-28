package com.mastersoft.fitpulse.model;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkoutSession {


    private String name;
    private Timestamp date;
    private long durationMin;
    private String type;


    private String documentId;

    public WorkoutSession() {
    }

    public WorkoutSession(String name, Timestamp date, long durationMin, String type) {
        this.name = name;
        this.date = date;
        this.durationMin = durationMin;
        this.type = type;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getName() {
        return name != null ? name : "Workout";
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public long getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(long min) {
        this.durationMin = min;
    }

    public String getType() {
        return type != null ? type : "";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String id) {
        this.documentId = id;
    }


    public String getFormattedDate() {
        if (date == null) return "—";
        Date d = date.toDate();
        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(d);
    }


    public String getIconDrawableName() {
        if (type == null) return "ic_fitness_upper";
        switch (type.toLowerCase(Locale.ROOT)) {
            case "lower":
                return "ic_fitness_lower";
            case "cardio":
                return "ic_fitness_cardio";
            case "fullbody":
                return "ic_fitness_full";
            default:
                return "ic_fitness_upper";
        }
    }


    public String getIconBackgroundDrawableName() {
        if (type == null) return "bg_workout_icon_blue";
        switch (type.toLowerCase(Locale.ROOT)) {
            case "lower":
                return "bg_workout_icon_purple";
            case "cardio":
                return "bg_workout_icon_orange";
            case "fullbody":
                return "bg_workout_icon_green";
            default:
                return "bg_workout_icon_pink";
        }
    }
}