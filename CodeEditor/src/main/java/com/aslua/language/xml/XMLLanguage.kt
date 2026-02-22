package com.aslua.language.xml

import com.aslua.language.Language

/**
 * Singleton class containing the symbols and operators of the Javascript language
 */
class XMLLanguage private constructor() : Language() {
    /**
     * Whether the word after c is a token
     */
    fun isWordStart2(c: Char): Boolean {
        return (c == '.')
    }

    override fun isLineAStart(c: Char): Boolean {
        return false
    }

    /**
     * Whether c0c1 signifies the start of a multi-line token
     */
    fun isMultilineStartDelimiter(c0: Char, c1: Char, c2: Char, c3: Char): Boolean {
        return (c0 == '<' && c1 == '!' && c2 == '-' && c3 == '-')
    }

    /**
     * Whether c0c1 signifies the end of a multi-line token
     */
    fun isMultilineEndDelimiter(c0: Char, c1: Char, c2: Char): Boolean {
        return (c0 == '-' && c1 == '-' && c2 == '>')
    }

    override fun isMultilineStartDelimiter(c0: Char, c1: Char): Boolean {
        return (c0 == '<' && c1 == '!')
    }

    /**
     * Whether c0c1 signifies the end of a multi-line token
     */
    override fun isMultilineEndDelimiter(c0: Char, c1: Char): Boolean {
        return (c0 == '-' && c1 == '>')
    }

    companion object {
        private var _theOne: XMLLanguage? = null

        fun getInstance(): Language {
            if (_theOne == null) {
                _theOne = XMLLanguage()
            }
            return _theOne!!
        }
    }
}

