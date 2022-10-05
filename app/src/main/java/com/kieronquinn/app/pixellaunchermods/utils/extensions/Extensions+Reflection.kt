package com.kieronquinn.app.pixellaunchermods.utils.extensions

import kotlin.reflect.KProperty

inline fun <reified C, R> C.reflect(vararg fieldNames: String): Reflect<C, R> {
    fieldNames.forEach { fieldName ->
        try {
            return Reflect(C::class.java, this, fieldName)
        }catch (e: NoSuchFieldException){
            return@forEach
        }
    }
    throw NoSuchFieldException()
}

class Reflect<C, R> constructor(clazz: Class<out C>, private val instance: C, fieldName: String) {

    private val field = clazz.getDeclaredField(fieldName).apply {
        isAccessible = true
    }

    operator fun getValue(ref: Any?, prop: KProperty<*>): R {
        return field.get(instance) as R
    }

    operator fun setValue(ref: Any?, prop: KProperty<*>, v: R) {
        field.set(instance, v)
    }
}