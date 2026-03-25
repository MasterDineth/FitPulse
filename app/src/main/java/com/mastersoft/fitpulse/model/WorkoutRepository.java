package com.mastersoft.fitpulse.model;


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

/**
 * Single source of truth for WorkoutPlan data.
 *
 * Strategy  — stale-while-revalidate:
 *  1. If cache exists AND is fresh (< CACHE_TTL_HOURS old) → return cache immediately,
 *     skip network entirely.
 *  2. If cache exists but is stale → return cache immediately (so the UI is never empty),
 *     THEN trigger a background fetch to refresh the cache silently.
 *  3. If no cache → show loading, fetch from Firestore, persist to cache, return data.
 *
 * This keeps network usage minimal while the UI always has something to show.
 */
public class WorkoutRepository {

    private static final String TAG             = "WorkoutRepository";
    private static final String PREFS_NAME      = "fitpulse_workout_cache";
    private static final String KEY_PLANS_JSON  = "workout_plans_json";
    private static final String KEY_LAST_FETCH  = "last_fetch_timestamp";
    private static final long   CACHE_TTL_HOURS = 24;   // refresh at most once a day

    private static WorkoutRepository sInstance;

    private final FirebaseFirestore db;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static synchronized WorkoutRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WorkoutRepository(context.getApplicationContext());
        }
        return sInstance;
    }

    private WorkoutRepository(Context context) {
        db    = FirebaseFirestore.getInstance();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Callback interface used to deliver results back to the Fragment.
     */
    public interface WorkoutPlansCallback {
        /** Called with cached data (possibly stale) when available immediately. */
        void onCacheLoaded(List<WorkoutPlan> plans);
        /** Called after a successful network refresh. */
        void onNetworkRefreshed(List<WorkoutPlan> plans);
        /** Called when a network error occurs and no cache is available. */
        void onError(Exception e);
    }

    /**
     * Main entry point.  Follows the stale-while-revalidate pattern described above.
     *
     * @param forceRefresh if true, always hit the network even if cache is fresh.
     */
    public void getWorkoutPlans(boolean forceRefresh, WorkoutPlansCallback callback) {
        List<WorkoutPlan> cached = loadFromCache();

        if (!forceRefresh && cached != null && isCacheFresh()) {
            // Cache is fresh — return immediately, skip network
            Log.d(TAG, "Cache is fresh — returning " + cached.size() + " plans without network call");
            callback.onCacheLoaded(cached);
            return;
        }

        if (cached != null) {
            // Stale cache — deliver it right away so UI is not empty
            Log.d(TAG, "Cache is stale — delivering cached data then refreshing");
            callback.onCacheLoaded(cached);
        }

        // Hit the network (either forced, stale, or no cache)
        fetchFromFirestore(callback, cached == null /* showLoadingState */);
    }

    /** Convenience overload: respects cache TTL, does not force-refresh. */
    public void getWorkoutPlans(WorkoutPlansCallback callback) {
        getWorkoutPlans(false, callback);
    }

    // ── Firebase fetch ────────────────────────────────────────────────────────

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

    // ── Cache helpers ─────────────────────────────────────────────────────────

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
        Type type = new TypeToken<List<WorkoutPlan>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private boolean isCacheFresh() {
        long lastFetch = prefs.getLong(KEY_LAST_FETCH, 0L);
        long ageMs     = System.currentTimeMillis() - lastFetch;
        return ageMs < TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS);
    }

    /** Call this to force the cache to be treated as stale on the next request. */
    public void invalidateCache() {
        prefs.edit().remove(KEY_LAST_FETCH).apply();
        Log.d(TAG, "Cache invalidated");
    }

    /** Completely clears all cached workout plan data. */
    public void clearCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Cache cleared");
    }
}