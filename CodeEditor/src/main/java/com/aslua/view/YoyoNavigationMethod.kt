package com.aslua.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import com.aslua.Pair
import com.aslua.view.ColorScheme.Colorable
import com.aslua.view.YoyoNavigationMethod.Yoyo

class YoyoNavigationMethod(textField: FreeScrollingTextField) : TouchNavigationMethod(textField) {
    private val yoyoCaret: Yoyo // 光标对象
    private val yoyoStart: Yoyo // 开始手柄对象
    private val yoyoEnd: Yoyo // 结束手柄对象
    private var isStartHandleTouched = false // 是否触摸到开始手柄
    private var isEndHandleTouched = false // 是否触摸到结束手柄
    private var isCaretHandleTouched = false // 是否触摸到光标手柄
    private var isShowYoyoCaret  = false // 是否显示光标手柄

    private val yoyoSize: Int // 手柄的尺寸

    // 构造函数，初始化光标手柄、开始手柄和结束手柄
    init {
        val dm = textField.context.resources.displayMetrics
        yoyoSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            (FreeScrollingTextField.BASE_TEXT_SIZE_PIXELS * 1.5).toFloat(),
            dm
        ).toInt()
        yoyoCaret = Yoyo()
        yoyoStart = Yoyo()
        yoyoEnd = Yoyo()
    }

    // 触摸按下事件
    override fun onDown(e: MotionEvent): Boolean {
        super.onDown(e)
        if (!_isCaretTouched) {
            val x = e.x.toInt() + _textField.scrollX
            val y = e.y.toInt() + _textField.scrollY
            // 判断触摸位置是否在光标、开始手柄或结束手柄上
            isCaretHandleTouched  = yoyoCaret.isInHandle(x, y)
            isStartHandleTouched = yoyoStart.isInHandle(x, y)
            isEndHandleTouched = yoyoEnd.isInHandle(x, y)

            // 如果触摸在光标手柄上，开始拖动光标
            if (isCaretHandleTouched ) {
                isShowYoyoCaret  = true
                yoyoCaret.setInitialTouch(x, y)
                yoyoCaret.invalidateCurrentPosition()
            } else if (isStartHandleTouched) {
                yoyoStart.setInitialTouch(x, y)
                _textField.focusSelectionStart()
                yoyoStart.invalidateCurrentPosition()
            } else if (isEndHandleTouched) {
                yoyoEnd.setInitialTouch(x, y)
                _textField.focusSelectionEnd()
                yoyoEnd.invalidateCurrentPosition()
            }
        }

        return true
    }

    // 触摸抬起事件
    override fun onUp(e: MotionEvent?): Boolean {
        isCaretHandleTouched  = false
        isStartHandleTouched = false
        isEndHandleTouched = false
        yoyoCaret.clearTouch()
        yoyoStart.clearTouch()
        yoyoEnd.clearTouch()
        super.onUp(e)
        return true
    }

    // 滑动事件
    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float,
        distanceY: Float
    ): Boolean {
        val activeHandle = when {
            isCaretHandleTouched -> yoyoCaret
            isStartHandleTouched -> yoyoStart
            isEndHandleTouched -> yoyoEnd
            else -> return super.onScroll(e1, e2, distanceX, distanceY)
        }
        
        if ((e2.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            return onUp(e2)
        }
        
        if (activeHandle == yoyoCaret) {
            isShowYoyoCaret = true
        }
        moveHandle(activeHandle, e2)
        return true
    }

    // 移动手柄位置
    private fun moveHandle(yoyo: Yoyo, e: MotionEvent) {
        val foundIndex = yoyo.findNearestChar(e.x.toInt(), e.y.toInt())
        val newCaretIndex = foundIndex.first

        // 如果找到新的字符索引，则移动光标并更新手柄位置
        if (newCaretIndex >= 0) {
            _textField.moveCaret(newCaretIndex)
            val newCaretBounds = _textField.getBoundingBox(newCaretIndex)
            val newX = newCaretBounds.left + _textField.getPaddingLeft()
            val newY = newCaretBounds.bottom + _textField.paddingTop

            yoyo.attachYoyo(newX, newY)
        }
    }

    // 单击事件
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val x = e.x.toInt() + _textField.scrollX
        val y = e.y.toInt() + _textField.scrollY

        // 忽略点击手柄
        if (yoyoCaret.isInHandle(x, y) || yoyoStart.isInHandle(x, y) || yoyoEnd.isInHandle(
                x,
                y
            )
        ) {
            return true
        } else {
            isShowYoyoCaret  = true
            return super.onSingleTapUp(e)
        }
    }

    // 双击事件
    override fun onDoubleTap(e: MotionEvent): Boolean {
        val x = e.x.toInt() + _textField.scrollX
        val y = e.y.toInt() + _textField.scrollY

        // 忽略点击光标手柄
        if (yoyoCaret.isInHandle(x, y)) {
            _textField.selectText(true)
            return true
        } else if (yoyoStart.isInHandle(x, y)) {
            return true
        } else {
            return super.onDoubleTap(e)
        }
    }

    // 长按事件
    override fun onLongPress(e: MotionEvent) {
        // 长按处理
        onDoubleTap(e)
    }

    // 滑动速度事件
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (isCaretHandleTouched  || isStartHandleTouched || isEndHandleTouched) {
            onUp(e2)
            return true
        } else {
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    // 绘制完成后执行的操作
    override fun onTextDrawComplete(canvas: Canvas) {
        if (!_textField.isSelectText2) {
            yoyoCaret.show()
            yoyoStart.hide()
            yoyoEnd.hide()

            if (!isCaretHandleTouched ) {
                val caret = _textField.getBoundingBox(_textField.caretPosition)
                val x = caret.left + _textField.getPaddingLeft()
                val y = caret.bottom + _textField.paddingTop
                yoyoCaret.setRestingPosition(x, y)
            }
            if (isShowYoyoCaret ) yoyoCaret.draw(canvas, isCaretHandleTouched )
            isShowYoyoCaret  = false
        } else {
            yoyoCaret.hide()
            yoyoStart.show()
            yoyoEnd.show()

            if (!(isStartHandleTouched && isEndHandleTouched)) {
                val caret = _textField.getBoundingBox(_textField.selectionStart)
                val x = caret.left + _textField.getPaddingLeft()
                val y = caret.bottom + _textField.paddingTop
                yoyoStart.setRestingPosition(x, y)

                val caret2 = _textField.getBoundingBox(_textField.selectionEnd)
                val x2 = caret2.left + _textField.getPaddingLeft()
                val y2 = caret2.bottom + _textField.paddingTop
                yoyoEnd.setRestingPosition(x2, y2)
            }

            yoyoStart.draw(canvas, isStartHandleTouched)
            yoyoEnd.draw(canvas, isStartHandleTouched)
        }
    }

    // 获取光标的扩展区域
    override fun getCaretBloat(): Rect = yoyoCaret.hANDLEBLOAT

    // 色彩方案变化时更新光标色
    override fun onColorSchemeChanged(colorScheme: ColorScheme) {
        yoyoCaret.setHandleColor(colorScheme.getColor(Colorable.CARET_BACKGROUND))
    }

    // 光标类，负责光标手柄的管理
    private inner class Yoyo {
        private val hANDLERECT: Rect = Rect(0, 0, yoyoSize, yoyoSize)
        val hANDLEBLOAT: Rect
        private val sTRINGHEIGHT: Int = yoyoSize / YOYO_STRING_FACTOR
        private val handlePaint: Paint
        private val arcRect: RectF
        private val ovalRect: RectF
        private val invalidateRect: Rect
        private var anchorX = 0
        private var anchorY = 0
        private var handleX = 0
        private var handleY = 0
        private var touchOffsetX = 0
        private var touchOffsetY = 0
        private var isVisible = false

        init {
            val radius = yoyoSize / 2
            hANDLEBLOAT = Rect(radius, 0, 0, hANDLERECT.bottom + sTRINGHEIGHT)

            handlePaint = Paint()
            handlePaint.setColor(_textField.colorScheme.getColor(Colorable.CARET_BACKGROUND))
            handlePaint.isAntiAlias = true

            arcRect = RectF()
            ovalRect = RectF()
            invalidateRect = Rect()
        }

        fun setHandleColor(color: Int) {
            handlePaint.setColor(color)
        }

        fun draw(canvas: Canvas, activated: Boolean) {
            if (!isVisible) return

            val radius = yoyoSize / 2

            // 绘制手柄线
            canvas.drawLine(
                anchorX.toFloat(), anchorY.toFloat(),
                (handleX + radius).toFloat(), (handleY + radius).toFloat(), handlePaint
            )

            // 设置并绘制弧形
            arcRect.set(
                (anchorX - radius).toFloat(),
                anchorY - radius.toFloat() / 2 - sTRINGHEIGHT,
                (handleX + radius * 2).toFloat(),
                handleY + radius.toFloat() / 2
            )
            canvas.drawArc(
                arcRect,
                ARC_START_ANGLE.toFloat(),
                ARC_SWEEP_ANGLE.toFloat(),
                true,
                handlePaint
            )

            // 设置并绘制手柄椭圆
            ovalRect.set(
                handleX.toFloat(), handleY.toFloat(),
                (handleX + hANDLERECT.right).toFloat(),
                (handleY + hANDLERECT.bottom).toFloat()
            )
            canvas.drawOval(ovalRect, handlePaint)
        }

        fun attachYoyo(x: Int, y: Int) {
            invalidateCurrentPosition()
            setRestingPosition(x, y)
            invalidateCurrentPosition()
        }

        fun setRestingPosition(x: Int, y: Int) {
            anchorX = x
            anchorY = y
            handleX = x - yoyoSize / 2
            handleY = y + sTRINGHEIGHT
        }

        fun invalidateCurrentPosition() {
            // 计算需要重绘的区域
            invalidateRect.set(
                minOf(anchorX, handleX),
                minOf(anchorY, handleY),
                maxOf(anchorX + yoyoSize, handleX + yoyoSize),
                maxOf(anchorY + sTRINGHEIGHT, handleY + hANDLERECT.bottom)
            )
            _textField.invalidate(invalidateRect)
        }

        fun findNearestChar(touchX: Int, touchY: Int): Pair {
            val adjustedX = screenToViewX(touchX) - touchOffsetX + yoyoSize / 2
            val adjustedY = screenToViewY(touchY) - touchOffsetY - sTRINGHEIGHT - 2

            return Pair(
                _textField.coordToCharIndex(adjustedX, adjustedY),
                _textField.coordToCharIndexStrict(adjustedX, adjustedY)
            )
        }

        fun setInitialTouch(x: Int, y: Int) {
            touchOffsetX = x - handleX
            touchOffsetY = y - handleY
        }

        fun isInHandle(x: Int, y: Int): Boolean {
            return isVisible && x >= handleX && x < (handleX + hANDLERECT.right) && y >= handleY && y < (handleY + hANDLERECT.bottom)
        }

        fun show() {
            isVisible = true
        }

        fun hide() {
            isVisible = false
        }

        fun clearTouch() {
            touchOffsetY = 0
            touchOffsetX = 0
        }

    }

    companion object {
        private const val ARC_START_ANGLE = 60
        private const val ARC_SWEEP_ANGLE = 60
        private const val YOYO_STRING_FACTOR = 3
    }
}