package com.aslua.view

import android.view.KeyEvent
import com.aslua.language.Language

object KeysInterpreter {
    fun isSwitchPanel(event: KeyEvent): Boolean {
        return (event.isShiftPressed &&
                (event.keyCode == KeyEvent.KEYCODE_ENTER))
    }

    /**
     * Maps shortcut keys and Android keycodes to printable characters.
     * Note that whitespace is considered printable.
     *
     * @param event The KeyEvent to interpret
     * @return The printable character the event represents,
     * or Language.NULL_CHAR if the event does not represent a printable char
     */
	@JvmStatic
	fun keyEventToPrintableChar(event: KeyEvent): Char {
        var c = Language.NULL_CHAR

        if (isNewline(event)) {
            c = Language.NEWLINE
        } else if (isBackspace(event)) {
            c = Language.BACKSPACE
        } else if (isTab(event)) {
            c = Language.TAB
        } else if (isSpace(event)) {
            c = ' '
        } else if (event.isPrintingKey) {
            c = event.getUnicodeChar(event.metaState).toChar()
        }

        return c
    }

    private fun isTab(event: KeyEvent): Boolean {
        return (event.isShiftPressed &&
                (event.keyCode == KeyEvent.KEYCODE_SPACE)) ||
                (event.keyCode == KeyEvent.KEYCODE_TAB)
    }

    private fun isBackspace(event: KeyEvent): Boolean {
        return (event.keyCode == KeyEvent.KEYCODE_DEL)
    }

    private fun isNewline(event: KeyEvent): Boolean {
        return (event.keyCode == KeyEvent.KEYCODE_ENTER)
    }

    private fun isSpace(event: KeyEvent): Boolean {
        return (event.keyCode == KeyEvent.KEYCODE_SPACE)
    }

    @JvmStatic
	fun isNavigationKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
    }
}
