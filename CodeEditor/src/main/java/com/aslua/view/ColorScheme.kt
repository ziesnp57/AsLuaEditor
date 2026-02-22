package com.aslua.view

import com.aslua.Lexer
import com.aslua.TextWarriorException.Companion.fail
import java.util.HashMap

abstract class ColorScheme {
    enum class Colorable {
        FOREGROUND,
        BACKGROUND,
        SELECTION_FOREGROUND,
        SELECTION_BACKGROUND,
        CARET_FOREGROUND,
        CARET_BACKGROUND,
        CARET_DISABLED,
        LINE_HIGHLIGHT,
        NON_PRINTING_GLYPH,
        COMMENT,
        KEYWORD,
        NAME,
        LITERAL,
        STRING,
        SECONDARY
    }

    protected var colors: HashMap<Colorable?, Int?> = generateDefaultColors()

    fun setColor(colorable: Colorable?, color: Int) {
        colors.put(colorable, color)
    }

    fun getColor(colorable: Colorable?): Int {
        val color = colors.get(colorable)
        if (color == null) {
            fail("Color not specified for $colorable")
            return 0
        }
        return color
    }

    // Currently, color scheme is tightly coupled with semantics of the token types
    fun getTokenColor(tokenType: Int): Int {
        val element = when (tokenType) {
            Lexer.NORMAL -> Colorable.FOREGROUND
            Lexer.KEYWORD -> Colorable.KEYWORD
            Lexer.NAME -> Colorable.NAME
            Lexer.DOUBLE_SYMBOL_LINE, Lexer.DOUBLE_SYMBOL_DELIMITED_MULTILINE, Lexer.SINGLE_SYMBOL_LINE_B -> Colorable.COMMENT
            Lexer.SINGLE_SYMBOL_DELIMITED_A, Lexer.SINGLE_SYMBOL_DELIMITED_B, Lexer.NUMBER -> Colorable.STRING
            Lexer.LITERAL -> Colorable.LITERAL
            Lexer.SINGLE_SYMBOL_LINE_A, Lexer.SINGLE_SYMBOL_WORD, Lexer.OPERATOR -> Colorable.SECONDARY
            else -> {
                fail("Invalid token type")
                Colorable.FOREGROUND
            }
        }
        return getColor(element)
    }


    private fun generateDefaultColors(): HashMap<Colorable?, Int?> {
        // High-contrast, black-on-white color scheme
        val colors = HashMap<Colorable?, Int?>(Colorable.entries.size)
        colors.put(Colorable.FOREGROUND, BLACK)
        colors.put(Colorable.BACKGROUND, WHITE)
        colors.put(Colorable.SELECTION_FOREGROUND, WHITE)
        colors.put(Colorable.SELECTION_BACKGROUND, -0x683fdc)
        colors.put(Colorable.CARET_FOREGROUND, WHITE)
        colors.put(Colorable.CARET_BACKGROUND, LIGHT_BLUE2)
        colors.put(Colorable.CARET_DISABLED, GREY)
        colors.put(Colorable.LINE_HIGHLIGHT, 0x20888888)

        colors.put(Colorable.NON_PRINTING_GLYPH, GREY)
        colors.put(Colorable.COMMENT, OLIVE_GREEN) //  Eclipse default color
        colors.put(Colorable.KEYWORD, DARK_BLUE) // Eclipse default color
        colors.put(Colorable.NAME, INDIGO) // Eclipse default color
        colors.put(Colorable.LITERAL, LIGHT_BLUE) // Eclipse default color
        colors.put(Colorable.STRING, PURPLE)
        colors.put(Colorable.SECONDARY, GREY)
        return colors
    }

    companion object {
        // In ARGB format: 0xAARRGGBB
        private const val BLACK = -0x10000000
        private const val BLUE = -0xffff01
        private const val DARK_RED = -0x750000
        private const val DARK_BLUE = -0x2fbf23
        private const val GREY = -0x7f7f80
        private const val LIGHT_GREY = -0x555556
        private const val MAROON = -0x800000
        private const val INDIGO = -0xd5bf01
        private const val OLIVE_GREEN = -0xc080a1
        private const val PURPLE = -0x22bb78
        private const val RED = 0x44FF0000
        private const val WHITE = -0x12
        private const val PURPLE2 = -0xff01
        private const val LIGHT_BLUE = -0x9f7f01
        private const val LIGHT_BLUE2 = -0xbf4f01
        private const val GREEN = -0x775578
    }
}
