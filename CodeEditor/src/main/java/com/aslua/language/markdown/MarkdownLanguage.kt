package com.aslua.language.markdown

import com.aslua.language.Language

class MarkdownLanguage private constructor() : Language() {
    fun isHeader(line: String): Boolean {
        return line.startsWith("#")
    }

    fun isListItem(line: String): Boolean {
        return line.startsWith("-") || line.startsWith("*") || line.startsWith("+")
    }

    fun extractLink(text: String): Pair<String, String>? {
        val regex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.destructured?.let { (label, url) -> Pair(label, url) }
    }

    fun isBlockQuote(line: String): Boolean {
        return line.startsWith(">")
    }

    fun isCodeBlock(line: String): Boolean {
        return line.startsWith("```")
    }

    companion object {
        private var _theOne: MarkdownLanguage? = null

        fun getInstance(): MarkdownLanguage {
            if (_theOne == null) {
                _theOne = MarkdownLanguage()
            }
            return _theOne!!
        }
    }
}