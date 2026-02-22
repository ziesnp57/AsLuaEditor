package com.aslua.view

import android.view.animation.AnimationUtils
import android.widget.OverScroller
import kotlin.math.max
import kotlin.math.min

/**
 * 文本编辑器滚动控制器
 * 负责处理编辑器的滚动行为
 */
class EditorScroller(private val _textField: FreeScrollingTextField) {
    private val scroller: OverScroller = OverScroller(_textField.context)
    private var lastScrollTime: Long = 0

    /**
     * 更新编辑器的滚动位置
     */
    fun updateEditorScroll() {
        if (scroller.computeScrollOffset()) {
            val oldX = _textField.scrollX
            val oldY = _textField.scrollY
            var x = scroller.currX
            var y = scroller.currY

            if (oldX != x || oldY != y) {
                // 限制滚动范围
                x = min(max(0.0, x.toDouble()), _textField.maxScrollX.toDouble()).toInt()
                y = min(max(0.0, y.toDouble()), _textField.maxScrollY.toDouble()).toInt()

                _textField.scrollTo(x, y)
                _textField.postInvalidateOnAnimation()
            }
        }
    }

    /**
     * 平滑滚动
     */
    fun smoothScrollBy(dx: Int, dy: Int) {
        if (_textField.height == 0) {
            return
        }

        val currentTime = AnimationUtils.currentAnimationTimeMillis()
        val duration = currentTime - lastScrollTime

        if (duration > 250) {
            val scrollY = _textField.scrollY
            val scrollX = _textField.scrollX

            scroller.startScroll(scrollX, scrollY, dx, dy)
            _textField.postInvalidate()
        } else {
            if (!scroller.isFinished) {
                scroller.abortAnimation()
            }
            _textField.scrollBy(dx, dy)
        }

        lastScrollTime = currentTime
    }

    /**
     * 平滑滚动到指定位置
     */
    fun smoothScrollTo(x: Int, y: Int) {
        smoothScrollBy(x - _textField.scrollX, y - _textField.scrollY)
    }

    /**
     * 开始惯性滑动
     */
    fun fling(velocityX: Int, velocityY: Int) {
        scroller.fling(
            _textField.scrollX, _textField.scrollY,
            velocityX, velocityY,
            0, _textField.maxScrollX,
            0, _textField.maxScrollY,  // 添加过度滚动效果
            _textField.width / 4, _textField.height / 4
        )
        _textField.postInvalidate()
    }

    fun isFinished(): Boolean {
        return !scroller.isFinished
    }

    fun forceFinished() {
        scroller.forceFinished(true)
    }

    fun isOverScrolled(): Boolean {
        return scroller.isOverScrolled
    }
}
