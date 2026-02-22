package com.aslua.language.c

import com.aslua.language.Language

/**
 * Singleton class containing the symbols and operators of the C language
 */
class CLanguage private constructor() : Language() {
    init {
        super.setKeywords(Companion.keywords)
    }

    companion object {
        private var _theOne: Language? = null

        private val keywords = arrayOf<String?>(
            "char", "double", "float", "int", "long", "short", "void",
            "auto", "const", "extern", "register", "static", "volatile",
            "signed", "unsigned", "sizeof", "typedef",
            "enum", "struct", "union",
            "break", "case", "continue", "default", "do", "else", "for",
            "goto", "if", "return", "switch", "while"
        )

        fun getInstance(): Language {
            if (_theOne == null) {
                _theOne = CLanguage()
            }
            return _theOne!!
        }
    }
}
