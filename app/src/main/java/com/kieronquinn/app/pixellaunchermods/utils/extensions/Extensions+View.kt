package com.kieronquinn.app.pixellaunchermods.utils.extensions

import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val TAP_DEBOUNCE = 250L

fun View.onApplyInsets(block: (view: View, insets: WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        block(view, windowInsets)
        windowInsets
    }
}

suspend fun View.awaitPost() = suspendCancellableCoroutine<View> {
    post {
        if(isAttachedToWindow){
            it.resume(this)
        }else{
            it.cancel()
        }
    }
}

fun View.onClicked() = callbackFlow {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.removeRipple() {
    setBackgroundResource(0)
}

fun View.delayPreDrawUntilFlow(flow: Flow<Boolean>, lifecycle: Lifecycle) {
    val listener = ViewTreeObserver.OnPreDrawListener {
        false
    }
    val removeListener = {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    lifecycle.runOnDestroy {
        removeListener()
    }
    viewTreeObserver.addOnPreDrawListener(listener)
    lifecycle.coroutineScope.launchWhenResumed {
        flow.collect {
            removeListener()
        }
    }
}

fun View.hideIme() {
    ViewCompat.getWindowInsetsController(this)?.hide(WindowInsetsCompat.Type.ime())
}