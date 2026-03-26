package com.mastersoft.fitpulse.fragment;
import static com.google.android.material.internal.ViewUtils.dpToPx;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.data.PaymentRepository;
import com.mastersoft.fitpulse.model.MembershipPlan;
import com.mastersoft.fitpulse.model.PaymentRecord;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.UUID;

// PayHere SDK imports — add to build.gradle (see payhere_dependencies.txt)
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

import android.content.Intent;

public class PaymentsFragment extends Fragment {

    // ── PayHere sandbox credentials ── REPLACE with live keys in production ──
    private static final String PAYHERE_MERCHANT_ID     = "1226859";
    private static final String PAYHERE_MERCHANT_SECRET = "MTA1NzIyMzA5ODUxNDA3ODcyMDI3MzUwNTI5OTIxMzUzODI5Mzg1";
    // Set to PHConfigs.LIVE_URL for production
    private static final String PAYHERE_ENDPOINT        = PHConfigs.SANDBOX_URL;

    private static final int PAYHERE_REQUEST_CODE = 2001;

    // ── Plan definitions ──────────────────────────────────────────────────────
    private static final MembershipPlan[] PLANS = {
            MembershipPlan.BASIC,
            MembershipPlan.PRO,
            MembershipPlan.ELITE
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private MembershipPlan selectedPlan = MembershipPlan.PRO; // default
    private String         pendingOrderId = null;

    // ── View refs ─────────────────────────────────────────────────────────────
    private TextView     tvStatus, tvRenewal, tvPlanName, tvCurrentPrice;
    private ImageView    ivStatusIcon;
    private View         cardProcessing;
    private TextView     tvProcessingMessage;
    private View         btnMakePayment;

    private MaterialCardView cardBasic, cardPro, cardElite;
    private ImageView        ivSelBasic, ivSelPro, ivSelElite;

    private LinearLayout              layoutHistory;
    private TextView                  tvHistoryEmpty;
    private CircularProgressIndicator historyLoader;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupPlanCards();
        selectPlan(MembershipPlan.PRO); // highlight Pro by default
        loadMembershipStatus();
        loadPaymentHistory();

        btnMakePayment.setOnClickListener(v -> confirmAndPay());
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View v) {
        tvStatus           = v.findViewById(R.id.tvMembershipStatus);
        tvRenewal          = v.findViewById(R.id.tvRenewalDate);
        tvPlanName         = v.findViewById(R.id.tvCurrentPlanName);
        tvCurrentPrice     = v.findViewById(R.id.tvCurrentPrice);
        ivStatusIcon       = v.findViewById(R.id.ivStatusIcon);
        cardProcessing     = v.findViewById(R.id.cardProcessing);
        tvProcessingMessage = v.findViewById(R.id.tvProcessingMessage);
        btnMakePayment     = v.findViewById(R.id.btnMakePayment);

        cardBasic  = v.findViewById(R.id.cardPlanBasic);
        cardPro    = v.findViewById(R.id.cardPlanPro);
        cardElite  = v.findViewById(R.id.cardPlanElite);
        ivSelBasic = v.findViewById(R.id.ivSelectBasic);
        ivSelPro   = v.findViewById(R.id.ivSelectPro);
        ivSelElite = v.findViewById(R.id.ivSelectElite);

        layoutHistory  = v.findViewById(R.id.layoutPaymentHistory);
        tvHistoryEmpty = v.findViewById(R.id.tvHistoryEmpty);
        historyLoader  = v.findViewById(R.id.historyLoadingIndicator);
    }

    // ── Plan card selection ───────────────────────────────────────────────────

    private void setupPlanCards() {
        cardBasic.setOnClickListener(v -> selectPlan(MembershipPlan.BASIC));
        cardPro.setOnClickListener(v   -> selectPlan(MembershipPlan.PRO));
        cardElite.setOnClickListener(v -> selectPlan(MembershipPlan.ELITE));
    }

    /**
     * Visually selects the given plan and updates the status card preview.
     */
    private void selectPlan(MembershipPlan plan) {
        selectedPlan = plan;

        // Reset all cards to unselected state
        applyCardUnselected(cardBasic,  ivSelBasic);
        applyCardUnselected(cardPro,    ivSelPro);
        applyCardUnselected(cardElite,  ivSelElite);

        // Highlight the chosen card
        switch (plan.getName()) {
            case MembershipPlan.PLAN_BASIC:
                applyCardSelected(cardBasic, ivSelBasic);
                break;
            case MembershipPlan.PLAN_ELITE:
                applyCardSelected(cardElite, ivSelElite);
                break;
            default: // PRO
                applyCardSelected(cardPro, ivSelPro);
                break;
        }

        // Update the status card preview with the selected plan's price
        tvCurrentPrice.setText(plan.getFormattedAmount());
    }

