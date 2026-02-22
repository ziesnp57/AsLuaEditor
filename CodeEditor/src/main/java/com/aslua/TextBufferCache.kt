package com.aslua

/**
 * 一个 LRU 缓存，用于存储最近访问的行及其对应的索引，
 * 这样未来的查找可以从缓存的位置开始，而不是从文件的开头开始。
 *
 * _cache.Pair.First = 行索引
 * _cache.Pair.Second = 该行中第一个字符的字符偏移
 *
 * TextBufferCache 始终有一个有效条目 (0,0)，表示在第 0 行中，
 * 第一个字符的偏移量为 0。即使对于一个“空”文件，这也是真的，
 * 因为 TextBuffer 在其中插入了 EOF 字符。
 *
 * 因此，_cache[0] 始终被条目 (0,0) 占用。它不受 invalidateCache、
 * 缓存未命中等操作的影响。
 */
class TextBufferCache {
    private val _cache = arrayOfNulls<Pair>(CACHE_SIZE)

    init {
        _cache[0] = Pair(0, 0) // 不变的行索引和字符偏移关系
        for (i in 1 until CACHE_SIZE) {
            _cache[i] = Pair(-1, -1)
        }
    }

    fun getNearestLine(lineIndex: Int): Pair? {
        var nearestMatch = 0
        var nearestDistance = Int.Companion.MAX_VALUE
        for (i in 0 until CACHE_SIZE) {
            val distance = (lineIndex - _cache[i]!!.first)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestMatch = i
            }
        }

        val nearestEntry = _cache[nearestMatch]
        makeHead(nearestMatch)
        return nearestEntry
    }

    fun getNearestCharOffset(charOffset: Int): Pair? {
        var nearestMatch = 0
        var nearestDistance = Int.Companion.MAX_VALUE
        for (i in 0 until CACHE_SIZE) {
            val distance: Int = (charOffset - _cache[i]!!.second)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestMatch = i
            }
        }

        val nearestEntry = _cache[nearestMatch]
        makeHead(nearestMatch)
        return nearestEntry
    }

    /**
     * 将 _cache[newHead] 放到列表的顶部
     */
    private fun makeHead(newHead: Int) {
        if (newHead == 0) {
            return
        }

        val temp = _cache[newHead]
        for (i in newHead downTo 2) {
            _cache[i] = _cache[i - 1]
        }
        _cache[1] = temp // _cache[0] 始终被 (0,0) 占用
    }

    fun updateEntry(lineIndex: Int, charOffset: Int) {
        if (lineIndex <= 0) {
            return
        }

        if (!replaceEntry(lineIndex, charOffset)) {
            insertEntry(lineIndex, charOffset)
        }
    }

    private fun replaceEntry(lineIndex: Int, charOffset: Int): Boolean {
        for (i in 1 until CACHE_SIZE) {
            if (_cache[i]!!.first == lineIndex) {
                _cache[i]!!.setSecond(charOffset)
                return true
            }
        }
        return false
    }

    private fun insertEntry(lineIndex: Int, charOffset: Int) {
        makeHead(CACHE_SIZE - 1) // 右移条目列表
        // 用新条目替换头部（最近使用的条目）
        _cache[1] = Pair(lineIndex, charOffset)
    }

    /**
     * 使所有字符偏移量 >= fromCharOffset 的缓存条目无效
     */
    fun invalidateCache(fromCharOffset: Int) {
        for (i in 1 until CACHE_SIZE) {
            if (_cache[i]!!.second >= fromCharOffset) {
                _cache[i] = Pair(-1, -1)
            }
        }
    }

    companion object {
        private const val CACHE_SIZE = 4 // 最小值 = 1
    }
}