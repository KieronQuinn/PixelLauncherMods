package com.kieronquinn.app.pixellaunchermods.ui.screens.tweaks.widgetresize

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WidgetResizePagerAdapter(fragment: Fragment, var pageCount: Int, var spanX: Int, var spanY: Int): FragmentStateAdapter(fragment) {

    override fun getItemCount() = pageCount

    override fun createFragment(position: Int): Fragment {
        return WidgetResizePageFragment.newInstance(position, spanX, spanY)
    }

}