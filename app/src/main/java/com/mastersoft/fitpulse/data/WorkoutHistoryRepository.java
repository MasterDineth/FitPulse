package com.mastersoft.fitpulse.data;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class WorkoutHistoryRepository {

    private static final String TAG = "WorkoutHistoryRepo";
    private static final String COLLECTION = "workoutHistory";

    private static final double DEFAULT_MET = 5.0;

    private static final double DEFAULT_WEIGHT_KG = 70.0;

    private static WorkoutHistoryRepository sInstance;

    private final FirebaseFirestore db;

    public static synchronized WorkoutHistoryRepository getInstance() {
        if (sInstance == null) sInstance = new WorkoutHistoryRepository();
        return sInstance;
    }

    private WorkoutHistoryRepository() {
        db = FirebaseFirestore.getInstance();
    }


    public interface SaveCallback {
        void onSuccess(String documentId);

        void onFailure(Exception e);
    }

    public void saveSession(String workoutType,
                            long totalSeconds,
                            double weightKg,
                            SaveCallback callback) {

        double effectiveWeight = weightKg > 0 ? weightKg : DEFAULT_WEIGHT_KG;
        double minutes = totalSeconds / 60.0;
        int calories = estimateCalories(DEFAULT_MET, effectiveWeight, minutes);

        // Get current user UID (may be empty for guest sessions)
        String userId = "";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) userId = user.getUid();

        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("workoutType", workoutType);
        session.put("totalSeconds", totalSeconds);
        session.put("totalMinutes", Math.round(minutes * 10.0) / 10.0);
        session.put("caloriesBurned", calories);
        session.put("date", Timestamp.now());

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

    //Calorie calculation

    private int estimateCalories(double met, double weightKg, double durationMinutes) {
        double hours = durationMinutes / 60.0;
        return (int) Math.round(met * weightKg * hours);
    }
}