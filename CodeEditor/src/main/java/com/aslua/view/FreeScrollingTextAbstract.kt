package com.aslua.view

import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import android.view.KeyCharacterMap
import android.view.View
import com.aslua.Document

/**
 * 文本编辑器行号显示，与文本滚动同步
 */
abstract class FreeScrollingTextAbstract : View, Document.TextFieldMetrics {
    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun isSaveEnabled(): Boolean {
        return true
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )


    companion object {
        @JvmField
        protected var PICKER_SETS: SparseArray<String?> = SparseArray<String?>()

        init {
            PICKER_SETS.put('A'.code, "\u00C0\u00C1\u00C2\u00C4\u00C6\u00C3\u00C5\u0104\u0100")
            PICKER_SETS.put('C'.code, "\u00C7\u0106\u010C")
            PICKER_SETS.put('D'.code, "Ď")
            PICKER_SETS.put('E'.code, "È\u00C9\u00CA\u00CB\u0118\u011A\u0112")
            PICKER_SETS.put('G'.code, "\u011E")
            PICKER_SETS.put('L'.code, "\u0141")
            PICKER_SETS.put('I'.code, "\u00CC\u00CD\u00CE\u00CF\u012A\u0130")
            PICKER_SETS.put('N'.code, "\u00D1\u0143\u0147")
            PICKER_SETS.put('O'.code, "\u00D8\u0152\u00D5\u00D2\u00D3\u00D4\u00D6\u014C")
            PICKER_SETS.put('R'.code, "\u0158")
            PICKER_SETS.put('S'.code, "\u015A\u0160\u015E")
            PICKER_SETS.put('T'.code, "\u0164")
            PICKER_SETS.put('U'.code, "\u00D9\u00DA\u00DB\u00DC\u016E\u016A")
            PICKER_SETS.put('Y'.code, "\u00DD\u0178")
            PICKER_SETS.put('Z'.code, "\u0179\u017B\u017D")
            PICKER_SETS.put('a'.code, "\u00E0\u00E1\u00E2\u00E4\u00E6\u00E3\u00E5\u0105\u0101")
            PICKER_SETS.put('c'.code, "\u00E7\u0107\u010D")
            PICKER_SETS.put('d'.code, "\u010F")
            PICKER_SETS.put('e'.code, "\u00E8\u00E9\u00EA\u00EB\u0119\u011B\u0113")
            PICKER_SETS.put('g'.code, "\u011F")
            PICKER_SETS.put('i'.code, "\u00EC\u00ED\u00EE\u00EF\u012B\u0131")
            PICKER_SETS.put('l'.code, "\u0142")
            PICKER_SETS.put('n'.code, "\u00F1\u0144\u0148")
            PICKER_SETS.put('o'.code, "\u00F8\u0153\u00F5\u00F2\u00F3\u00F4\u00F6\u014D")
            PICKER_SETS.put('r'.code, "\u0159")
            PICKER_SETS.put('s'.code, "\u00A7\u00DF\u015B\u0161\u015F")
            PICKER_SETS.put('t'.code, "\u0165")
            PICKER_SETS.put('u'.code, "\u00F9\u00FA\u00FB\u00FC\u016F\u016B")
            PICKER_SETS.put('y'.code, "\u00FD\u00FF")
            PICKER_SETS.put('z'.code, "\u017A\u017C\u017E")
            PICKER_SETS.put(
                KeyCharacterMap.PICKER_DIALOG_INPUT.code,
                "\u2026\u00A5\u2022\u00AE\u00A9\u00B1[]{}\\|"
            )
            PICKER_SETS.put('/'.code, "\\")


            PICKER_SETS.put('1'.code, "\u00b9\u00bd\u2153\u00bc\u215b")
            PICKER_SETS.put('2'.code, "\u00b2\u2154")
            PICKER_SETS.put('3'.code, "\u00b3\u00be\u215c")
            PICKER_SETS.put('4'.code, "\u2074")
            PICKER_SETS.put('5'.code, "\u215d")
            PICKER_SETS.put('7'.code, "\u215e")
            PICKER_SETS.put('0'.code, "\u207f\u2205")
            PICKER_SETS.put('$'.code, "\u00a2\u00a3\u20ac\u00a5\u20a3\u20a4\u20b1")
            PICKER_SETS.put('%'.code, "\u2030")
            PICKER_SETS.put('*'.code, "\u2020\u2021")
            PICKER_SETS.put('-'.code, "\u2013\u2014")
            PICKER_SETS.put('+'.code, "\u00b1")
            PICKER_SETS.put('('.code, "[{<")
            PICKER_SETS.put(')'.code, "]}>")
            PICKER_SETS.put('!'.code, "\u00a1")
            PICKER_SETS.put('"'.code, "\u201c\u201d\u00ab\u00bb\u02dd")
            PICKER_SETS.put('?'.code, "\u00bf")
            PICKER_SETS.put(','.code, "\u201a\u201e")
            PICKER_SETS.put('='.code, "\u2260\u2248\u221e")
            PICKER_SETS.put('<'.code, "\u2264\u00ab\u2039")
            PICKER_SETS.put('>'.code, "\u2265\u00bb\u203a")
        }
    }
}
