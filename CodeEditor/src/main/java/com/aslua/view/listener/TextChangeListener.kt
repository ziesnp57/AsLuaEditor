package com.aslua.view.listener

interface TextChangeListener {
    fun onNewLine(c: String?, caretPosition: Int, p2: Int)

    fun onDel(text: CharSequence?, caretPosition: Int, newCursorPosition: Int)

    fun onAdd(text: CharSequence?, caretPosition: Int, newCursorPosition: Int)
}