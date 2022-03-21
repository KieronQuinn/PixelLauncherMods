package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

fun EditText.onChanged() = callbackFlow {
    val watcher = addTextChangedListener {
        trySend(it?.toString() ?: "")
    }
    addTextChangedListener(watcher)
    awaitClose {
        removeTextChangedListener(watcher)
    }
}.debounce(TAP_DEBOUNCE)

fun EditText.onEditorActionSent(filter: Int? = null) = callbackFlow {
    val listener = TextView.OnEditorActionListener { _, actionId, _ ->
        trySend(actionId)
        filter?.let { actionId == filter } ?: true
    }
    setOnEditorActionListener(listener)
    awaitClose {
        setOnEditorActionListener(null)
    }
}.debounce(TAP_DEBOUNCE)