package com.kieronquinn.app.pixellaunchermods.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.kieronquinn.app.pixellaunchermods.R

/**
 *  A full-width Switch designed to look like the primary ones in Android 12's Settings app. It has
 *  its own background, tinted to Monet's colors, with the [Switch] thumb set to the same color,
 *  and the track a darker color. The background/track color changes depending on the switch state.
 */
open class MonetSwitch: FrameLayout {

    constructor(context: Context): super(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet, 0) {
        readAttributes(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet?, styleResId: Int): super(context, attributeSet, styleResId){
        readAttributes(attributeSet)
    }

    private val layoutInflater = LayoutInflater.from(context)
    private var suppressCheckChange = false

    private val layout by lazy {
        layoutInflater.inflate(R.layout.view_monet_switch, this, false)
    }

    private val label by lazy {
        layout.findViewById<TextView>(R.id.view_monet_switch_text)
    }

    private val switch by lazy {
        layout.findViewById<MaterialSwitch>(R.id.view_monet_switch_switch)
    }

    private val root by lazy {
        layout.findViewById<LinearLayout>(R.id.view_monet_switch_root)
    }

    var isChecked: Boolean
        get() = root.isActivated
        set(value) {
            suppressCheckChange = true
            switch.isChecked = value
            root.isActivated = value
            suppressCheckChange = false
        }

    var text: CharSequence
        get() = label.text
        set(value) {
            label.text = value
        }

    var typeface: Typeface?
        get() = label.typeface
        set(value) {
            label.typeface = typeface
        }

    @SuppressLint("PrivateResource", "CustomViewStyleable")
    private fun readAttributes(attributeSet: AttributeSet?){
        if(attributeSet == null || isInEditMode) return
        addView(layout)
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MonetSwitch)
        var textAppearance = typedArray.getResourceId(R.styleable.MonetSwitch_android_textAppearance, androidx.appcompat.R.style.TextAppearance_AppCompat_Medium)
        //Sometimes the field will default to TextAppearance.Material so we need to counter that
        if(textAppearance == android.R.style.TextAppearance_Material) textAppearance = androidx.appcompat.R.style.TextAppearance_AppCompat_Medium
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            label.setTextAppearance(textAppearance)
        }else{
            label.setTextAppearance(context, textAppearance)
        }
        val materialTypedArray = context.obtainStyledAttributes(attributeSet, com.google.android.material.R.styleable.MaterialSwitch)
        val thumbIcon = materialTypedArray.getResourceId(com.google.android.material.R.styleable.MaterialSwitch_thumbIcon, 0)
        val thumbTint = materialTypedArray.getColor(com.google.android.material.R.styleable.MaterialSwitch_thumbIconTint, 0)
        val thumbTintMode = materialTypedArray.getInt(com.google.android.material.R.styleable.MaterialSwitch_thumbIconTintMode, 0)
        if(thumbIcon != 0){
            switch.setThumbIconResource(thumbIcon)
        }
        if(thumbTint != 0){
            switch.thumbIconTintList = ColorStateList.valueOf(thumbTint)
        }
        if(thumbTintMode != 0){
            switch.thumbIconTintMode = PorterDuff.Mode.values()[thumbTintMode]
        }
        val textColor = typedArray.getColor(R.styleable.MonetSwitch_android_textColor, Color.BLACK)
        label.setTextColor(textColor)
        val switchText = typedArray.getText(R.styleable.MonetSwitch_android_text) ?: ""
        label.text = switchText
        typedArray.recycle()
        materialTypedArray.recycle()
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyMonet()
    }

    private fun applyMonet() {
        val checkedThumbColor = ContextCompat.getColor(context, android.R.color.system_accent2_100)
        val uncheckedThumbColor = ContextCompat.getColor(context, android.R.color.system_accent2_100)
        setTint(uncheckedThumbColor, checkedThumbColor)
    }

    private fun setTint(
        @ColorInt uncheckedThumbColor: Int,
        @ColorInt checkedThumbColor: Int
    ){
        val bgTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checkedThumbColor, uncheckedThumbColor)
        )
        root.backgroundTintList = bgTintList
        backgroundTintList = bgTintList
        backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        switch.setOnCheckedChangeListener { _, _ ->
            root.isActivated = switch.isChecked
            if(!suppressCheckChange){
                performClick()
            }
        }
        root.setOnClickListener {
            switch.toggle()
        }
    }

}