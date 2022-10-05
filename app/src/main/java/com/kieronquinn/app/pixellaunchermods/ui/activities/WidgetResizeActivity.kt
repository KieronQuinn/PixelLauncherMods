package com.kieronquinn.app.pixellaunchermods.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import com.kieronquinn.app.pixellaunchermods.R

class WidgetResizeActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_widget_resize)
    }

}