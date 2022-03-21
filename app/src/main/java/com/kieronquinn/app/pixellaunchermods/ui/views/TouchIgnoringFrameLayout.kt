package com.kieronquinn.app.pixellaunchermods.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 *  A [FrameLayout] that will block touch events to its children, and instead send them on to its
 *  parent, if one is attached. Used to block widget preview layout touches.
 */
class TouchIgnoringFrameLayout: FrameLayout {

    constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int):
            super(context, attributeSet, defStyleRes)
    constructor(context: Context, attributeSet: AttributeSet?):
            this(context, attributeSet, 0)
    constructor(context: Context):
            this(context, null, 0)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        (parent as? View)?.let {
            it.dispatchTouchEvent(ev)
            return true
        }
        return false
    }

}