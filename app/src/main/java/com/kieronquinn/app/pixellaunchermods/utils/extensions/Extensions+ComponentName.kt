package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.content.ComponentName
import java.lang.Exception

fun String.parseToComponentName(): ComponentName? {
    return try {
        ComponentName.unflattenFromString(this)
    }catch (e: Exception){
        null
    }
}

fun ComponentName.unshortenToString(): String {
    val clazz = if(className.startsWith(".")){
        "$packageName$className"
    }else className
    return ComponentName(packageName, clazz).flattenToString()
}