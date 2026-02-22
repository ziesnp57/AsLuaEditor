package com.aslua

class Pair(x: Int, y: Int) {
    @JvmField
    var first: Int
    @JvmField
    var second: Int

    init {
        first = x
        second = y
    }

    fun setFirst(value: Int) {
        first = value
    }

    fun setSecond(value: Int) {
        second = value
    }
}
