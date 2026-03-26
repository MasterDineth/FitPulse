package com.mastersoft.fitpulse.model;
/**
 * Represents one of the three FitPulse membership plans.
 */
public class MembershipPlan {

    public static final String PLAN_BASIC = "Basic";
    public static final String PLAN_PRO   = "Pro";
    public static final String PLAN_ELITE = "Elite";

    // Plan definitions ── amounts in LKR
    public static final MembershipPlan BASIC = new MembershipPlan(
            PLAN_BASIC, 3000, "Gym Access · App · Tutorials");

    public static final MembershipPlan PRO = new MembershipPlan(
            PLAN_PRO,   5000, "All Basic · QR Check-In · Stats");

    public static final MembershipPlan ELITE = new MembershipPlan(
            PLAN_ELITE, 8000, "All Pro · Personal Trainer · Diet Plan");

    private final String name;
    private final double amount;
    private final String features;

    private MembershipPlan(String name, double amount, String features) {
        this.name     = name;
        this.amount   = amount;
        this.features = features;
    }

    public String getName()     { return name; }
    public double getAmount()   { return amount; }
    public String getFeatures() { return features; }

    /** Returns the formatted amount string, e.g. "Rs 5,000". */
    public String getFormattedAmount() {
        return String.format("Rs %,.0f", amount);
    }

    /** Finds a plan by name (case-insensitive). Returns PRO as default. */
    public static MembershipPlan fromName(String name) {
        if (PLAN_BASIC.equalsIgnoreCase(name)) return BASIC;
        if (PLAN_ELITE.equalsIgnoreCase(name)) return ELITE;
        return PRO;
    }
}