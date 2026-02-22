package com.aslua.language.javascript

import com.aslua.language.Language

class JavaScriptLanguage private constructor() : Language() {
    init {
        super.setKeywords(Companion.keywords)
    }

    override fun isLineAStart(c: Char): Boolean {
        return false
    }

    companion object {
        private var _theOne: JavaScriptLanguage? = null

        private val keywords = arrayOf<String?>(
            "abstract", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "debugger", "default", "delete", "do",
            "double", "else", "enum", "export", "extends", "false", "final",
            "finally", "float", "for", "function", "goto", "if", "implements",
            "import", "in", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public", "return",
            "short", "static", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "true", "try", "typeof", "var", "void",
            "volatile", "while", "with"
        )

        fun getInstance(): JavaScriptLanguage {
            if (_theOne == null) {
                _theOne = JavaScriptLanguage()
            }
            return _theOne!!
        }
    }
}
