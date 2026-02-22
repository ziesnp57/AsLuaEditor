package com.aslua

import com.aslua.TextWarriorException.Companion.fail
import com.aslua.language.Language
import java.util.ArrayList

/**
 * TextBuffer的装饰器，添加自动换行功能。
 * 此类存储自动换行的断点位置。
 * 默认启用自动换行。
 */
class Document(metrics: TextFieldMetrics) : TextBuffer() {
    private var _isWordWrap = false

    /** 包含字符打印相关信息，显示大小等 */
    private var _metrics: TextFieldMetrics

    /** 存储每行起始位置的表格 */
    private var _rowTable: ArrayList<Int?>? = null

    init {
        _metrics = metrics
        resetRowTable()
    }

    /**
     * 设置文本内容
     * @param text 要设置的文本
     */
    fun setText(text: CharSequence) {
        var lineCount = 1
        val len = text.length
        val ca = CharArray(memoryNeeded(len))
        for (i in 0 until len) {
            ca[i] = text[i]
            if (text[i] == '\n') lineCount++
        }
        setBuffer(ca, len, lineCount)
    }

    /**
     * 重置行表格，初始化为只包含一行（从0开始）
     */
    private fun resetRowTable() {
        val rowTable = ArrayList<Int?>()
        rowTable.add(0) //每个文档至少包含1行
        _rowTable = rowTable
    }

    /**
     * 设置文本度量对象
     */
    fun setMetrics(metrics: TextFieldMetrics) {
        _metrics = metrics
    }

    /**
     * 启用/禁用自动换行。
     * 如果启用，文档会立即分析自动换行断点，这可能需要较长时间。
     */
    fun setWordWrap(enable: Boolean) {
        if (enable && !_isWordWrap) {
            _isWordWrap = true
            analyzeWordWrap()
        } else if (!enable && _isWordWrap) {
            _isWordWrap = false
            analyzeWordWrap()
        }
    }

    /**
     * 获取当前是否启用自动换行
     */
    fun isWordWrap(): Boolean {
        return _isWordWrap
    }

    /**
     * 删除指定位置的字符
     * @param charOffset 起始字符位置
     * @param totalChars 要删除的字符数
     * @param timestamp 时间戳
     * @param undoable 是否可撤销
     */
    @Synchronized
    override fun delete(charOffset: Int, totalChars: Int, timestamp: Long, undoable: Boolean) {
        super.delete(charOffset, totalChars, timestamp, undoable)

        val startRow = findRowNumber(charOffset)
        val analyzeEnd = findNextLineFrom(charOffset)
        updateWordWrapAfterEdit(startRow, analyzeEnd, -totalChars)
    }

    /**
     * 在指定位置插入字符
     * @param c 要插入的字符数组
     * @param charOffset 插入位置
     * @param timestamp 时间戳
     * @param undoable 是否可撤销
     */
    @Synchronized
    override fun insert(c: CharArray, charOffset: Int, timestamp: Long, undoable: Boolean) {
        super.insert(c, charOffset, timestamp, undoable)

        val startRow = findRowNumber(charOffset)
        val analyzeEnd = findNextLineFrom(charOffset + c.size)
        updateWordWrapAfterEdit(startRow, analyzeEnd, c.size)
    }

    /**
     * 移动间隙起始位置
     * 移动_gapStartIndex指定的单位。注意displacement可以为负数，会向左移动_gapStartIndex。
     * 只有UndoStack应该使用此方法来执行简单的插入/删除的撤销/重做。
     * 不进行错误检查。
     */
    @Synchronized
    override fun shiftGapStart(displacement: Int) {
        super.shiftGapStart(displacement)

        if (displacement != 0) {
            val startOffset = if ((displacement > 0))
                _gapStartIndex - displacement
            else
                _gapStartIndex
            val startRow = findRowNumber(startOffset)
            val analyzeEnd = findNextLineFrom(_gapStartIndex)
            updateWordWrapAfterEdit(startRow, analyzeEnd, displacement)
        }
    }

