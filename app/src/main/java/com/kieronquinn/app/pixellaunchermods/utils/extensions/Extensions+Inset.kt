package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.kieronquinn.app.pixellaunchermods.R

fun View.applyBottomNavigationInset(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height).toInt()
    updatePadding(bottom = bottomNavHeight + extraPadding.toInt())
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        updatePadding(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
    }
}

fun View.applyBottomNavigationMargin(extraPadding: Float = 0f) {
    val bottomNavHeight = resources.getDimension(R.dimen.bottom_nav_height_margins).toInt()
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + bottomNavHeight + extraPadding.toInt())
        }
    }
}