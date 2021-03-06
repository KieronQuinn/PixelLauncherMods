package com.kieronquinn.app.pixellaunchermods.utils.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun <T> Flow<T>.debounceIf(requirement: Boolean, time: Long): Flow<T> {
    return this.debounce { _: T ->
        if(requirement) time else 0L
    }
}

fun tickerFlow(timePeriod: Long, initialDelay: Long) = flow {
    delay(initialDelay)
    while(true){
        emit(Unit)
        delay(timePeriod)
    }
}

suspend fun <T> Flow<T?>.collectUntilNull(flowCollector: FlowCollector<T>) =
    takeWhile { it != null }.mapNotNull { it }.collect(flowCollector)

inline fun <reified T> instantCombine(vararg flows: Flow<T>) = channelFlow {
    val array = Array(flows.size) {
        false to (null as T?)
    }

    flows.forEachIndexed { index, flow ->
        launch {
            flow.collect { emittedElement ->
                array[index] = true to emittedElement
                send(array.filter { it.first }.map { it.second })
            }
        }
    }
}

suspend fun <T> Flow<T>.collectUntilTimeout(timeoutMillis: Long, collector: FlowCollector<T>) = mapLatest {
    delay(timeoutMillis)
    null
}.collectUntilNull(collector)