package com.aslua.view.listener

fun interface OnSelectionChangedListener {
    fun onSelectionChanged(active: Boolean, selStart: Int, selEnd: Int)
}
