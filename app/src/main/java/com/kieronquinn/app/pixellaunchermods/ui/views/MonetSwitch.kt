package com.kieronquinn.app.pixellaunchermods.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Switch
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.kieronquinn.app.pixellaunchermods.R

/**
 *  Imported from MonetCompat, modified to not require the library.
 *
 *  A full-width Switch designed to look like the primary ones in Android 12's Settings app. It has
 *  its own background, tinted to Monet's colors, with the [Switch] thumb set to the same color,
 *  and the track a darker color. The background/track color changes depending on the switch state.
 */
class MonetSwitch: SwitchCompat {

    constructor(context: Context): super(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet, androidx.appcompat.R.attr.switchStyle) {
        readAttributes(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet?, styleResId: Int): super(context, attributeSet, styleResId){
        readAttributes(attributeSet)
    }

    private fun readAttributes(attributeSet: AttributeSet?){
        if(attributeSet == null) return
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MonetSwitch)
        var textAppearance = typedArray.getResourceId(R.styleable.MonetSwitch_android_textAppearance, androidx.appcompat.R.style.TextAppearance_AppCompat_Medium)
        //Sometimes the field will default to TextAppearance.Material so we need to counter that
        if(textAppearance == android.R.style.TextAppearance_Material) textAppearance = androidx.appcompat.R.style.TextAppearance_AppCompat_Medium
        setTextAppearance(textAppearance)
        val textColor = typedArray.getColor(R.styleable.MonetSwitch_android_textColor, Color.BLACK)
        setTextColor(textColor)
        val text = typedArray.getText(R.styleable.MonetSwitch_android_text) ?: ""
        setText(text)
        typedArray.recycle()
    }

    private val monetSwitchPadding by lazy {
        context.resources.getDimension(R.dimen.monet_switch_padding).toInt()
    }

    private val monetSwitchPaddingStart by lazy {
        context.resources.getDimension(R.dimen.monet_switch_padding_start).toInt()
    }

    private val monetSwitchPaddingEnd by lazy {
        context.resources.getDimension(R.dimen.monet_switch_padding_end).toInt()
    }

    init {
        textOn = ""
        textOff = ""
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER_VERTICAL
        minHeight = resources.getDimension(R.dimen.monet_switch_height).toInt()
        if(layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            setPadding(
                monetSwitchPaddingEnd,
                monetSwitchPadding,
                monetSwitchPaddingStart,
                monetSwitchPadding
            )
        }else{
            setPadding(
                monetSwitchPaddingStart,
                monetSwitchPadding,
                monetSwitchPaddingEnd,
                monetSwitchPadding
            )
        }
        background = ContextCompat.getDrawable(context, R.drawable.switch_background)
        foreground = ContextCompat.getDrawable(context, R.drawable.switch_foreground)
        trackDrawable = ContextCompat.getDrawable(context, R.drawable.switch_track)
        thumbDrawable = ContextCompat.getDrawable(context, R.drawable.switch_thumb)
        addRipple()
        applyMonet()
    }

    private fun View.addRipple() = with(TypedValue()) {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    }

    private fun applyMonet() {
        val uncheckedTrackColor = ContextCompat.getColor(context, android.R.color.system_accent1_600)
        val checkedTrackColor = ContextCompat.getColor(context, android.R.color.system_accent1_300)
        val checkedThumbColor = ContextCompat.getColor(context, android.R.color.system_accent2_100)
        val uncheckedThumbColor = ContextCompat.getColor(context, android.R.color.system_accent2_100)
        setTint(checkedTrackColor, uncheckedTrackColor, uncheckedThumbColor, checkedThumbColor)
    }

    private fun setTint(@ColorInt checkedTrackColor: Int, @ColorInt unCheckedTrackColor: Int, @ColorInt uncheckedThumbColor: Int, @ColorInt checkedThumbColor: Int){
        trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checkedTrackColor, unCheckedTrackColor)
        )
        val bgTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checkedThumbColor, uncheckedThumbColor)
        )
        thumbTintList = bgTintList
        backgroundTintList = bgTintList
        backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        overrideRippleColor(colorStateList = bgTintList)
    }

    /**
     *  Overrides the ripple drawable background for a view to be a specific color
     *  @param color The color the ripple should be
     */
    private fun View.overrideRippleColor(@ColorInt color: Int? = null, colorStateList: ColorStateList? = null){
        val backgroundRipple = background as? RippleDrawable ?: foreground as? RippleDrawable ?: return
        if(colorStateList != null){
            backgroundRipple.setColor(colorStateList)
        }else if(color != null){
            backgroundRipple.setColor(ColorStateList.valueOf(color))
        }
    }


}