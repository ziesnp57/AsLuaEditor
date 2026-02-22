package com.aslua

import java.util.LinkedList

/**
 * 实现文本缓冲区的插入和删除事件的撤销/重做功能
 *
 * 该类与 TextBuffer 的实现紧密耦合，特别是为了优化撤销/重做效率的间隙数据结构的内部工作。
 *
 * 当文本被插入/删除时...
 * 1. 在文本插入/删除之前，TextBuffer 调用 captureInsert()/captureDelete()。
 * 2. 如果插入/删除是前一个编辑的延续，
 * 则新的编辑会与撤销堆栈的顶部条目合并。
 * 要认为两个编辑是连续的，它们必须是相同类型（插入或删除），
 * 在预定义的时间间隔 MERGE_TIME 内发生，并且后续编辑的开始位置
 * 必须与先前编辑后的光标位置一致。
 * 3. 如果新的编辑与前一个编辑不连续，则为其推送新的条目到堆栈中。
 *
 * 批量模式：
 * 客户端应用程序可以将连续的插入/删除操作作为一个组进行撤销/重做。
 * 在调用 beginBatchEdit() 和结束调用 endBatchEdit() 之间所做的编辑将作为一个单元进行分组。
 *
 * 撤销/重做：
 * 撤销/重做命令仅仅移动堆栈指针，并不会删除或插入条目。
 * 只有在进行新的编辑时，堆栈指针之后的条目才会被删除。
 *
 */
class UndoStack(buf: TextBuffer) {
    private val _buf: TextBuffer = buf
    private val stack = LinkedList<Command>()
    private var isBatchEdit = false

    /** 用于分组批处理操作  */
    private var groupId = 0

    /** 新条目应该放置的位置  */
    private var top = 0

    /** 上一个编辑操作的时间戳  */
    var lastEditTime: Long = -1

    /**
     * 撤销上一个插入/删除操作
     *
     * @return 撤销后光标的建议位置，如果没有可以撤销的操作则返回 -1
     */
    fun undo(): Int {
        if (canUndo()) {
            var lastUndone = stack[top - 1]
            val group = lastUndone.group
            do {
                val c = stack[top - 1]
                if (c.group != group) {
                    break
                }

                lastUndone = c
                c.undo()
                --top
            } while (canUndo())

            return lastUndone.findUndoPosition()
        }

        return -1
    }

    /**
     * 重做上一个插入/删除操作
     *
     * @return 重做后光标的建议位置，如果没有可以重做的操作则返回 -1
     */
    fun redo(): Int {
        if (canRedo()) {
            var lastRedone = stack[top]
            val group = lastRedone.group
            do {
                val c = stack[top]
                if (c.group != group) {
                    break
                }

                lastRedone = c
                c.redo()
                ++top
            } while (canRedo())

            return lastRedone.findRedoPosition()
        }

        return -1
    }

    /**
     * 记录插入操作。应在实际插入之前调用。
     */
    fun captureInsert(start: Int, length: Int, time: Long) {
        var mergeSuccess = false

        if (canUndo()) {
            val c = stack[top - 1]

            if (c is InsertCommand
                && c.merge(start, length, time)
            ) {
                mergeSuccess = true
            } else {
                c.recordData()
            }
        }

        if (!mergeSuccess) {
            push(InsertCommand(start, length, groupId))

            if (!isBatchEdit) {
                groupId++
            }
        }

        lastEditTime = time
    }

    /**
     * 记录删除操作。应在实际删除之前调用。
     */
    fun captureDelete(start: Int, length: Int, time: Long) {
        var mergeSuccess = false

        if (canUndo()) {
            val c = stack[top - 1]

            if (c is DeleteCommand
                && c.merge(start, length, time)
            ) {
                mergeSuccess = true
            } else {
                c.recordData()
            }
        }

        if (!mergeSuccess) {
            push(DeleteCommand(start, length, groupId))

            if (!isBatchEdit) {
                groupId++
            }
        }

        lastEditTime = time
    }

