package com.aslua.language.lua

import android.graphics.Rect
import com.aslua.DocumentProvider
import com.aslua.Flag
import com.aslua.Lexer
import com.aslua.Pair
import com.aslua.TextWarriorException
import com.aslua.language.LexerTokenizer
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList

class LuaTokenizer : LexerTokenizer() {
    override fun tokenize(hDoc: DocumentProvider, abort: Flag): ArrayList<Pair> {
        val rowCount = hDoc.rowCount
        val maxRow = 999999
        
        // 根据文档大小估算初始容量
        val estimatedTokens = (hDoc.length / 8).coerceAtLeast(256)
        val tokens = ArrayList<Pair>(estimatedTokens)
        val lines = ArrayList<Rect?>(estimatedTokens / 4)
        val lineStacks = ArrayList<Rect>(32)
        val lineStacks2 = ArrayList<Rect>(32)

        val lexer = LuaLexer(hDoc)
        val language = Lexer.getLanguage()
        language.clearUserWord()
        
        // 重用StringBuilder
        val bul = StringBuilder(64)
        
        try {
            var idx = 0
            var lastType: LuaType? = null
            var lastType2: LuaType? = null
            var lastName = ""
            var lastPair: Pair? = null
            var lastLen = 0
            var isModule = false
            var hasDo = true
            var lastNameIdx = -1
            
            while (!abort.isSet()) {
                val type = lexer.advance() ?: break
                val len = lexer.yylength()
                var currentPair: Pair? = null

                if (isModule && lastType == LuaType.STRING && type != LuaType.STRING) {
                    if (bul.length > 2) {
                        language.addUserWord(bul.substring(1, bul.length - 1))
                    }
                    bul.setLength(0)
                    isModule = false
                }

                lastLen = len
                when (type) {
                    LuaType.DO -> {
                        if (hasDo) {
                            lineStacks.add(Rect(lexer.yychar(), lexer.yyline(), 0, lexer.yyline()))
                        }
                        hasDo = true
                        currentPair = Pair(len, Lexer.KEYWORD)
                        tokens.add(currentPair)
                    }

                    LuaType.WHILE, LuaType.FOR -> {
                        hasDo = false
                        lineStacks.add(Rect(lexer.yychar(), lexer.yyline(), 0, lexer.yyline()))
                        currentPair = Pair(len, Lexer.KEYWORD)
                        tokens.add(currentPair)
                    }

                    LuaType.FUNCTION, LuaType.IF, LuaType.SWITCH -> {
                        lineStacks.add(Rect(lexer.yychar(), lexer.yyline(), 0, lexer.yyline()))
                        currentPair = Pair(len, Lexer.KEYWORD)
                        tokens.add(currentPair)
                    }

                    LuaType.END -> {
                        val size = lineStacks.size
                        if (size > 0) {
                            val rect = lineStacks.removeAt(size - 1)
                            rect.bottom = lexer.yyline()
                            rect.right = lexer.yychar()
                            if (rect.bottom - rect.top > 1) lines.add(rect)
                        }
                        currentPair = Pair(len, Lexer.KEYWORD)
                        tokens.add(currentPair)
                        hasDo = true
                    }

                    LuaType.TRUE, LuaType.FALSE, LuaType.NOT, LuaType.AND, LuaType.OR, LuaType.THEN, LuaType.ELSEIF, LuaType.ELSE, LuaType.IN, LuaType.RETURN, LuaType.BREAK, LuaType.LOCAL, LuaType.REPEAT, LuaType.UNTIL, LuaType.NIL, LuaType.CASE, LuaType.DEFAULT, LuaType.CONTINUE, LuaType.GOTO -> {
                        currentPair = Pair(len, Lexer.KEYWORD)
                        tokens.add(currentPair)
                    }

                    LuaType.LCURLY -> {
                        lineStacks2.add(Rect(lexer.yychar(), lexer.yyline(), 0, lexer.yyline()))
                        currentPair = Pair(len, Lexer.OPERATOR)
                        tokens.add(currentPair)
                    }

                    LuaType.RCURLY -> {
                        val size2 = lineStacks2.size
                        if (size2 > 0) {
                            val rect = lineStacks2.removeAt(size2 - 1)
                            rect.bottom = lexer.yyline()
                            rect.right = lexer.yychar()
                            if (rect.bottom - rect.top > 1) lines.add(rect)
                        }
                        currentPair = Pair(len, Lexer.OPERATOR)
                        tokens.add(currentPair)
                    }

                    LuaType.LPAREN, LuaType.RPAREN, LuaType.LBRACK, LuaType.RBRACK, LuaType.COMMA, LuaType.DOT -> {
                        currentPair = Pair(len, Lexer.OPERATOR)
                        tokens.add(currentPair)
                    }

                    LuaType.STRING, LuaType.LONG_STRING -> {
                        if (rowCount > maxRow) break
                        
                        currentPair = Pair(len, Lexer.SINGLE_SYMBOL_DELIMITED_A)
                        tokens.add(currentPair)
                        
                        if (lastName == "require") {
                            isModule = true
                            bul.append(lexer.yytext())
                        }
                    }

                    LuaType.NAME -> {
                        if (rowCount > maxRow) {
                            currentPair = Pair(len, Lexer.NORMAL)
                            tokens.add(currentPair)
                            break
                        }
                        if (lastType2 == LuaType.NUMBER) {
                            val p = tokens[tokens.size - 1]
                            p.setSecond(Lexer.NORMAL)
                            p.setFirst(p.first + len)
                        }
                        val name = lexer.yytext()
                        if (lastType == LuaType.FUNCTION) {
                            currentPair = Pair(len, Lexer.LITERAL)
                            tokens.add(currentPair)
                            language.addUserWord(name)
                        } else if (language.isUserWord(name)) {
                            currentPair = Pair(len, Lexer.LITERAL)
                            tokens.add(currentPair)
                        } else if (lastType == LuaType.GOTO || lastType == LuaType.AT) {
                            currentPair = Pair(len, Lexer.LITERAL)
                            tokens.add(currentPair)
                        } else if (language.isBasePackage(name)) {
                            currentPair = Pair(len, Lexer.NAME)
                            tokens.add(currentPair)
                        } else if (lastType == LuaType.DOT && language.isBasePackage(lastName) && language.isBaseWord(
                                lastName,
                                name
                            )
                        ) {
                            currentPair = Pair(len, Lexer.NAME)
                            tokens.add(currentPair)
                        } else if (language.isName(name)) {
                            currentPair = Pair(len, Lexer.NAME)
                            tokens.add(currentPair)
                        } else {
                            currentPair = Pair(len, Lexer.NORMAL)
                            tokens.add(currentPair)
                        }

                        if (lastType == LuaType.ASSIGN && name == "require") {
                            language.addUserWord(lastName)
                            if (lastNameIdx >= 0) {
                                val p = tokens[lastNameIdx - 1]
                                p.setSecond(Lexer.LITERAL)
                                lastNameIdx = -1
                            }
                        }
                        lastNameIdx = tokens.size
                        lastName = name
                    }

                    LuaType.SHORT_COMMENT, LuaType.BLOCK_COMMENT, LuaType.DOC_COMMENT -> {
                        currentPair = Pair(len, Lexer.DOUBLE_SYMBOL_LINE)
                        tokens.add(currentPair)
                    }

                    LuaType.NUMBER -> {
                        currentPair = Pair(len, Lexer.LITERAL)
                        tokens.add(currentPair)
                    }

                    else -> {
                        currentPair = Pair(len, Lexer.NORMAL)
                        tokens.add(currentPair)
                    }
                }

                if (type != LuaType.WHITE_SPACE) {
                    lastType = type
                }
                lastType2 = type
                if (true) lastPair = currentPair
                idx += len
            }
            
            // 清理资源
            lineStacks.clear()
            lineStacks2.clear()
            bul.setLength(0)
            
        } catch (e: Exception) {
            e.printStackTrace()
            TextWarriorException.fail(e.message ?: "Unknown error")
        }
        
        if (tokens.isEmpty()) {
            tokens.add(Pair(0, Lexer.NORMAL))
        }
        
        language.updateUserWord()
        Lexer.mLines = lines
        return tokens
    }

    companion object {
        private var _theOne: LuaTokenizer? = null

        fun getInstance(): LuaTokenizer {
            if (_theOne == null) {
                _theOne = LuaTokenizer()
            }
            return _theOne!!
        }
    }
}
