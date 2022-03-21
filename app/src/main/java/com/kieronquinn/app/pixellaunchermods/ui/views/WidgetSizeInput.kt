package com.kieronquinn.app.pixellaunchermods.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.ViewWidgetSizeInputBinding

class WidgetSizeInput: FrameLayout {

    var onMinusClicked: (() -> Unit)? = null
    var onPlusClicked: (() -> Unit)? = null

    var plusEnabled: Boolean = false
        set(value) {
            binding.widgetSizeInputPlus.isEnabled = value
            binding.widgetSizeInputPlus.alpha = if(value) 1f else 0.5f
            field = value
        }

    var minusEnabled: Boolean = false
        set(value) {
            binding.widgetSizeInputMinus.isEnabled = value
            binding.widgetSizeInputMinus.alpha = if(value) 1f else 0.5f
            field = value
        }

    private val binding by lazy {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        ViewWidgetSizeInputBinding.inflate(layoutInflater, this, false)
    }

    constructor(context: Context): super(context, null)

    constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, androidx.appcompat.R.attr.switchStyle)

    constructor(context: Context, attributeSet: AttributeSet?, styleResId: Int): super(context, attributeSet, styleResId){
        initLayout()
        readAttributes(attributeSet)
        setupClickListeners()
    }

    private fun initLayout() {
        removeAllViews()
        addView(binding.root)
    }

    private fun readAttributes(attributeSet: AttributeSet?) {
        if (attributeSet == null) return
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.WidgetSizeInput)
        val iconSrc = typedArray.getResourceId(R.styleable.WidgetSizeInput_android_src, 0)
        typedArray.recycle()
        if(iconSrc == 0) return
        binding.widgetSizeInputIcon.setImageResource(iconSrc)
    }

    private fun setupClickListeners() {
        binding.widgetSizeInputMinus.setOnClickListener {
            onMinusClicked?.invoke()
        }
        binding.widgetSizeInputPlus.setOnClickListener {
            onPlusClicked?.invoke()
        }
    }

}