    /**
     * 从指定字符位置查找下一行
     * 不进行参数错误检查
     */
    private fun findNextLineFrom(charOffset: Int): Int {
        var lineEnd = logicalToRealIndex(charOffset)

        while (lineEnd < _contents.size) {
            // 跳过间隙
            if (lineEnd == _gapStartIndex) {
                lineEnd = _gapEndIndex
            }

            if (_contents[lineEnd] == Language.NEWLINE ||
                _contents[lineEnd] == Language.EOF
            ) {
                break
            }

            ++lineEnd
        }

        return realToLogicalIndex(lineEnd) + 1
    }

    /**
     * 编辑后更新自动换行信息
     * @param startRow 起始行
     * @param analyzeEnd 分析结束位置
     * @param delta 字符变化量
     */
    private fun updateWordWrapAfterEdit(startRow: Int, analyzeEnd: Int, delta: Int) {
        var startRow = startRow
        if (startRow > 0) {
            // 如果第一个词变短或插入的空格将其分开，
            // 它可能适合前一行，所以也要分析那一行
            --startRow
        }
        val analyzeStart: Int = _rowTable!![startRow]!!

        //变化只影响startRow之后的行
        removeRowMetadata(startRow + 1, analyzeEnd - delta)
        adjustOffsetOfRowsFrom(startRow + 1, delta)
        analyzeWordWrap(startRow + 1, analyzeStart, analyzeEnd)
    }

    /**
     * 移除从fromRow到endOffset所在行的行偏移信息（包含这些行）
     * 不进行参数错误检查
     */
    private fun removeRowMetadata(fromRow: Int, endOffset: Int) {
        while (fromRow < _rowTable!!.size &&
            _rowTable!![fromRow]!! <= endOffset
        ) {
            _rowTable!!.removeAt(fromRow)
        }
    }

    /**
     * 从指定行开始调整行偏移量
     */
    private fun adjustOffsetOfRowsFrom(fromRow: Int, offset: Int) {
        for (i in fromRow until _rowTable!!.size) {
            _rowTable!![i] = _rowTable!![i]!! + offset
        }
    }

    /**
     * 分析整个文档的自动换行
     */
    fun analyzeWordWrap() {
        resetRowTable()

        if (_isWordWrap && !hasMinimumWidthForWordWrap()) {
            if (_metrics.getRowWidth() > 0) {
                fail("文本区域宽度非零但仍然太小，无法进行自动换行")
            }
            // _metrics.getRowWidth()在文本区域尚未布局时可能合法地为零
            return
        }

        analyzeWordWrap(1, 0, textLength)
    }

    /**
     * 检查是否有足够的宽度进行自动换行
     */
    private fun hasMinimumWidthForWordWrap(): Boolean {
        val maxWidth = _metrics.getRowWidth()
        //假设最宽的字符是2ems宽
        return (maxWidth >= 2 * _metrics.getAdvance('M'))
    }

