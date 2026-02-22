package com.aslua.language

interface Formatter {
    fun createAutoIndent(text: CharSequence?): Int

    fun format(text: CharSequence?, width: Int): CharSequence?
}