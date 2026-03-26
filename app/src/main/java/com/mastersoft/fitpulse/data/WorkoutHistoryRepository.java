package com.mastersoft.fitpulse.data;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles writing completed workout sessions to the Firestore "workoutHistory" collection.
 *
 * Document schema written per session:
 *   userId        : String   – UID of the signed-in user (empty if anonymous)
 *   workoutType   : String   – selected ChipGroup label (e.g. "Upper Body")
 *   totalSeconds  : long     – total elapsed seconds (not counting pauses)
 *   totalMinutes  : double   – rounded to 1 decimal place
 *   date          : Timestamp – server timestamp
 *   caloriesBurned: int      – estimated based on MET × weight × time
 */
public class WorkoutHistoryRepository {

    private static final String TAG        = "WorkoutHistoryRepo";
    private static final String COLLECTION = "workoutHistory";

    // Average MET value used when no body weight data is available.
    // MET 5 = moderate-intensity weight training.
    private static final double DEFAULT_MET = 5.0;

    // Default body weight (kg) used for calorie estimation if user profile is unavailable.
    private static final double DEFAULT_WEIGHT_KG = 70.0;

    // Singleton
    private static WorkoutHistoryRepository sInstance;

    private final FirebaseFirestore db;

    public static synchronized WorkoutHistoryRepository getInstance() {
        if (sInstance == null) sInstance = new WorkoutHistoryRepository();
        return sInstance;
    }

    private WorkoutHistoryRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Callback for the save operation. */
    public interface SaveCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    /**
     * Saves a completed workout session to Firestore.
     *
     * @param workoutType   Category name from the selected chip (e.g. "Upper Body")
     * @param totalSeconds  Total active seconds (excluding paused time)
     * @param weightKg      User's body weight for calorie calculation; pass 0 to use default
     * @param callback      Delivers the new document ID or an error
     */
    public void saveSession(String workoutType,
                            long totalSeconds,
                            double weightKg,
                            SaveCallback callback) {

        double effectiveWeight = weightKg > 0 ? weightKg : DEFAULT_WEIGHT_KG;
        double minutes         = totalSeconds / 60.0;
        int calories           = estimateCalories(DEFAULT_MET, effectiveWeight, minutes);

        // Get current user UID (may be empty for guest sessions)
        String userId = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) userId = user.getUid();

        Map<String, Object> session = new HashMap<>();
        session.put("userId",         userId);
        session.put("workoutType",    workoutType);
        session.put("totalSeconds",   totalSeconds);
        session.put("totalMinutes",   Math.round(minutes * 10.0) / 10.0);
        session.put("caloriesBurned", calories);
        session.put("date",           Timestamp.now());

        db.collection(COLLECTION)
                .add(session)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Session saved: " + ref.getId());
                    callback.onSuccess(ref.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save session", e);
                    callback.onFailure(e);
                });
    }

    // ── Calorie estimation ────────────────────────────────────────────────────

    /**
     * Mifflin–St Jeor / MET formula:
     *   Calories = MET × weightKg × durationHours
     */
    private int estimateCalories(double met, double weightKg, double durationMinutes) {
        double hours = durationMinutes / 60.0;
        return (int) Math.round(met * weightKg * hours);
    }
}