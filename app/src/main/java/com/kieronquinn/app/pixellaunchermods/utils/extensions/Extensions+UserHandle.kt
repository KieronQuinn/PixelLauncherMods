package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.os.UserHandle

fun UserHandle.getIdentifier(): Int {
    return UserHandle::class.java.getMethod("getIdentifier").invoke(this) as Int
}