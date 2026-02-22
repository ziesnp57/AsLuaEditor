package com.aslua

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import com.aslua.DocumentProvider.OnTextChangeListener
import com.aslua.language.lua.LuaLanguage.Companion.getInstance
import com.aslua.view.AutoCompletePanel
import com.aslua.view.CodeTextField
import com.aslua.view.YoyoNavigationMethod
import com.aslua.view.listener.OnKeyShortcutListener
import com.aslua.view.listener.TextChangeListener

/**
 * 代码编辑器
 */
class CodeEditor : CodeTextField {
    private var _onKeyShortcutListener: OnKeyShortcutListener =
        OnKeyShortcutListener { keyCode: Int, event: KeyEvent? ->
            val filteredMetaState = event!!.metaState and KeyEvent.META_CTRL_MASK.inv()
            if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
                when (keyCode) {
                    KeyEvent.KEYCODE_A -> {
                        selectAll()
                        return@OnKeyShortcutListener true
                    }

                    KeyEvent.KEYCODE_X -> {
                        cut()
                        return@OnKeyShortcutListener true
                    }

                    KeyEvent.KEYCODE_C -> {
                        copy()
                        return@OnKeyShortcutListener true
                    }

                    KeyEvent.KEYCODE_V -> {
                        paste()
                        return@OnKeyShortcutListener true
                    }
                }
            }
            false
        }
    private var _isWordWrap = false

    private var _index = 0

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        initFont(context)
        val dm = context.resources.displayMetrics
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            BASE_TEXT_SIZE_PIXELS.toFloat(),
            dm
        )
        setTextSize(size.toInt()) // 设置字体大小
        isShowLineNumbers = true // 显示行号
        setWordWrap(true) // 自动换行
        setAutoIndent(true) // 自动缩进
        setAutoComplete(true) // 设置自动补全面板
        setHighlightCurrentRow(true) // 高亮当前行
        setNavigationMethod(YoyoNavigationMethod(this))  // 设置导航方法

    }

    fun initFont(context: Context) {
        setTypeface(Typeface.MONOSPACE)
        // 从资源中加载字体
        val font = Typeface.createFromAsset(context.assets, "fonts/jetbrainsmono_medium.ttf")

        if (font != null) {
            setTypeface(font)
        } else {
            // 处理字体加载失败的情况
            Log.e("FontError", "无法加载字体")
        }
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (_index != 0 && right > 0) {
            moveCaret(_index)
            _index = 0
        }
    }

    fun addNames(names: Array<String?>) {
        val lang = Lexer.getLanguage()
        val old = lang.names
        val news = arrayOfNulls<String>(old.size + names.size)
        System.arraycopy(old, 0, news, 0, old.size)
        System.arraycopy(names, 0, news, old.size, names.size)
        lang.setNames(news)
        Lexer.setLanguage(lang)
        setTabSpaces(4)
        respan()
        invalidate()
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent?): Boolean {
        return _onKeyShortcutListener.onKeyShortcut(keyCode, event)
                || super.onKeyShortcut(keyCode, event)
    }

    // 设置快捷键监听器
    fun setOnKeyShortcutListener(l: OnKeyShortcutListener) {
        _onKeyShortcutListener = l
    }

    //自动换行
    override fun setWordWrap(enable: Boolean) {
        _isWordWrap = enable
        super.setWordWrap(enable)
    }


    fun insert(idx: Int, text: String?) {
        selectText(false)
        moveCaret(idx)
        paste(text)
        //_hDoc.insert(idx,text);
    }


    // 获取文本
    fun getText(): DocumentProvider {
        return createDocumentProvider()
    }

    // 设置文本
    fun setText(c: CharSequence) {
        val doc = Document(this)
        doc.isWordWrap()
        doc.setText(c)
        setDocumentProvider(DocumentProvider(doc)) // 设置文档提供者
        doc.analyzeWordWrap() // 分析自动换行
    }

    fun setText(c: CharSequence, isRep: Boolean) {
        replaceText(0, length - 1, c.toString())
    }


    // 获取当前选中的文本
    fun getSelectedText(): String {
        return _hDoc.subSequence(selectionStart, selectionEnd - selectionStart)
            .toString()
    }


    // 设置光标位置
    fun setSelection(index: Int) {
        selectText(false)
        if (!hasLayout()) moveCaret(index)
        else _index = index
    }

    // 移动光标
    fun gotoLine(line: Int) {
        var line = line
        if (line > _hDoc.rowCount) {
            line = _hDoc.rowCount  // 如果指定的行号大于文档的行数，设定行号为文档的行数
        }
        val i = getText().getLineOffset(line - 1)  // 获取指定行的起始索引位置
        setSelection(i)  // 设置光标到指定位置
    }


    //
    fun canRedo(): Boolean {
        return createDocumentProvider().canRedo()
    }

    // 撤销
    fun canUndo(): Boolean {
        return createDocumentProvider().canUndo()
    }


    // 撤销
    fun undo() {
        val doc = createDocumentProvider()
        val newPosition = doc.undo()
        if (newPosition >= 0) {
            isEdited = true
            respan()
            selectText(false)
            moveCaret(newPosition)
            invalidate()
        }
    }

    fun redo() {
        val doc = createDocumentProvider()
        val newPosition = doc.redo()
        if (newPosition >= 0) {
            isEdited = true
            respan()
            selectText(false)
            moveCaret(newPosition)
            invalidate()
        }
    }

    override fun paste(text: String?) {
        if (text != null) {
            super.paste(text)
        }
    }


    // 设置语言与自动补全
    fun setLanguage() {
        Lexer.setLanguage(getInstance())
        AutoCompletePanel.setLanguage(getInstance())
    }


}
