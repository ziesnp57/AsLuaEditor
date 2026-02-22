package com.aslua.language.lua

import com.aslua.language.DefFormatter
import java.io.IOException
import java.lang.StringBuilder
import java.util.Arrays
import kotlin.math.max

class LuaFormatter : DefFormatter() {
    override fun createAutoIndent(text: CharSequence?): Int {
        val lexer = LuaLexer(text)
        var idt = 0
        try {
            while (true) {
                val type = lexer.advance()
                if (type == null) break
                idt += indentAuto(type)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return idt
    }

    private fun indentAuto(t: LuaType): Int {
        return when (t) {
            LuaType.DO, LuaType.FUNCTION, LuaType.THEN, LuaType.REPEAT, LuaType.LCURLY, LuaType.ELSE, LuaType.ELSEIF -> 1
            LuaType.UNTIL, LuaType.RETURN ->  //case RCURLY:
                -1

            else -> 0
        }
    }


    private fun indent(t: LuaType): Int {
        return when (t) {
            LuaType.DO, LuaType.FUNCTION, LuaType.THEN, LuaType.REPEAT, LuaType.LCURLY, LuaType.ELSE ->  //case ELSEIF:
                1

            LuaType.UNTIL, LuaType.END, LuaType.RCURLY -> -1
            else -> 0
        }
    }

    override fun format(text: CharSequence?, width: Int): CharSequence {
        val builder = StringBuilder()
        var isNewLine = true
        val lexer = LuaLexer(text)
        try {
            var idt: Int = 0

            while (true) {
                val type = lexer.advance()
                if (type == null) break

                if (type == LuaType.NEW_LINE) {
                    if (builder.isNotEmpty() && builder.get(builder.length - 1) == ' ') builder.deleteCharAt(
                        builder.length - 1
                    )
                    isNewLine = true
                    builder.append('\n')
                    idt = max(0.0, idt.toDouble()).toInt()
                } else if (isNewLine) {
                    when (type) {
                        LuaType.WHITE_SPACE -> {}
                        LuaType.ELSE, LuaType.ELSEIF, LuaType.CASE, LuaType.DEFAULT -> {
                            //idt--;
                            builder.append(createIndent(idt * width - width / 2))
                            builder.append(lexer.yytext())
                            //idt++;
                            isNewLine = false
                        }

                        LuaType.DOUBLE_COLON, LuaType.AT -> {
                            builder.append(lexer.yytext())
                            isNewLine = false
                        }

                        LuaType.END, LuaType.UNTIL, LuaType.RCURLY -> {
                            idt--
                            builder.append(createIndent(idt * width))
                            builder.append(lexer.yytext())
                            isNewLine = false
                        }

                        else -> {
                            builder.append(createIndent(idt * width))
                            builder.append(lexer.yytext())
                            idt += indent(type)
                            isNewLine = false
                        }
                    }
                } else if (type == LuaType.WHITE_SPACE) {
                    builder.append(' ')
                } else {
                    builder.append(lexer.yytext())
                    idt += indent(type)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return builder
    }

    companion object {
        private var _theOne: LuaFormatter? = null

        @JvmStatic
		fun getInstance(): LuaFormatter {
            if (_theOne == null) {
                _theOne = LuaFormatter()
            }
            return _theOne!!
        }


        private fun createIndent(n: Int): CharArray? {
            if (n < 0) return CharArray(0)
            val idts = CharArray(n)
            Arrays.fill(idts, indentChar)
            return idts
        }
    }
}
