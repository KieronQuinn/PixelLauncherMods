package com.kieronquinn.app.pixellaunchermods.model.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GridSize(val x: Int, val y: Int): Parcelable {

    companion object {
        private const val LAUNCHER_DB_PREFIX = "launcher"
        private const val LAUNCHER_DB_FILETYPE = ".db"
    }

    fun toLauncherFilename(): String {
        return if(x == 5 && y == 5) {
            "$LAUNCHER_DB_PREFIX$LAUNCHER_DB_FILETYPE"
        }else{
            "${LAUNCHER_DB_PREFIX}_${x}_by_${y}$LAUNCHER_DB_FILETYPE"
        }
    }

}