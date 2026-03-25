package com.mastersoft.fitpulse.model;

import com.google.firebase.firestore.PropertyName;

/**
 * Mirrors a single document in the Firestore "workoutPlan" collection.
 *
 * Document schema:
 *   name : String          – category display name (e.g. "Upper Body")
 *   w1   : String          – exercise 1 descriptor (e.g. "Pull Ups|4 × 10 · Rest: 90s")
 *   w2   : String          – exercise 2 descriptor
 *   w3   : String
 *   w4   : String
 *   w5   : String
 *   w6   : String
 *
 * Each wN string uses the pipe character '|' as a separator:
 *   <exercise name>|<sets × reps · rest info>
 *
 * Example:
 *   name : "Upper Body"
 *   w1   : "Pull Ups|4 × 10 · Rest: 90s"
 *   w2   : "Lat Pull-downs|3 × 12 · Rest: 60s"
 *   …
 */
public class WorkoutPlan {

    // Firestore document ID (not a field, set manually after fetch)
    private String documentId;

    private String name;
    private String w1;
    private String w2;
    private String w3;
    private String w4;
    private String w5;
    private String w6;

    // Required no-arg constructor for Firestore deserialization
    public WorkoutPlan() {}

    public WorkoutPlan(String documentId, String name,
                       String w1, String w2, String w3,
                       String w4, String w5, String w6) {
        this.documentId = documentId;
        this.name = name;
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.w4 = w4;
        this.w5 = w5;
        this.w6 = w6;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getDocumentId() { return documentId; }
    public String getName()       { return name; }
    public String getW1()         { return w1; }
    public String getW2()         { return w2; }
    public String getW3()         { return w3; }
    public String getW4()         { return w4; }
    public String getW5()         { return w5; }
    public String getW6()         { return w6; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setName(String name)             { this.name = name; }
    public void setW1(String w1)                 { this.w1 = w1; }
    public void setW2(String w2)                 { this.w2 = w2; }
    public void setW3(String w3)                 { this.w3 = w3; }
    public void setW4(String w4)                 { this.w4 = w4; }
    public void setW5(String w5)                 { this.w5 = w5; }
    public void setW6(String w6)                 { this.w6 = w6; }

    /**
     * Convenience: returns a 6-element array of the raw wN strings.
     * Index 0 = w1, index 5 = w6.
     */
    public String[] getExercises() {
        return new String[]{ w1, w2, w3, w4, w5, w6 };
    }

    /**
     * Parses a wN field into [exerciseName, detailText].
     * Returns ["—", "—"] if the field is null or malformed.
     */
    public static String[] parseExercise(String raw) {
        if (raw == null || raw.isEmpty()) return new String[]{"—", "—"};
        String[] parts = raw.split("\\|", 2);
        String exerciseName = parts[0].trim();
        String detail       = parts.length > 1 ? parts[1].trim() : "";
        return new String[]{ exerciseName, detail };
    }
}