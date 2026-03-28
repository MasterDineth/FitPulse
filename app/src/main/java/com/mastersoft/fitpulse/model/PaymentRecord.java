package com.mastersoft.fitpulse.model;

import com.google.firebase.Timestamp;

public class PaymentRecord {

    private String userId;
    private String orderId;
    private String planName;
    private double amount;
    private String currency;
    private Timestamp paymentDate;
    private Timestamp renewalDate;
    private String status;
    private String paymentMethod;
    private String paymentId;

    // Required no-arg constructor for Firestore
    public PaymentRecord() {
    }

    public PaymentRecord(String userId, String orderId, String planName,
                         double amount, String currency,
                         Timestamp paymentDate, Timestamp renewalDate,
                         String status, String paymentMethod, String paymentId) {
        this.userId = userId;
        this.orderId = orderId;
        this.planName = planName;
        this.amount = amount;
        this.currency = currency;
        this.paymentDate = paymentDate;
        this.renewalDate = renewalDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentId = paymentId;
    }


    public String getUserId() {
        return userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPlanName() {
        return planName;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Timestamp getPaymentDate() {
        return paymentDate;
    }

    public Timestamp getRenewalDate() {
        return renewalDate;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setUserId(String v) {
        userId = v;
    }

    public void setOrderId(String v) {
        orderId = v;
    }

    public void setPlanName(String v) {
        planName = v;
    }

    public void setAmount(double v) {
        amount = v;
    }

    public void setCurrency(String v) {
        currency = v;
    }

    public void setPaymentDate(Timestamp v) {
        paymentDate = v;
    }

    public void setRenewalDate(Timestamp v) {
        renewalDate = v;
    }

    public void setStatus(String v) {
        status = v;
    }

    public void setPaymentMethod(String v) {
        paymentMethod = v;
    }

    public void setPaymentId(String v) {
        paymentId = v;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    public String getFormattedAmount() {
        return String.format("Rs %,.0f", amount);
    }
}