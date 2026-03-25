package com.mastersoft.fitpulse.model;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepCounterHelper implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private int currentStepCount = 0;

    public StepCounterHelper(Context context) {
        // Initialize the Sensor Manager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // Get the default step counter sensor
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
    }

    // Call this method (e.g., in onResume) to start listening for steps
    public void startListening() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e("StepCounterHelper", "Pedometer sensor not available on this device!");
        }
    }

    // Call this method (e.g., in onPause) to stop listening and save battery
    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // The sensor returns a float. We cast it to an int.
            currentStepCount = (int) event.values[0];
            Log.d("StepCounterHelper", "Total steps since reboot: " + currentStepCount);

            // You can trigger a callback interface here to update your UI
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // This is required by the interface but typically not needed for the step counter
    }

    // Method to output the number of steps as an int
    public int getStepCount() {
        return currentStepCount;
    }



}