    private fun push(c: Command?) {
        trimStack()
        ++top
        stack.add(c!!)
    }

    private fun trimStack() {
        while (stack.size > top) {
            stack.removeLast()
        }
    }

    fun canUndo(): Boolean {
        return top > 0
    }

    fun canRedo(): Boolean {
        return top < stack.size
    }

    fun isBatchEdit(): Boolean {
        return isBatchEdit
    }

    fun beginBatchEdit() {
        isBatchEdit = true
    }

    fun endBatchEdit() {
        isBatchEdit = false
        groupId++
    }

    companion object {
        const val MERGE_TIME = 1000000000
    }

    private abstract inner class Command {
        /** 编辑的起始位置  */
        var start: Int = 0

        /** 受影响段的长度  */
        var length: Int = 0

        /** 受影响段的内容  */
        var data: String? = null

        /** 组 ID。相同组的命令作为一个单位撤销/重做  */
        var group: Int = 0

        abstract fun undo()
        abstract fun redo()

        /** 用受影响文本填充 data  */
        abstract fun recordData()
        abstract fun findUndoPosition(): Int
        abstract fun findRedoPosition(): Int

        /**
         * 尝试合并编辑。只有在新编辑是连续的情况下才会成功。
         * 详细要求见 [UndoStack] 中关于连续编辑的要求。
         *
         * @param start 新编辑的起始位置
         * @param length 新编辑段的长度
         * @param time 新编辑的时间戳。对使用的单位没有限制，
         * 只要在整个程序中保持一致即可。
         *
         * @return 合并是否成功
         */
        abstract fun merge(start: Int, length: Int, time: Long): Boolean


    }

    private inner class InsertCommand(starts: Int, lengths: Int, groupNumber: Int) : Command() {
        /**
         * 对应于在起始位置之前插入长度为 length 的文本。
         */
        init {
            start = starts
            length = lengths
            group = groupNumber
        }

        override fun merge(newStart: Int, lengths: Int, time: Long): Boolean {
            if (lastEditTime < 0) {
                return false
            }

            if ((time - lastEditTime) < MERGE_TIME
                && newStart == start + length
            ) {
                length += lengths
                trimStack()
                return true
            }

            return false
        }

        override fun recordData() {
            //TODO handle memory allocation failure
            data = _buf.subSequence(start, length).toString()
        }

        override fun undo() {
            if (data == null) {
                recordData()
                _buf.shiftGapStart(-length)
            } else {
                //dummy timestamp of 0
                _buf.delete(start, length, 0, false)
            }
        }

        override fun redo() {
            //dummy timestamp of 0
            _buf.insert(data!!.toCharArray(), start, 0, false)
        }

        override fun findRedoPosition(): Int {
            return start + length
        }

        override fun findUndoPosition(): Int {
            return start
        }
    }


    private inner class DeleteCommand(starts: Int, lengths: Int, seqNumber: Int) : Command() {
        /**
         * 对应于从起始位置开始，长度为 length 的文本删除操作。
         */
        init {
            start = starts
            length = lengths
            group = seqNumber
        }

        override fun merge(newStart: Int, lengths: Int, time: Long): Boolean {
            if (lastEditTime < 0) {
                return false
            }

            if ((time - lastEditTime) < MERGE_TIME
                && newStart == start - length - lengths + 1
            ) {
                start = newStart
                length += lengths
                trimStack()
                return true
            }

            return false
        }

        override fun recordData() {
            //TODO handle memory allocation failure
            data = String(_buf.gapSubSequence(length))
        }

        override fun undo() {
            if (data == null) {
                recordData()
                _buf.shiftGapStart(length)
            } else {
                // 虚拟时间戳 0
                _buf.insert(data!!.toCharArray(), start, 0, false)
            }
        }

        override fun redo() {
            _buf.delete(start, length, 0, false)
        }

        override fun findRedoPosition(): Int {
            return start
        }

        override fun findUndoPosition(): Int {
            return start + length
        }
    }
}
