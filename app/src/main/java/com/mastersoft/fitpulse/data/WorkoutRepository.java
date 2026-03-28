package com.mastersoft.fitpulse.data;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.mastersoft.fitpulse.model.WorkoutPlan;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class WorkoutRepository {

    private static final String TAG = "WorkoutRepository";
    private static final String PREFS_NAME = "fitpulse_workout_cache";
    private static final String KEY_PLANS_JSON = "workout_plans_json";
    private static final String KEY_LAST_FETCH = "last_fetch_timestamp";
    private static final long CACHE_TTL_HOURS = 24;   // refresh at most once a day

    private static WorkoutRepository sInstance;

    private final FirebaseFirestore db;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();


    public static synchronized WorkoutRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WorkoutRepository(context.getApplicationContext());
        }
        return sInstance;
    }

    private WorkoutRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public interface WorkoutPlansCallback {

        void onCacheLoaded(List<WorkoutPlan> plans);

        void onNetworkRefreshed(List<WorkoutPlan> plans);

        void onError(Exception e);
    }

    public void getWorkoutPlans(boolean forceRefresh, WorkoutPlansCallback callback) {
        List<WorkoutPlan> cached = loadFromCache();

        if (!forceRefresh && cached != null && isCacheFresh()) {
            Log.d(TAG, "Cache is fresh — returning " + cached.size() + " plans without network call");
            callback.onCacheLoaded(cached);
            return;
        }

        if (cached != null) {

            Log.d(TAG, "Cache is stale — delivering cached data then refreshing");
            callback.onCacheLoaded(cached);
        }


        fetchFromFirestore(callback, cached == null /* showLoadingState */);
    }

    public void getWorkoutPlans(WorkoutPlansCallback callback) {
        getWorkoutPlans(false, callback);
    }

    private void fetchFromFirestore(WorkoutPlansCallback callback, boolean isFirstLoad) {
        Log.d(TAG, "Fetching workoutPlan collection from Firestore…");

        db.collection("workoutPlan")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WorkoutPlan> plans = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        WorkoutPlan plan = doc.toObject(WorkoutPlan.class);
                        plan.setDocumentId(doc.getId());
                        plans.add(plan);
                        Log.d(TAG, "Loaded plan: " + plan.getName());
                    }

                    // Persist to cache with fresh timestamp
                    saveToCache(plans);
                    Log.d(TAG, "Cached " + plans.size() + " plans");

                    callback.onNetworkRefreshed(plans);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore fetch failed", e);
                    callback.onError(e);
                });
    }

    private void saveToCache(List<WorkoutPlan> plans) {
        String json = gson.toJson(plans);
        prefs.edit()
                .putString(KEY_PLANS_JSON, json)
                .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                .apply();
    }

    private List<WorkoutPlan> loadFromCache() {
        String json = prefs.getString(KEY_PLANS_JSON, null);
        if (json == null) return null;
        Type type = new TypeToken<List<WorkoutPlan>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    private boolean isCacheFresh() {
        long lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L);
        long ageMs = System.currentTimeMillis() - lastFetch;
        return ageMs < TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS);
    }


    public void invalidateCache() {
        prefs.edit().remove(KEY_LAST_FETCH).apply();
        Log.d(TAG, "Cache invalidated");
    }

    public void clearCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Cache cleared");
    }
}