    private void applyCardSelected(MaterialCardView card, ImageView radioIcon) {
        if (getContext() == null) return;
        card.setStrokeColor(
                ContextCompat.getColor(requireContext(),
                        com.google.android.material.R.color.material_dynamic_primary40));
//        card.setStrokeWidth(dpToPx(2));
        card.setCardBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        com.google.android.material.R.color.material_dynamic_primary90)));
        radioIcon.setImageResource(R.drawable.ic_radio_checked);
        radioIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        com.google.android.material.R.color.material_dynamic_primary40)));
    }

    private void applyCardUnselected(MaterialCardView card, ImageView radioIcon) {
        if (getContext() == null) return;
        card.setStrokeColor(android.graphics.Color.TRANSPARENT);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(ColorStateList.valueOf(
                requireContext().obtainStyledAttributes(
                                new int[]{com.google.android.material.R.attr.colorSurfaceContainerLow})
                        .getColor(0, 0xFFF5F5F5)));
        radioIcon.setImageResource(R.drawable.ic_radio_unchecked);
        radioIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        com.google.android.material.R.color.material_dynamic_neutral_variant60)));
    }

    // ── Membership status ─────────────────────────────────────────────────────

    private void loadMembershipStatus() {
        tvStatus.setText("Loading…");
        tvRenewal.setText("");
        ivStatusIcon.setVisibility(View.GONE);

        PaymentRepository.getInstance().getMembershipStatus(
                new PaymentRepository.MembershipStatusCallback() {
                    @Override
                    public void onLoaded(String status, String planName,
                                         double amount, Timestamp renewalDate) {
                        if (getContext() == null) return;
                        updateStatusCard(status, planName, amount, renewalDate);
                        // Also pre-select the current plan in the plan cards
                        if (!planName.equals("—")) {
                            selectPlan(MembershipPlan.fromName(planName));
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getContext() == null) return;
                        tvStatus.setText("Inactive");
                        tvRenewal.setText("Could not load status");
                    }
                });
    }

    private void updateStatusCard(String status, String planName,
                                  double amount, Timestamp renewalDate) {
        boolean isActive = "Active".equalsIgnoreCase(status);

        tvStatus.setText(status);
        tvPlanName.setText(planName.equals("—") ? "" : planName + " Plan");
        tvCurrentPrice.setText(amount > 0
                ? String.format("Rs %,.0f", amount)
                : "Rs —");

        if (renewalDate != null) {
            tvRenewal.setText("Renews: " + PaymentRepository.formatDate(renewalDate));
        } else {
            tvRenewal.setText(isActive ? "" : "No active subscription");
        }

        ivStatusIcon.setVisibility(isActive ? View.VISIBLE : View.GONE);

        // Tint the status card green for active, amber for inactive
        if (getContext() != null) {
            int cardColor = isActive
                    ? ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.material_dynamic_primary40)
                    : ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.design_default_color_error);

            View statusCard = requireView().findViewById(R.id.cardMembershipStatus);
            if (statusCard instanceof MaterialCardView) {
                ((MaterialCardView) statusCard).setCardBackgroundColor(
                        ColorStateList.valueOf(cardColor));
            }
        }
    }

    // ── Payment history ───────────────────────────────────────────────────────

    private void loadPaymentHistory() {
        historyLoader.setVisibility(View.VISIBLE);
        tvHistoryEmpty.setVisibility(View.GONE);
        layoutHistory.removeAllViews();

        PaymentRepository.getInstance().getPaymentHistory(
                new PaymentRepository.PaymentHistoryCallback() {
                    @Override
                    public void onLoaded(List<PaymentRecord> records) {
                        historyLoader.setVisibility(View.GONE);
                        if (records.isEmpty()) {
                            tvHistoryEmpty.setVisibility(View.VISIBLE);
                        } else {
                            for (PaymentRecord record : records) {
                                addHistoryRow(record);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        historyLoader.setVisibility(View.GONE);
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                        tvHistoryEmpty.setText("Failed to load history");
                    }
                });
    }

    /**
     * Inflates a history row card and appends it to the history list.
     */
    private void addHistoryRow(PaymentRecord record) {
        if (getContext() == null) return;

        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_payment_history, layoutHistory, false);

        ((TextView) row.findViewById(R.id.tvPaymentMonth))
                .setText(PaymentRepository.formatMonthYear(record.getPaymentDate()));
        ((TextView) row.findViewById(R.id.tvPaymentPlan))
                .setText(record.getPlanName() + " Membership");
        ((TextView) row.findViewById(R.id.tvPaymentAmount))
                .setText(record.getFormattedAmount());

        Chip chipStatus = row.findViewById(R.id.chipPaymentStatus);
        chipStatus.setText(record.isSuccess() ? "Paid" : record.getStatus());
        if (record.isSuccess()) {
            chipStatus.setChipBackgroundColorResource(
                    com.google.android.material.R.color.material_dynamic_tertiary90);
            chipStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.material_dynamic_tertiary10));
        } else {
            chipStatus.setChipBackgroundColorResource(
                    com.google.android.material.R.color.m3_ref_palette_error90);
            chipStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    com.google.android.material.R.color.design_default_color_error));
        }

        layoutHistory.addView(row);
    }

    // ── Payment flow ──────────────────────────────────────────────────────────

    /**
     * Shows a confirmation dialog before launching PayHere.
     */
    private void confirmAndPay() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Payment")
                .setMessage("Subscribe to " + selectedPlan.getName()
                        + " plan for " + selectedPlan.getFormattedAmount() + "/month?")
                .setPositiveButton("Pay Now", (d, w) -> launchPayhere())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Builds the PayHere InitRequest and starts the PayHere SDK Activity.
     *
     * PayHere SDK reference: https://github.com/PayHere/payhere-android-sdk
     */


    private void launchPayhere() {
        pendingOrderId = "FP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email   = user != null && user.getEmail() != null ? user.getEmail() : "guest@fitpulse.lk";
        String phone   = "0771234567"; // TODO: load from user profile
        String firstName = "FitPulse";
        String lastName  = "Member";

        // Build PayHere request
//        InitRequest request = new InitRequest();
//        request.merchantId     = PAYHERE_MERCHANT_ID;
//        request.notifyUrl      = "https://your-backend.com/payhere-notify"; // TODO: set your backend URL
//        request.orderId        = pendingOrderId;
//        request.itemsDescription  = selectedPlan.getName() + " Membership – 1 Month";
//        request.currency       = "LKR";
//        request.amount         = selectedPlan.getAmount();
//        request.firstName      = firstName;
//        request.lastName       = lastName;
//        request.email          = email;
//        request.phone          = phone;
//        request.address        = "Colombo";
//        request.city           = "Colombo";
//        request.country        = "Sri Lanka";

//        InitRequest req = new InitRequest();
//        req.setMerchantId("1210XXX");       // Merchant ID
//        req.setCurrency("LKR");             // Currency code LKR/USD/GBP/EUR/AUD
//        req.setAmount(1000.00);             // Final Amount to be charged
//        req.setOrderId("230000123");        // Unique Reference ID
//        req.setItemsDescription("Door bell wireless");  // Item description title
//        req.setCustom1("This is the custom message 1");
//        req.setCustom2("This is the custom message 2");
//        req.getCustomer().setFirstName("Saman");
//        req.getCustomer().setLastName("Perera");
//        req.getCustomer().setEmail("samanp@gmail.com");
//        req.getCustomer().setPhone("+94771234567");
//        req.getCustomer().getAddress().setAddress("No.1, Galle Road");
//        req.getCustomer().getAddress().setCity("Colombo");
//        req.getCustomer().getAddress().setCountry("Sri Lanka");
//
//        Intent intent = new Intent(PaymentsFragment.this, PHMainActivity.class);
//        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
//        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
//        startActivityForResult(intent, PAYHERE_REQUEST);
//
//
//        // Add one item
//        Item item = new Item(
//                pendingOrderId,
//                selectedPlan.getName() + " Membership",
//                1,
//                selectedPlan.getAmount());
//        request.addItem(item);
//
//        // Generate hash using your merchant secret
//        // IMPORTANT: In production, generate this hash on your server to keep the secret safe.
//        // This is shown here for completeness; do NOT embed merchant secret in production APKs.
//        String hash = generateHash(request);
//        request.hash = hash;
//
//        // Launch PayHere
//        Intent intent = new Intent(requireContext(), PHMainActivity.class);
//        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, request);
//        PHConfigs.setBaseUrl(PAYHERE_ENDPOINT);
//        startActivityForResult(intent, PAYHERE_REQUEST_CODE);
//
//        setProcessingState(true, "Opening payment gateway…");
    }

    /**
     * Generates the MD5 hash required by PayHere.
     *
     * Formula: MD5( merchant_id + order_id + amount_formatted + currency +
     *               MD5(merchant_secret).toUpperCase() ).toUpperCase()
     *
     * SECURITY NOTE: In a production app, generate this hash server-side and
     * return it to the app. Never embed merchant_secret in the APK.
     */
