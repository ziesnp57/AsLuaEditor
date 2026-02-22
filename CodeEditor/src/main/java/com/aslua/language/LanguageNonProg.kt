package com.aslua.language

/**
 * 无编程语言类
 */
class LanguageNonProg private constructor() : Language() {
    init {
        super.setKeywords(Companion.keywords)
        super.setOperators(operators)
    }

    override fun isProgLang(): Boolean {
        return false
    }

    override fun isEscapeChar(c: Char): Boolean {
        return false
    }

    override fun isDelimiterA(c: Char): Boolean {
        return false
    }

    override fun isDelimiterB(c: Char): Boolean {
        return false
    }

    override fun isLineAStart(c: Char): Boolean {
        return false
    }

    override fun isLineStart(c0: Char, c1: Char): Boolean {
        return false
    }

    override fun isMultilineStartDelimiter(c0: Char, c1: Char): Boolean {
        return false
    }

    override fun isMultilineEndDelimiter(c0: Char, c1: Char): Boolean {
        return false
    }

    companion object {
        private var _theOne: LanguageNonProg? = null

        private val keywords = arrayOf<String?>()

        private val operators = charArrayOf()


        @JvmStatic
		fun getInstance(): LanguageNonProg {
            if (_theOne == null) {
                _theOne = LanguageNonProg()
            }
            return _theOne!!
        }
    }
}
