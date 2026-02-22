package com.aslua

import android.graphics.Rect
import com.aslua.language.DefFormatter
import com.aslua.language.Language
import com.aslua.language.LanguageNonProg.Companion.getInstance
import java.util.ArrayList

/**
 * 为类C语言进行词法分析。
 * 编程语言的语法规则通过静态类变量设置。
 */
class Lexer(var callback: LexCallback?) {
    private var _hDoc: DocumentProvider? = null
    private var _workerThread: LexThread? = null

    /**
     * 对文档进行词法分析
     */
    fun tokenize(hDoc: DocumentProvider) {
        setDocument(DocumentProvider(hDoc))
        if (_workerThread == null) {
            _workerThread = LexThread(this)
            _workerThread!!.start()
        } else {
            _workerThread!!.restart()
        }
    }

    /**
     * 词法分析完成的回调
     */
    fun tokenizeDone(result: MutableList<Pair?>?) {
        if (callback != null) {
            callback!!.lexDone(result)
        }
        _workerThread = null
    }

    /**
     * 取消正在进行的词法分析
     */
    fun cancelTokenize() {
        if (_workerThread != null) {
            _workerThread!!.abort()
            _workerThread = null
        }
    }

    @Synchronized
    fun getDocument(): DocumentProvider? {
        return _hDoc
    }

    @Synchronized
    fun setDocument(hDoc: DocumentProvider?) {
        _hDoc = hDoc
    }

    interface LexCallback {
        fun lexDone(results: MutableList<Pair?>?)
    }

    private inner class LexThread(p: Lexer) : Thread() {
        private val _lexManager: Lexer = p
        private val _abort: Flag = Flag()  // 可以被其他线程设置以立即停止扫描
        private var rescan = false

        override fun run() {
            var tokens: ArrayList<Pair?>?
            do {
                rescan = false
                _abort.clear()
                tokens = getLanguage().tokenizer.tokenize(getDocument(), _abort)
            } while (rescan)

            if (!_abort.isSet()) {
                // 词法分析完成
                _lexManager.tokenizeDone(tokens)
            }
        }

        fun restart() {
            rescan = true
            _abort.set()
        }

        fun abort() {
            _abort.set()
        }
    }

    companion object {
        // 词法单元类型常量
        const val UNKNOWN = -1            // 未知类型
        const val NORMAL = 0              // 普通文本
        const val KEYWORD = 1             // 关键字
        const val OPERATOR = 2            // 运算符
        const val NAME = 3                // 标识符
        const val LITERAL = 4             // 字面量
        const val NUMBER = 5              // 数字

        /**
         * 以特殊符号开始的单词（包含该符号）
         * 示例：:ruby_symbol
         */
        const val SINGLE_SYMBOL_WORD = 10

        /**
         * 从单个起始符号（包含）开始直到行尾的词法单元
         * 每种语言最多支持两种符号，用A和B表示
         * 示例：
         * #include "myCppFile"
         * #这是Python中的注释
         * %这是Prolog中的注释
         */
        const val SINGLE_SYMBOL_LINE_A = 20
        const val SINGLE_SYMBOL_LINE_B = 21

        /**
         * 从两个起始符号（包含）开始直到行尾的词法单元
         * 示例：//这是C语言中的注释
         */
        const val DOUBLE_SYMBOL_LINE: Int = 30

        /**
         * 在起始和结束序列之间的词法单元（包含这些序列）
         * 可以跨越多行。起始和结束序列都包含2个符号。
         * 示例：
         * {- 这是Haskell中的...
         * ...多行注释 -}
         */
        const val DOUBLE_SYMBOL_DELIMITED_MULTILINE: Int = 40

        /**
         * 由相同的单个符号包围的词法单元（包含这些符号）
         * 不能跨越多行
         * 示例：'c'，"hello world"
         */
        const val SINGLE_SYMBOL_DELIMITED_A = 50
        const val SINGLE_SYMBOL_DELIMITED_B = 51

        const val MAX_KEYWORD_LENGTH = 127  // 关键字最大长度
        private var _globalLanguage: Language = getInstance()

        @JvmStatic
        @Synchronized
        fun getLanguage(): Language {
            return _globalLanguage
        }

        @Synchronized
        fun setLanguage(lang: Language) {
            _globalLanguage = lang
        }

        @JvmStatic
        fun getFormatter(): DefFormatter? {
            return _globalLanguage.formatter
        }

        @JvmField
        var mLines: ArrayList<Rect?> = ArrayList<Rect?>()
    }
}