//    private String generateHash(InitRequest request) {
//        try {
//            String secretHash = md5(PAYHERE_MERCHANT_SECRET).toUpperCase();
//            String amountFormatted = String.format(java.util.Locale.US, "%.2f", request.amount);
//            String rawString = PAYHERE_MERCHANT_ID
//                    + request.orderId
//                    + amountFormatted
//                    + request.currency
//                    + secretHash;
//            return md5(rawString).toUpperCase();
//        } catch (Exception e) {
//            return "";
//        }
//    }

    private String md5(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── PayHere callback ──────────────────────────────────────────────────────

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode != PAYHERE_REQUEST_CODE || data == null) return;
//
//        setProcessingState(false, "");
//
//        PHResponse<StatusResponse> response =
//                (PHResponse<StatusResponse>) data.getSerializableExtra(
//                        PHConstants.INTENT_EXTRA_RESULT);
//
//        if (response == null) {
//            showPaymentResult(false, "Payment cancelled", null);
//            return;
//        }

//        StatusResponse sr         = response.getData();
//        String paymentId          = sr != null ? sr.getPaymentId()  : "";
//        String paymentMethod      = sr != null ? sr.getPaymentMethod() : "Card";
//        String returnedStatus     = sr != null ? sr.getStatusMessage() : "";
//        boolean success           = response.isSuccess();

