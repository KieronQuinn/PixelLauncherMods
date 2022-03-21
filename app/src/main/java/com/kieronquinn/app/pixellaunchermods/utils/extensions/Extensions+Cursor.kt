package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.database.Cursor

fun <T> Cursor.map(row: (Cursor) -> T): List<T> {
    moveToFirst()
    if(isAfterLast) return emptyList()
    val list = ArrayList<T>()
    do {
        try {
            list.add(row(this))
        }catch (e: IllegalStateException){
            //Cursor has hit a snag, suppress for now
            //TODO try to find a better solution to this - probably concurrent change with the launcher
        }
    }while (moveToNext())
    return list
}

fun <T> Cursor.firstNotNull(row: (Cursor) -> T?): T? {
    moveToFirst()
    if(isAfterLast){
        return null
    }
    do {
        val processed = row(this)
        if(processed != null){
            return processed
        }
    }while (moveToNext())
    return null
}