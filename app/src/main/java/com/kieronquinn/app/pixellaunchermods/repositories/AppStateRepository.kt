package com.kieronquinn.app.pixellaunchermods.repositories

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface AppStateRepository {

    val appInForeground: StateFlow<Boolean>

    fun onResume()
    fun onPause()

}

class AppStateRepositoryImpl: AppStateRepository {

    override val appInForeground = MutableStateFlow(false)

    override fun onResume() {
        GlobalScope.launch {
            appInForeground.emit(true)
        }
    }

    override fun onPause() {
        GlobalScope.launch {
            appInForeground.emit(false)
        }
    }

}