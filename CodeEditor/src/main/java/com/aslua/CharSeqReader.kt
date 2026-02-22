package com.aslua

import java.io.IOException
import java.io.Reader
import java.lang.Exception
import kotlin.math.min

/**
 * 字符序列阅读器
 * 用于将CharSequence转换为可读取的字符流
 * @param src 源字符序列
 */
class CharSeqReader(src: CharSequence?) : Reader() {
    /**
     * 当前读取位置的偏移量
     */
    var offset: Int = 0
    /**
     * 源字符序列
     */
    var src: CharSequence?

    init {
        this.src = src
    }
    /**
     * 关闭阅读器，释放资源
     */
    override fun close() {
        src = null
        offset = 0
    }

    /**
     * 从字符序列中读取字符到指定数组
     * @param chars 目标字符数组
     * @param offset 写入起始位置
     * @param length 要读取的最大字符数
     * @return 实际读取的字符数，如果到达末尾返回-1
     * @throws IOException 读取过程中发生IO异常
     */
    @Throws(IOException::class)
    override fun read(chars: CharArray, i: Int, i1: Int): Int {
        var i = i
        val len: Int = min((src!!.length - offset).toDouble(), i1.toDouble()).toInt()
        for (n in 0 until len) {
            try {
                val c = src!![offset++]
                chars[i++] = c
            } catch (_: Exception) {
            }
        }
        if (len <= 0) return -1
        return len
    }
}
