package com.mastersoft.fitpulse.model;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Firestore-mapped model for a single workout session.
 *
 * Expected document fields:
 *   name        : String    — e.g. "Advanced Upper Body"
 *   date        : Timestamp — Firestore server timestamp
 *   durationMin : long      — workout duration in minutes
 *   type        : String    — "upper" | "lower" | "cardio" | "fullbody"
 */
public class WorkoutSession {

    // ── Firestore field names must match these property names exactly ─────────
    private String    name;
    private Timestamp date;
    private long      durationMin;
    private String    type;

    // Transient — not stored in Firestore
    private String documentId;

    // ── No-arg constructor required by Firestore ──────────────────────────────
    public WorkoutSession() {}

    // ── Full constructor (useful for local testing) ───────────────────────────
    public WorkoutSession(String name, Timestamp date, long durationMin, String type) {
        this.name        = name;
        this.date        = date;
        this.durationMin = durationMin;
        this.type        = type;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getName()                    { return name != null ? name : "Workout"; }
    public void   setName(String name)         { this.name = name; }

    public Timestamp getDate()                 { return date; }
    public void      setDate(Timestamp date)   { this.date = date; }

    public long   getDurationMin()             { return durationMin; }
    public void   setDurationMin(long min)     { this.durationMin = min; }

    public String getType()                    { return type != null ? type : ""; }
    public void   setType(String type)         { this.type = type; }

    public String getDocumentId()              { return documentId; }
    public void   setDocumentId(String id)     { this.documentId = id; }

    // ── Derived helpers ───────────────────────────────────────────────────────

    /**
     * Returns the workout date formatted as "dd/MM/yy".
     * Falls back to an em-dash if the timestamp is null.
     */
    public String getFormattedDate() {
        if (date == null) return "—";
        Date d = date.toDate();
        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(d);
    }

    /**
     * Maps the {@code type} field to a drawable resource name.
     * Used by the adapter to select the correct workout icon.
     *
     * Supported type strings (case-insensitive):
     *   "upper"    → ic_fitness_upper
     *   "lower"    → ic_fitness_lower
     *   "cardio"   → ic_fitness_cardio
     *   "fullbody" → ic_fitness_full   (add this drawable if needed)
     */
    public String getIconDrawableName() {
        if (type == null) return "ic_fitness_upper";
        switch (type.toLowerCase(Locale.ROOT)) {
            case "lower":    return "ic_fitness_lower";
            case "cardio":   return "ic_fitness_cardio";
            case "fullbody": return "ic_fitness_full";
            default:         return "ic_fitness_upper";
        }
    }

    /**
     * Maps the {@code type} field to a background drawable name
     * matching the coloured circle behind each history-row icon.
     */
    public String getIconBackgroundDrawableName() {
        if (type == null) return "bg_workout_icon_blue";
        switch (type.toLowerCase(Locale.ROOT)) {
            case "lower":    return "bg_workout_icon_purple";
            case "cardio":   return "bg_workout_icon_orange";
            case "fullbody": return "bg_workout_icon_green";
            default:         return "bg_workout_icon_pink";
        }
    }
}