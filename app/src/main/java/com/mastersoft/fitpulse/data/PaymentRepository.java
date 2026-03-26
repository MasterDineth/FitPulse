package com.mastersoft.fitpulse.data;

import android.util.Log;

import com.mastersoft.fitpulse.model.MembershipPlan;
import com.mastersoft.fitpulse.model.PaymentRecord;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for payment data:
 *   - Reads from / writes to Firestore "paymentHistory" collection
 *   - Reads from / writes to Firestore "membershipStatus" collection
 *     (one document per user, keyed by UID)
 */
public class PaymentRepository {

    private static final String TAG                = "PaymentRepository";
    private static final String COL_HISTORY        = "paymentHistory";
    private static final String COL_STATUS         = "membershipStatus";

    private static PaymentRepository sInstance;
    private final FirebaseFirestore db;

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static synchronized PaymentRepository getInstance() {
        if (sInstance == null) sInstance = new PaymentRepository();
        return sInstance;
    }

    private PaymentRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public interface PaymentHistoryCallback {
        void onLoaded(List<PaymentRecord> records);
        void onError(Exception e);
    }

    public interface MembershipStatusCallback {
        void onLoaded(String status, String planName, double amount, Timestamp renewalDate);
        void onError(Exception e);
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ── Current user helper ───────────────────────────────────────────────────

    private String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }

    // ── Read payment history ──────────────────────────────────────────────────

    /**
     * Fetches all payment records for the current user, ordered by most recent first.
     */
    public void getPaymentHistory(PaymentHistoryCallback callback) {
        db.collection(COL_HISTORY)
                .whereEqualTo("userId", currentUid())
                .orderBy("paymentDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PaymentRecord> records = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PaymentRecord record = doc.toObject(PaymentRecord.class);
                        record.setOrderId(doc.getId());
                        records.add(record);
                    }
                    Log.d(TAG, "Loaded " + records.size() + " payment records");
                    callback.onLoaded(records);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load payment history", e);
                    callback.onError(e);
                });
    }

    // ── Read membership status ────────────────────────────────────────────────

    /**
     * Fetches the current membership status document for this user.
     * Document path: membershipStatus/{userId}
     */
    public void getMembershipStatus(MembershipStatusCallback callback) {
        db.collection(COL_STATUS)
                .document(currentUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String    status      = doc.getString("status");
                        String    planName    = doc.getString("planName");
                        double    amount      = doc.contains("amount") ? doc.getDouble("amount") : 0;
                        Timestamp renewalDate = doc.getTimestamp("renewalDate");
                        callback.onLoaded(
                                status != null ? status : "Inactive",
                                planName != null ? planName : "—",
                                amount,
                                renewalDate);
                    } else {
                        // No membership record yet
                        callback.onLoaded("Inactive", "—", 0, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load membership status", e);
                    callback.onError(e);
                });
    }

    // ── Save payment record ───────────────────────────────────────────────────

    /**
     * Persists a completed payment record and updates membership status atomically.
     *
     * @param orderId       PayHere order_id
     * @param paymentId     PayHere payment_id from callback
     * @param plan          The plan that was purchased
     * @param paymentMethod e.g. "VISA", "MASTERCARD", "FRIMI"
     * @param status        "SUCCESS" | "FAILED"
     * @param callback      Result callback
     */
    public void savePayment(String orderId,
                            String paymentId,
                            MembershipPlan plan,
                            String paymentMethod,
                            String status,
                            SaveCallback callback) {

        Timestamp now         = Timestamp.now();
        Timestamp renewalDate = addOneMonth(now);
        String uid            = currentUid();

        // ── 1. Build payment record ───────────────────────────────────────────
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("userId",        uid);
        paymentData.put("orderId",       orderId);
        paymentData.put("paymentId",     paymentId);
        paymentData.put("planName",      plan.getName());
        paymentData.put("amount",        plan.getAmount());
        paymentData.put("currency",      "LKR");
        paymentData.put("paymentDate",   now);
        paymentData.put("renewalDate",   renewalDate);
        paymentData.put("status",        status);
        paymentData.put("paymentMethod", paymentMethod != null ? paymentMethod : "Card");

        // ── 2. Build membership status update ─────────────────────────────────
        Map<String, Object> membershipData = new HashMap<>();
        membershipData.put("userId",      uid);
        membershipData.put("status",      "SUCCESS".equalsIgnoreCase(status) ? "Active" : "Inactive");
        membershipData.put("planName",    plan.getName());
        membershipData.put("amount",      plan.getAmount());
        membershipData.put("renewalDate", renewalDate);
        membershipData.put("lastUpdated", now);

        // ── 3. Write payment record ───────────────────────────────────────────
        db.collection(COL_HISTORY)
                .document(orderId)
                .set(paymentData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Payment record saved: " + orderId);

                    // ── 4. Update membership status only for successful payments ──
                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        db.collection(COL_STATUS)
                                .document(uid)
                                .set(membershipData)
                                .addOnSuccessListener(v2 -> {
                                    Log.d(TAG, "Membership status updated to Active");
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update membership status", e);
                                    // Payment was saved; status update failed — partial success
                                    callback.onFailure(e);
                                });
                    } else {
                        callback.onSuccess(); // record saved even for failed payment
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save payment record", e);
                    callback.onFailure(e);
                });
    }

    // ── Date helper ───────────────────────────────────────────────────────────

    /**
     * Returns a Timestamp that is exactly 30 days after the given Timestamp.
     */
    public static Timestamp addOneMonth(Timestamp base) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base.toDate());
        cal.add(Calendar.DAY_OF_MONTH, 30);
        return new Timestamp(cal.getTime());
    }

    /**
     * Formats a Timestamp as "MMM dd, yyyy" for display (e.g. "Jun 23, 2025").
     */
    public static String formatDate(Timestamp ts) {
        if (ts == null) return "—";
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(ts.toDate());
    }

    /**
     * Formats a Timestamp as "MMM yyyy" for payment history row (e.g. "Jun 2025").
     */
    public static String formatMonthYear(Timestamp ts) {
        if (ts == null) return "—";
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault());
        return sdf.format(ts.toDate());
    }
}
