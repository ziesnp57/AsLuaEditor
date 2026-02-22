package com.aslua.language.python

import com.aslua.language.Language

class PythonLanguage private constructor() : Language() {
    override fun isWordStart(c: Char): Boolean {
        return (c == '@')
    }

    override fun isLineAStart(c: Char): Boolean {
        return false
    }

    override fun isLineBStart(c: Char): Boolean {
        return (c == '#')
    }

    override fun isLineStart(c0: Char, c1: Char): Boolean {
        return false
    }

    override fun isMultilineStartDelimiter(c0: Char, c1: Char): Boolean {
        return false
    }

    init {
        super.setKeywords(Companion.keywords)
        super.setOperators(operators)
    }

    companion object {
        private var _theOne: PythonLanguage? = null

        private val keywords = arrayOf<String?>(
            "and", "assert", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "exec", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "not", "or",
            "pass", "print", "raise", "return", "try", "while", "with",
            "yield", "True", "False", "None"
        )

        private val operators = charArrayOf(
            '(', ')', '{', '}', '.', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '~', '%', '^'
        ) // no ternary operator ? :


        fun getInstance(): PythonLanguage {
            if (_theOne == null) {
                _theOne = PythonLanguage()
            }
            return _theOne!!
        }
    }
}
