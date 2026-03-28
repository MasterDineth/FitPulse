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

    public void startListening() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e("StepCounterHelper", "Pedometer sensor not available on this device!");
        }
    }

    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            currentStepCount = (int) event.values[0];
            Log.d("StepCounterHelper", "Total steps since reboot: " + currentStepCount);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public int getStepCount() {
        return currentStepCount;
    }


}