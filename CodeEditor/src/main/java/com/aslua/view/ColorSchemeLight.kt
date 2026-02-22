package com.aslua.view

class ColorSchemeLight : ColorScheme() {
    init {
        //文字
        setColor(Colorable.FOREGROUND, OFF_BLACK)
        //背景
        setColor(Colorable.BACKGROUND, OFF_WHITE)
        //选取文字
        setColor(Colorable.SELECTION_FOREGROUND, OFF_WHITE)
        //选取背景
        setColor(Colorable.SELECTION_BACKGROUND, -0x666667)
        //关键字
        setColor(Colorable.KEYWORD, BLUE_DARK)
        //函数名
        setColor(Colorable.LITERAL, BLUE_LIGHT)
        //字符串、数字
        setColor(Colorable.STRING, -0x55de00)
        //次关键字
        setColor(Colorable.NAME, -0xd5bf01)
        //符号
        setColor(Colorable.SECONDARY, BLUE_DARK)
        //光标
        setColor(Colorable.CARET_DISABLED, GREEN_DARK)
        //yoyo？
        setColor(Colorable.CARET_FOREGROUND, OFF_WHITE)
        //yoyo背景
        setColor(Colorable.CARET_BACKGROUND, -0xd6490a)
        //当前行
        setColor(Colorable.LINE_HIGHLIGHT, 0x1E888888)

        //注释
        setColor(Colorable.COMMENT, GREEN_LIGHT)
    }

    companion object {
        private const val OFF_WHITE = -0x1
        private const val OFF_BLACK = -0xcccccd

        private const val GREEN_LIGHT = -0xff6500
        private const val GREEN_DARK = -0xc080a1
        private const val BLUE_LIGHT = -0xf06301
        private const val BLUE_DARK = -0xd37d38
    }
}