    /**
     * 分析指定范围的自动换行
     * @param rowIndex 行索引
     * @param startOffset 起始偏移量
     * @param endOffset 结束偏移量
     */
    private fun analyzeWordWrap(rowIndex: Int, startOffset: Int, endOffset: Int) {
        if (!_isWordWrap) {
            var offset = logicalToRealIndex(startOffset)
            val end = logicalToRealIndex(endOffset)
            val rowTable = ArrayList<Int?>()

            while (offset < end) {
                // 跳过间隙
                if (offset == _gapStartIndex) {
                    offset = _gapEndIndex
                }
                val c = _contents[offset]
                if (c == Language.NEWLINE) {
                    rowTable.add(realToLogicalIndex(offset) + 1)
                }
                ++offset
            }
            _rowTable!!.addAll(rowIndex, rowTable)
            return
        }
        if (!hasMinimumWidthForWordWrap()) {
            fail("没有足够的空间进行自动换行")
            return
        }

        val rowTable = ArrayList<Int?>()
        var offset = logicalToRealIndex(startOffset)
        val end = logicalToRealIndex(endOffset)
        var potentialBreakPoint = startOffset
        var wordExtent = 0
        val maxWidth = _metrics.getRowWidth()
        var remainingWidth = maxWidth

        while (offset < end) {
            // 跳过间隙
            if (offset == _gapStartIndex) {
                offset = _gapEndIndex
            }

            val c = _contents[offset]
            wordExtent += _metrics.getAdvance(c)

            val isWhitespace =
                (c == ' ' || c == Language.TAB || c == Language.NEWLINE || c == Language.EOF)

            if (isWhitespace) {
                //获得完整单词
                if (wordExtent <= remainingWidth) {
                    remainingWidth -= wordExtent
                } else if (wordExtent > maxWidth) {
                    //处理一个太长而无法放在一行的单词
                    var current = logicalToRealIndex(potentialBreakPoint)
                    remainingWidth = maxWidth

                    //在新行开始这个单词，如果它还没有开始
                    if (potentialBreakPoint != startOffset && (rowTable.isEmpty() ||
                                potentialBreakPoint != rowTable[rowTable.size - 1])
                    ) {
                        rowTable.add(potentialBreakPoint)
                    }

                    while (current <= offset) {
                        // 跳过间隙
                        if (current == _gapStartIndex) {
                            current = _gapEndIndex
                        }

                        val advance = _metrics.getAdvance(_contents[current])
                        if (advance > remainingWidth) {
                            rowTable.add(realToLogicalIndex(current))
                            remainingWidth = maxWidth - advance
                        } else {
                            remainingWidth -= advance
                        }

                        ++current
                    }
                } else {
                    //不变量：potentialBreakPoint != startOffset
                    //在新行放置单词
                    rowTable.add(potentialBreakPoint)
                    remainingWidth = maxWidth - wordExtent
                }

                wordExtent = 0
                potentialBreakPoint = realToLogicalIndex(offset) + 1
            }

            if (c == Language.NEWLINE) {
                //开始新行
                rowTable.add(potentialBreakPoint)
                remainingWidth = maxWidth
            }

            ++offset
        }

        //与现有行表合并
        _rowTable!!.addAll(rowIndex, rowTable)
    }

    /**
     * 获取指定行号的文本内容
     */
    fun getRow(rowNumber: Int): String {
        val rowSize = getRowSize(rowNumber)
        if (rowSize == 0) {
            return ""
        }

        val startIndex: Int = _rowTable!![rowNumber]!!
        return subSequence(startIndex, rowSize).toString()
    }

    /**
     * 获取指定行的大小（字符数）
     */
    fun getRowSize(rowNumber: Int): Int {
        if (isInvalidRow(rowNumber)) {
            return 0
        }

        return if (rowNumber != (_rowTable!!.size - 1)) {
            _rowTable!![rowNumber + 1]!!.toInt() - (_rowTable!![rowNumber]!!.toInt())
        } else {
            //最后一行
            textLength.toInt() - _rowTable!![rowNumber]!!.toInt()
        }
    }

    /**
     * 获取总行数
     */
    fun getRowCount(): Int {
        return _rowTable!!.size
    }

    /**
     * 获取指定行的起始偏移量
     */
    fun getRowOffset(rowNumber: Int): Int {
        if (isInvalidRow(rowNumber)) {
            return -1
        }

        return _rowTable!![rowNumber]!!
    }

    /**
     * 获取字符偏移量所在的行号
     * @param charOffset 字符偏移量
     * @return 字符偏移量所在的行号，如果偏移量无效则返回-1
     */
    fun findRowNumber(charOffset: Int): Int {
        if (!isValid(charOffset)) {
            return -1
        }

        //对_rowTable进行二分查找
        var right = _rowTable!!.size - 1
        var left = 0
        while (right >= left) {
            val mid = (left + right) / 2
            val nextLineOffset: Int =
                (if (((mid + 1) < _rowTable!!.size)) _rowTable!![mid + 1] else textLength)!!
            if (charOffset.toInt() >= _rowTable!![mid]!!.toInt() && charOffset < nextLineOffset) {
                return mid
            }

            if (charOffset >= nextLineOffset) {
                left = mid + 1
            } else {
                right = mid - 1
            }
        }

        return -1
    }

    /**
     * 检查行号是否无效
     */
    private fun isInvalidRow(rowNumber: Int): Boolean {
        return rowNumber < 0 || rowNumber >= _rowTable!!.size
    }

    /**
     * 文本区域度量接口
     */
    interface TextFieldMetrics {
        /**
         * 获取字符的宽度
         */
        fun getAdvance(c: Char): Int

        /**
         * 获取行宽度
         */
        fun getRowWidth(): Int
    }
}
