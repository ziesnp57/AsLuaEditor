package com.aslua.language.rust

import com.aslua.language.Language

class RustLanguage private constructor() : Language() {
init {
        setKeywords(Companion.keywords)
    }

    companion object {
        private var _theOne: RustLanguage? = null

        private val keywords = arrayOf<String?>(
            "as", "break", "const", "continue", "crate", "else", "enum",
            "extern", "fn", "for", "if", "impl", "in", "let", "loop",
            "match", "mod", "move", "pub", "ref", "return", "self",
            "static", "struct", "trait", "type", "unsafe", "use", "where",
            "while", "async", "await"
        )

        fun getInstance(): RustLanguage {
            if (_theOne == null) {
                _theOne = RustLanguage()
            }
            return _theOne!!
        }
    }
}