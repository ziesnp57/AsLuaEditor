package com.aslua

class Flag {
    private var state = false

    @Synchronized
    fun set() {
        state = true
    }

    @Synchronized
    fun clear() {
        state = false
    }

    @Synchronized
    fun isSet(): Boolean {
        return state
    }
}
