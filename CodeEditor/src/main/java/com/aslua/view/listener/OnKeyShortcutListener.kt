package com.aslua.view.listener

import android.view.KeyEvent

fun interface OnKeyShortcutListener {
    fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean
}
