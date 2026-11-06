
package com.example.foldgestures

import android.os.Bundle
import android.graphics.Rect
import android.view.View
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

class MainActivity : ComponentActivity() {

    private lateinit var windowInfoTracker: WindowInfoTracker
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowInfoTracker = WindowInfoTracker.getOrCreate(this)

        obtainWindowMetrics()
        onWindowLayoutInfoChange()
    }

    // Obtain current and maximum window metrics
    private fun obtainWindowMetrics() {
        val wmc = WindowMetricsCalculator.getOrCreate()

        val currentWM =
            wmc.computeCurrentWindowMetrics(this).bounds.flattenToString()

        val maximumWM =
            wmc.computeMaximumWindowMetrics(this).bounds.flattenToString()

        binding.windowMetrics.text =
            "CurrentWindowMetrics: $currentWM\nMaximumWindowMetrics: $maximumWM"
    }

    // Observe window layout changes
    private fun onWindowLayoutInfoChange() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                windowInfoTracker
                    .windowLayoutInfo(this@MainActivity)
                    .collect { value ->
                        updateUI(value)
                    }
            }
        }
    }

    // Update UI when window layout changes
    private fun updateUI(newLayoutInfo: WindowLayoutInfo) {

        binding.layoutChange.text = newLayoutInfo.toString()

        val foldingFeature =
            newLayoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()

        if (foldingFeature != null) {
            binding.configurationChanged.text =
                "Folding feature detected"

            alignViewToFoldingFeatureBounds(
                newLayoutInfo
            )
        } else {
            binding.configurationChanged.text =
                "One logical/physical display - unspanned"

            val constraintLayout = binding.constraintLayout
            val set = ConstraintSet()

            set.clone(constraintLayout)
            set.setVisibility(R.id.folding_feature, View.GONE)
            set.applyTo(constraintLayout)
        }
    }

    // Align the folding feature indicator
    private fun alignViewToFoldingFeatureBounds(
        newLayoutInfo: WindowLayoutInfo
    ) {
        val constraintLayout = binding.constraintLayout

        val set = ConstraintSet()
        set.clone(constraintLayout)

        val foldingFeature =
            newLayoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull() ?: return

        val rect = getFeatureBoundsInWindow(
            foldingFeature,
            binding.root
        )

        val featureHeight = rect.height().coerceAtLeast(1)
        val featureWidth = rect.width().coerceAtLeast(1)

        // Set folding feature dimensions
        set.constrainHeight(
            R.id.folding_feature,
            featureHeight
        )

        set.constrainWidth(
            R.id.folding_feature,
            featureWidth
        )

        set.connect(
            R.id.folding_feature,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0
        )

        set.connect(
            R.id.folding_feature,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            0
        )

        if (foldingFeature.orientation ==
            FoldingFeature.Orientation.VERTICAL
        ) {
            set.setMargin(
                R.id.folding_feature,
                ConstraintSet.START,
                rect.left
            )

            set.connect(
                R.id.layout_change,
                ConstraintSet.END,
                R.id.folding_feature,
                ConstraintSet.START,
                0
            )

        } else {
            // Horizontal folding feature
            set.setMargin(
                R.id.folding_feature,
                ConstraintSet.TOP,
                rect.top
            )

            set.connect(
                R.id.layout_change,
                ConstraintSet.TOP,
                R.id.folding_feature,
                ConstraintSet.BOTTOM,
                0
            )
        }

        set.setVisibility(
            R.id.folding_feature,
            View.VISIBLE
        )

        set.applyTo(constraintLayout)
    }

    // Translate folding feature bounds into view coordinates
    private fun getFeatureBoundsInWindow(
        feature: FoldingFeature,
        view: View
    ): Rect {

        val location = IntArray(2)
        view.getLocationInWindow(location)

        return Rect(feature.bounds).apply {
            offset(-location[0], -location[1])
        }
    }
}
