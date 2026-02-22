package com.aslua.language.lua

import com.aslua.language.Language

/**
 * Singleton class containing the symbols and operators of the Lua language.
 */
class LuaLanguage private constructor() : Language() {
    init {
        super.setOperators(LUA_OPERATORS)
        super.setKeywords(Companion.keywords)
        super.setNames(Companion.names)
        addBasePackage(
            "io",
            package_io.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "string",
            package_string.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "luakt",
            package_luakt.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "os",
            package_os.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "table",
            package_table.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "math",
            package_math.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "utf8",
            package_utf8.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "coroutine",
            package_coroutine.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "package",
            package_package.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "debug",
            package_debug.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "bit",
            package_bit.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "ffi",
            package_ffi.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
        addBasePackage(
            "jit",
            package_jit.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
    }

    override fun getTokenizer(): LuaTokenizer? {
        return LuaTokenizer.getInstance()
    }

    override fun getFormatter(): LuaFormatter {
        return LuaFormatter.getInstance()
    }

    override fun isLineAStart(c: Char): Boolean {
        return false
    }

    override fun isLineStart(c0: Char, c1: Char): Boolean {
        return (c0 == '-' && c1 == '-')
    }

    override fun isMultilineStartDelimiter(c0: Char, c1: Char): Boolean {
        return (c0 == '[' && c1 == '[')
    }

    override fun isMultilineEndDelimiter(c0: Char, c1: Char): Boolean {
        return (c0 == ']' && c1 == ']')
    }

    companion object {
        private var _theOne: LuaLanguage? = null

        private const val keywordTarget =
            "and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while"
        private const val globalTarget =
            "__add|__band|__bnot|__bor|__bxor|__call|__concat|__div|__eq|__idiv|__index|__le|__len|__lt|__mod|__mul|__newindex|__pow|__shl|__shr|__sub|__unm|_ENV|_G|assert|collectgarbage|dofile|error|findtable|getmetatable|ipairs|load|loadfile|loadstring|module|next|pairs|pcall|print|float|rawequal|rawget|rawlen|rawset|require|select|self|setmetatable|tointeger|tonumber|tostring|type|unpack|xpcall"

        private const val packageName =
            "coroutine|debug|io|luakt|math|os|package|string|table|utf8|jit|bit|ffi"
        private const val package_coroutine = "create|isyieldable|resume|running|status|wrap|yield"
        private const val package_debug =
            "debug|gethook|getinfo|getlocal|getmetatable|getregistry|getupvalue|getuservalue|sethook|setlocal|setmetatable|setupvalue|setuservalue|traceback|upvalueid|upvaluejoin"
        private const val package_io =
            "close|flush|input|lines|open|output|popen|read|stderr|stdin|stdout|tmpfile|type|write"
        private const val package_luakt =
            "astable|bindClass|clear|coding|createArray|createProxy|instanceof|loadLib|loaded|luapath|new|newInstance|package|tostring"
        private const val package_math =
            "abs|acos|asin|atan|atan2|ceil|cos|cosh|deg|exp|floor|fmod|frexp|huge|ldexp|log|log10|max|maxinteger|min|mininteger|modf|pi|pow|rad|round|random|randomseed|sin|sinh|sqrt|tan|tanh|tointeger|type|ult"
        private const val package_os =
            "clock|date|difftime|execute|exit|getenv|remove|rename|setlocale|time|tmpname"
        private const val package_package =
            "config|cpath|loaded|loaders|loadlib|path|preload|searchers|searchpath|seeall"
        private const val package_string =
            "byte|char|dump|find|format|gfind|gmatch|gsub|len|lower|match|pack|packsize|rep|reverse|sub|unpack|upper"
        private const val package_table =
            "concat|foreach|foreachi|insert|maxn|move|pack|remove|sort|unpack"
        private const val package_utf8 = "char|charpattern|codepoint|codes|len|offset"
        private const val package_bit = "bor|band|bxor|bnot|lshift|rshift|arshift|rol|ror|bswap"
        private const val package_ffi = "abi|arch|cast|cdef|copy|errno|fill|gc|istype|load|metatype|new|os|sizeof|string|typeof|typeof"
        private const val package_jit = "arch|bc|dump|flush|log|off|on|opt|status|version"

        private const val extFunctionTarget =
            "this|activity|call|compile|dump|each|enum|import|loadbitmap|loadlayout|loadmenu|service|set|task|thread|timer"
        private const val functionTarget = "$globalTarget|$extFunctionTarget|$packageName"

        private val keywords: Array<String?> =
            keywordTarget.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        private val names: Array<String?> =
            functionTarget.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        private val LUA_OPERATORS = charArrayOf(
            '(', ')', '{', '}', ',', ';', '=', '+', '-',
            '/', '*', '&', '!', '|', ':', '[', ']', '<', '>',
            '?', '~', '%', '^'
        )

        @JvmStatic
        fun getInstance(): Language {
            return _theOne ?: LuaLanguage().also { _theOne = it }
        }
    }
}
