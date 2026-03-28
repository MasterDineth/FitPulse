package com.mastersoft.fitpulse.model;

import java.util.Date;

public class Workout {

    private int caloriesBurned;
    private Date date;
    private double totalMinutes;
    private int totalSeconds;
    private String userId;
    private String workoutType;
    private String workoutName;

    public Workout() {

    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public Date getDate() {
        return date;
    }

    public double getTotalMinutes() {
        return totalMinutes;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public String getUserId() {
        return userId;
    }

    public String getWorkoutType() {
        return workoutType;
    }

    public String getWorkoutName() {
        return workoutName;
    }
}