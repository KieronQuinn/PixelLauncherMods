package com.kieronquinn.app.pixellaunchermods.utils.extensions

import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun TabLayout.selectTab(index: Int) {
    getTabAt(index)?.let {
        selectTab(it)
    }
}

fun TabLayout.onSelected(includeReselection: Boolean = false) = callbackFlow {
    val listener = object: TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            trySend(tab.position)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            //No-op
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            if(includeReselection){
                trySend(tab.position)
            }
        }
    }
    addOnTabSelectedListener(listener)
    awaitClose {
        removeOnTabSelectedListener(listener)
    }
}.debounce(TAP_DEBOUNCE)