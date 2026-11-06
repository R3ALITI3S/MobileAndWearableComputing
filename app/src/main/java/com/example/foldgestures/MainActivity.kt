
package com.example.foldgestures

import android.os.Bundle
import android.graphics.Rect
import android.view.View
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.FoldingFeature

import com.example.foldgestures.databinding.ActivityMainBinding

import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var windowInfoTracker: WindowInfoTracker
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var hingeAngleSensor: Sensor? = null

    private var hingeSensorListener: SensorEventListener? = null

    private var currentHingeAngle: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowInfoTracker = WindowInfoTracker.getOrCreate(this)

        // Get the hinge method
        initializeHingeSensor()
    }

    // THE HINGE SENSOR
    private fun initializeHingeSensor() {

        sensorManager = getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

        hingeAngleSensor = sensorManager.getDefaultSensor(
            Sensor.TYPE_HINGE_ANGLE
        )

        if (hingeAngleSensor != null) {

            binding.hingeAngle.text =
                "Hinge Angle: Waiting for sensor..."

            hingeSensorListener = object : SensorEventListener {

                override fun onSensorChanged(event: SensorEvent) {

                    if (event.values.isNotEmpty()) {

                        val angle = event.values[0]

                        currentHingeAngle = angle

                        runOnUiThread {

                            binding.hingeAngle.text =
                                String.format(
                                    Locale.getDefault(),
                                    "Hinge Angle: %.2f°",
                                    angle
                                )
                        }
                    }
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int
                ) {
                    binding.hingeAngle.text = "Data changed!"
                }
            }

        } else {

            binding.hingeAngle.text =
                "Hinge angle sensor is not suppotrted"
        }
    }

    // Register hinge sensor
    override fun onResume() {
        super.onResume()

        val sensor = hingeAngleSensor
        val listener = hingeSensorListener

        if (sensor != null && listener != null) {

            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    // Unregister hinge sensor
    override fun onPause() {
        super.onPause()

        hingeSensorListener?.let {
            if (::sensorManager.isInitialized) {
                sensorManager.unregisterListener(it)
            }
        }
    }
}