//        String firestoreStatus = success ? "SUCCESS" : "FAILED";
//
//        // Save to Firebase
//        setProcessingState(true, success ? "Saving payment…" : "Recording failed payment…");
//
//        PaymentRepository.getInstance()
//                .savePayment(
//                        pendingOrderId,
//                        paymentId,
//                        selectedPlan,
//                        paymentMethod,
//                        firestoreStatus,
//                        new PaymentRepository.SaveCallback() {
//                            @Override
//                            public void onSuccess() {
//                                if (getContext() == null) return;
//                                setProcessingState(false, "");
//                                showPaymentResult(success, returnedStatus, paymentId);
//                                // Refresh the UI
//                                loadMembershipStatus();
//                                loadPaymentHistory();
//                            }
//
//                            @Override
//                            public void onFailure(Exception e) {
//                                if (getContext() == null) return;
//                                setProcessingState(false, "");
//                                Snackbar.make(requireView(),
//                                        "Payment recorded but sync failed. Retry later.",
//                                        Snackbar.LENGTH_LONG).show();
//                                loadPaymentHistory();
//                            }
//                        });
//    }
//
//    private void showPaymentResult(boolean success, String message, String paymentId) {
//        String title   = success ? "Payment Successful 🎉" : "Payment Failed";
//        String detail  = success
//                ? "Your " + selectedPlan.getName() + " membership is now active.\n"
//                + "Payment ID: " + paymentId
//                : "Payment could not be processed.\n" + message
//                + "\nPlease try again.";
//
//        new MaterialAlertDialogBuilder(requireContext())
//                .setTitle(title)
//                .setMessage(detail)
//                .setPositiveButton("OK", null)
//                .show();
//    }
//
//    // ── UI helpers ────────────────────────────────────────────────────────────
//
//    private void setProcessingState(boolean processing, String message) {
//        cardProcessing.setVisibility(processing ? View.VISIBLE : View.GONE);
//        if (processing && tvProcessingMessage != null) {
//            tvProcessingMessage.setText(message);
//        }
//        btnMakePayment.setEnabled(!processing);
//        cardBasic.setClickable(!processing);
//        cardPro.setClickable(!processing);
//        cardElite.setClickable(!processing);
//    }
//
//    private int dpToPx(int dp) {
//        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
//    }
}