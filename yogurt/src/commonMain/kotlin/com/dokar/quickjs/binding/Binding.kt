package com.dokar.quickjs.binding

import com.dokar.quickjs.QuickJs

class JsObject(private val backing: MutableMap<String, Any?> = linkedMapOf()) : MutableMap<String, Any?> by backing

open class ObjectBindingScope {
    fun function(name: String, block: (Array<Any?>) -> Any?) = Unit
    fun asyncFunction(name: String, block: suspend (Array<Any?>) -> Any?) = Unit
}

fun QuickJs.define(name: String, block: ObjectBindingScope.() -> Unit) {
    ObjectBindingScope().apply(block)
}
