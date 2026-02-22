package com.aslua.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import com.aslua.DocumentProvider
import com.aslua.Lexer
import com.aslua.language.Language

/**
 * 代码提示文本框
 */
abstract class CodeTextField : FreeScrollingTextField {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setLanguage(lan: Language?) {
        Lexer.setLanguage(lan!!)
        AutoCompletePanel.setLanguage(lan)
    }

    fun format() {
        selectText(false)
        val text = Lexer.getFormatter()?.format(DocumentProvider(_hDoc), autoIndentWidth)
        _hDoc.beginBatchEdit()
        _hDoc.deleteAt(0, _hDoc.docLength() - 1, System.nanoTime())
        checkNotNull(text)
        _hDoc.insertBefore(text.toString().toCharArray(), 0, System.nanoTime())
        _hDoc.endBatchEdit()
        _hDoc.clearSpans()
        respan()
        invalidate()
    }

    fun getDocumentProvider(): DocumentProvider? {
        return _hDoc
    }

    fun getUiState(): TextFieldUiState {
        return TextFieldUiState(this)
    }

    fun restoreUiState(state: Parcelable?) {
        val uiState = state as TextFieldUiState
        if (uiState.doc != null) setDocumentProvider(uiState.doc)
        val caretPosition = uiState.caretPosition
        scrollX = uiState.scrollX
        scrollY = uiState.scrollY

        if (uiState.selectMode) {
            val selStart = uiState.selectBegin
            val selEnd = uiState.selectEnd

            post(Runnable {
                setSelectionRange(selStart, selEnd - selStart)
                if (caretPosition < selEnd) {
                    focusSelectionStart() //caret at the end by default
                }
            })
        } else {
            post(Runnable { moveCaret(caretPosition) })
        }
    }

    //*********************************************************************
    //**************** UI State for saving and restoring ******************
    //*********************************************************************
    class TextFieldUiState : Parcelable {
        val caretPosition: Int
        val scrollX: Int
        val scrollY: Int
        val selectMode: Boolean
        val selectBegin: Int
        val selectEnd: Int
        var doc: DocumentProvider? = null

        constructor(textField: CodeTextField) {
            caretPosition = textField.caretPosition
            scrollX = textField.scrollX
            scrollY = textField.scrollY
            selectMode = textField.isSelectText
            selectBegin = textField.selectionStart
            selectEnd = textField.selectionEnd
            doc = textField.getDocumentProvider()
        }

        private constructor(`in`: Parcel) {
            caretPosition = `in`.readInt()
            scrollX = `in`.readInt()
            scrollY = `in`.readInt()
            selectMode = `in`.readInt() != 0
            selectBegin = `in`.readInt()
            selectEnd = `in`.readInt()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            out.writeInt(caretPosition)
            out.writeInt(scrollX)
            out.writeInt(scrollY)
            out.writeInt(if (selectMode) 1 else 0)
            out.writeInt(selectBegin)
            out.writeInt(selectEnd)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<TextFieldUiState?> =
                object : Parcelable.Creator<TextFieldUiState?> {
                    override fun createFromParcel(`in`: Parcel): TextFieldUiState {
                        return TextFieldUiState(`in`)
                    }

                    override fun newArray(size: Int): Array<TextFieldUiState?> {
                        return arrayOfNulls<TextFieldUiState>(size)
                    }
                }
        }
    }
}
