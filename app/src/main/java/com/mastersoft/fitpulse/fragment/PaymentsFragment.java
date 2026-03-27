package com.mastersoft.fitpulse.fragment;

import static com.google.common.hash.Hashing.md5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mastersoft.fitpulse.R;
import com.mastersoft.fitpulse.data.PaymentRepository;
import com.mastersoft.fitpulse.model.MembershipPlan;
import com.mastersoft.fitpulse.model.PaymentRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class PaymentsFragment extends Fragment {

    private static final String PAYHERE_MERCHANT_ID = "1226859";
    private static final String PAYHERE_MERCHANT_SECRET = "MTg2MjY1ODUyNjE2MjA1OTk4MzYxMzA0MzU0MzUyOTcwMzk5Nzgy";
    private static final String PAYHERE_ENDPOINT = PHConfigs.SANDBOX_URL;
    private static final int PAYHERE_REQUEST_CODE = 2001;

    private static final String PREFS_PAYMENTS = "PaymentCache";
    private static final String KEY_HISTORY = "history_json";
    private SharedPreferences paymentPrefs;

    private MembershipPlan selectedPlan = MembershipPlan.PRO;
    private String pendingOrderId = null;
    private boolean isMembershipActive = false;

    // Firestore data for PayHere
    private String uFullName, uEmail, uMobile, uAddress, uCity, uCountry,username;

    private TextView tvStatus, tvRenewal, tvPlanName, tvCurrentPrice, tvProcessingMessage, tvHistoryEmpty;
    private ImageView ivStatusIcon, ivSelBasic, ivSelPro, ivSelElite;
    private View cardProcessing;
    private MaterialButton btnMakePayment;
    private MaterialCardView cardBasic, cardPro, cardElite;
    private LinearLayout layoutHistory;
    private CircularProgressIndicator historyLoader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        paymentPrefs = requireContext().getSharedPreferences(PREFS_PAYMENTS, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupPlanCards();
        loadLocalHistory();
        loadMembershipStatus();
        loadPaymentHistory();

        btnMakePayment.setOnClickListener(v -> confirmAndPay());
    }

    private void bindViews(View v) {
        tvStatus = v.findViewById(R.id.tvMembershipStatus);
        tvRenewal = v.findViewById(R.id.tvRenewalDate);
        tvPlanName = v.findViewById(R.id.tvCurrentPlanName);
        tvCurrentPrice = v.findViewById(R.id.tvCurrentPrice);
        ivStatusIcon = v.findViewById(R.id.ivStatusIcon);
        cardProcessing = v.findViewById(R.id.cardProcessing);
        tvProcessingMessage = v.findViewById(R.id.tvProcessingMessage);
        btnMakePayment = v.findViewById(R.id.btnMakePayment);
        cardBasic = v.findViewById(R.id.cardPlanBasic);
        cardPro = v.findViewById(R.id.cardPlanPro);
        cardElite = v.findViewById(R.id.cardPlanElite);
        ivSelBasic = v.findViewById(R.id.ivSelectBasic);
        ivSelPro = v.findViewById(R.id.ivSelectPro);
        ivSelElite = v.findViewById(R.id.ivSelectElite);
        layoutHistory = v.findViewById(R.id.layoutPaymentHistory);
        tvHistoryEmpty = v.findViewById(R.id.tvHistoryEmpty);
        historyLoader = v.findViewById(R.id.historyLoadingIndicator);
    }

    private void setupPlanCards() {
        cardBasic.setOnClickListener(v -> selectPlan(MembershipPlan.BASIC));
        cardPro.setOnClickListener(v -> selectPlan(MembershipPlan.PRO));
        cardElite.setOnClickListener(v -> selectPlan(MembershipPlan.ELITE));
    }



    private void selectPlan(MembershipPlan plan) {
        if (isMembershipActive) return; // Disable plan selection if active
        selectedPlan = plan;
        applyCardState(cardBasic, ivSelBasic, plan == MembershipPlan.BASIC);
        applyCardState(cardPro, ivSelPro, plan == MembershipPlan.PRO);
        applyCardState(cardElite, ivSelElite, plan == MembershipPlan.ELITE);
        tvCurrentPrice.setText(plan.getFormattedAmount());
    }

    private void applyCardState(MaterialCardView card, ImageView icon, boolean selected) {
        int stroke = selected ? com.google.android.material.R.color.material_dynamic_primary40 : android.R.color.transparent;
        int bg = selected ? com.google.android.material.R.color.material_dynamic_primary90 : com.google.android.material.R.attr.colorSurfaceContainerLow;
        int iconRes = selected ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked;


        card.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), stroke)));
        card.setCardBackgroundColor(selected ? ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bg)) : null);
        icon.setImageResource(iconRes);
    }

    private void confirmAndPay() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Subscription")
                .setMessage("Subscribe to " + selectedPlan.getName() + " for " + selectedPlan.getFormattedAmount() + "/month?")
                .setPositiveButton("Pay Now", (d, w) -> launchPayhere())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchPayhere() {
        pendingOrderId = "FP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId(PAYHERE_MERCHANT_ID);
        req.setCurrency("LKR");

//        req.setAmount(5000);
//        req.setOrderId("FP-0001");
//        req.setItemsDescription(" Membership");
//
//        // Map data from Firestore user collection
//        req.getCustomer().setFirstName("Dineth");
//        req.getCustomer().setLastName("FitPulse");
//        req.getCustomer().setEmail("guest@fitpulse.lk");
//        req.getCustomer().setPhone("0771234567");
//        req.getCustomer().getAddress().setAddress("Colombo");
//        req.getCustomer().getAddress().setCity("Colombo");
//        req.getCustomer().getAddress().setCountry("Sri Lanka");
//        Log.d("TAG", "launchPayhere: " + selectedPlan.getName());



        req.setAmount(selectedPlan.getAmount());
        req.setOrderId(pendingOrderId);
        req.setItemsDescription(selectedPlan.getName() + " Membership");

        // Map data from Firestore user collection
        req.getCustomer().setFirstName(uFullName != null ? uFullName : "FitPulse");
        req.getCustomer().setLastName(username != null ? uFullName : "Member");
        req.getCustomer().setEmail(uEmail != null ? uEmail : "guest@fitpulse.lk");
        req.getCustomer().setPhone(uMobile != null ? uMobile : "0771234567");
        req.getCustomer().getAddress().setAddress(uAddress != null ? uAddress : "Colombo");
        req.getCustomer().getAddress().setCity(uCity != null ? uCity : "Colombo");
        req.getCustomer().getAddress().setCountry(uCountry != null ? uCountry : "Sri Lanka");



        Intent intent = new Intent(requireContext(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);



        PHConfigs.setBaseUrl(PAYHERE_ENDPOINT);
        startActivityForResult(intent, PAYHERE_REQUEST_CODE);
        setProcessingState(true, "Opening Gateway...");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PAYHERE_REQUEST_CODE || data == null) return;

        setProcessingState(false, "");
        PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

        if (resultCode == Activity.RESULT_OK && response != null && response.isSuccess()) {
            handleSuccess(String.valueOf(response.getData().getPaymentNo()));
        } else {
            Snackbar.make(requireView(), "Payment Canceled", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleSuccess(String paymentId) {
        setProcessingState(true, "Activating Membership...");
        PaymentRepository.getInstance().savePayment(pendingOrderId, paymentId, selectedPlan, "Card", "SUCCESS", new PaymentRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                setProcessingState(false, "");
                showSuccessDialog(paymentId);
                loadMembershipStatus();
                loadPaymentHistory();
            }
            @Override
            public void onFailure(Exception e) {
                setProcessingState(false, "");
                Snackbar.make(requireView(), "Update Failed. Support notified.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loadMembershipStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Fetch User details for PayHere fields
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        uFullName = doc.getString("fullName");
                        username = doc.getString("username");
                        uEmail = doc.getString("email");
                        uMobile = doc.getString("mobile");
                        uAddress = doc.getString("address");
                        uCity = doc.getString("city");
                        uCountry = doc.getString("country");

                        Log.d("Data from the collection ",username + uFullName+" "+ uEmail + uMobile + uAddress + uCity + uCountry);
                    }
                });

        PaymentRepository.getInstance().getMembershipStatus(new PaymentRepository.MembershipStatusCallback() {
            @Override
            public void onLoaded(String status, String planName, double amount, Timestamp renewalDate) {
                if (getContext() == null) return;
                isMembershipActive = "Active".equalsIgnoreCase(status);
                updateStatusUI(status, planName, amount, renewalDate);
                lockUI(isMembershipActive);
            }
            @Override
            public void onError(Exception e) { tvStatus.setText("Inactive"); }
        });
    }

    private void updateStatusUI(String status, String planName, double amount, Timestamp renewalDate) {
        tvStatus.setText(status);
        tvPlanName.setText(planName + " Plan");
        tvCurrentPrice.setText(String.format("Rs %,.0f", amount));
        if (renewalDate != null) tvRenewal.setText("Next Due: " + PaymentRepository.formatDate(renewalDate));
        ivStatusIcon.setVisibility(isMembershipActive ? View.VISIBLE : View.GONE);
    }

    private void lockUI(boolean active) {
        btnMakePayment.setEnabled(!active);
        btnMakePayment.setAlpha(active ? 0.5f : 1.0f);
        btnMakePayment.setText(active ? "Membership Active" : "Make Payment");
        cardBasic.setAlpha(active ? 0.6f : 1.0f);
        cardPro.setAlpha(active ? 0.6f : 1.0f);
        cardElite.setAlpha(active ? 0.6f : 1.0f);
    }


    private void loadPaymentHistory() {
        historyLoader.setVisibility(View.VISIBLE);
        PaymentRepository.getInstance().getPaymentHistory(new PaymentRepository.PaymentHistoryCallback() {
            @Override
            public void onLoaded(List<PaymentRecord> history) {
                if (getContext() == null) return;
                historyLoader.setVisibility(View.GONE);
                populateHistory(history);
                saveLocalHistory(history);
            }
            @Override
            public void onError(Exception e) {
                historyLoader.setVisibility(View.GONE);
                tvHistoryEmpty.setVisibility(View.VISIBLE);
                tvHistoryEmpty.setText("Failed to load history.");
            }
        });
    }

    private void populateHistory(List<PaymentRecord> history) {
        layoutHistory.removeAllViews();
        if (history == null || history.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvHistoryEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (PaymentRecord record : history) {
            View itemView = inflater.inflate(R.layout.item_payment_history, layoutHistory, false);

            TextView tvDate = itemView.findViewById(R.id.tvPaymentMonth);
            TextView tvPlan = itemView.findViewById(R.id.tvPaymentPlan);
            TextView tvAmount = itemView.findViewById(R.id.tvPaymentAmount);
            Chip statusChip = itemView.findViewById(R.id.chipPaymentStatus);

            tvDate.setText(PaymentRepository.formatDate(record.getPaymentDate()));
            tvPlan.setText(record.getPlanName());
            tvAmount.setText(String.format("Rs %,.0f", record.getAmount()));
            statusChip.setText(record.getStatus());

            layoutHistory.addView(itemView);
        }
    }

    private void saveLocalHistory(List<PaymentRecord> history) {
        String json = new Gson().toJson(history);
        paymentPrefs.edit().putString(KEY_HISTORY, json).apply();
    }

    private void loadLocalHistory() {
        String json = paymentPrefs.getString(KEY_HISTORY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<PaymentRecord>>() {}.getType();
            List<PaymentRecord> history = new Gson().fromJson(json, type);
            populateHistory(history);
        }
    }

    private void setProcessingState(boolean processing, String message) {
        cardProcessing.setVisibility(processing ? View.VISIBLE : View.GONE);
        tvProcessingMessage.setText(message);
        btnMakePayment.setEnabled(!processing);
    }

    private void showSuccessDialog(String paymentId) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Payment Successful")
                .setMessage("Your membership has been activated!\nTransaction ID: " + paymentId)
                .setPositiveButton("Awesome", null)
                .show();
    }
}
