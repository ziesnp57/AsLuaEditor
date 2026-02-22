package com.aslua.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.aslua.DocumentProvider;
import com.aslua.view.FreeScrollingTextField;

/**
 * 实现自己的光标的 TouchNavigationMethod 类必须重写
 * getCaretBloat() 方法，以返回其所需的绘制区域的大小，
 * 超出光标所在字符的边界框，并使用
 * onTextDrawComplete(Canvas) 方法绘制光标。目前，仅允许固定大小的光标，
 * 但未来可能会实现可缩放的光标。
 */
public class TouchNavigationMethod extends GestureDetector.SimpleOnGestureListener {
    private final static Rect _caretBloat = new Rect(0, 0, 0, 0);
    protected static final int SCROLL_EDGE_SLOP = 10;
    protected static final int TOUCH_SLOP = 12;
    protected FreeScrollingTextField _textField;
    private EditorScroller _scroller;
    protected boolean _isCaretTouched = false;
    private GestureDetector _gestureDetector;
    private float lastDist;
    private float lastSize;
    private int fling;

    // 常量定义优化：使用 final 提高性能
    private static final int MIN_TEXT_SIZE = 20;
    private static final int MAX_TEXT_SIZE = 72;
    private static final float FLING_VELOCITY_MULTIPLIER = 2.0f;

    // 添加触摸状态追踪
    private boolean isZooming = false;
    private boolean isScrolling = false;

    public TouchNavigationMethod(FreeScrollingTextField textField) {
        _textField = textField;
        _gestureDetector = new GestureDetector(textField.getContext(), this);
        _gestureDetector.setIsLongpressEnabled(true);
    }

    @SuppressWarnings("unused")
    private TouchNavigationMethod() {
    }

    @Override
    public boolean onDown(MotionEvent e) {
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        _isCaretTouched = isNearChar(x, y, _textField.getCaretPosition());

        if (_textField.isFlingScrolling()) {
            _textField.stopFlingScrolling();
        } else if (_textField.isSelectText()) {
            if (isNearChar(x, y, _textField.getSelectionStart())) {
                _textField.focusSelectionStart();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            } else if (isNearChar(x, y, _textField.getSelectionEnd())) {
                _textField.focusSelectionEnd();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            }
        }

        if (_isCaretTouched) {
            _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // 在此处可添加按下后的逻辑
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);

        if (_textField.isSelectText()) {
            int strictCharOffset = _textField.coordToCharIndexStrict(x, y);
            if (_textField.inSelectionRange(strictCharOffset) ||
                    isNearChar(x, y, _textField.getSelectionStart()) ||
                    isNearChar(x, y, _textField.getSelectionEnd())) {
            } else {
                _textField.selectText(false);
                if (strictCharOffset >= 0) {
                    _textField.moveCaret(charOffset);
                }
            }
        } else {
            if (charOffset >= 0) {
                _textField.moveCaret(charOffset);
            }
        }

        _textField.showIME();
        return true;
    }

    /**
     * 请注意，来自滑动的抬起事件在此处未被捕获。
     * 子类必须在其 onFling() 实现中调用 super.onUp(MotionEvent)。
     * <p>
     * 此外，在多点触控情况下，非主要指针的抬起事件
     * 也未在此处捕获。
     */
    public boolean onUp(MotionEvent e) {
        _textField.stopAutoScrollCaret();
        _isCaretTouched = false;
        lastDist = 0;
        fling = 0;
        isScrolling = false;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e2.getPointerCount() > 1) {
            return false; // 多点触控时交给缩放处理
        }

        if (_isCaretTouched) {
            dragCaret(e2);
        } else {
            if (Math.abs(distanceX) > Math.abs(distanceY) * 1.5f) {
                distanceY = 0; // 水平滚动
            } else if (Math.abs(distanceY) > Math.abs(distanceX) * 1.5f) {
                distanceX = 0; // 垂直滚动
            }
            scrollView(distanceX, distanceY);
        }

        return true;
    }

    private void dragCaret(MotionEvent e) {
        if (!_textField.isSelectText() && isDragSelect()) {
            _textField.selectText(true);
        }

        int x = (int) e.getX() - _textField.getPaddingLeft();
        int y = (int) e.getY() - _textField.getPaddingTop();

        // 处理滚动
        if (isAtScrollEdge(x, y)) {
            return; // 触摸到边缘，开始自动滚动
        }

        _textField.stopAutoScrollCaret();
        int newCaretIndex = _textField.coordToCharIndex(screenToViewX((int) e.getX()), screenToViewY((int) e.getY()));
        if (newCaretIndex >= 0) {
            _textField.moveCaret(newCaretIndex);
        }
    }

    private boolean isAtScrollEdge(int x, int y) {
        return x < SCROLL_EDGE_SLOP || x >= (_textField.getContentWidth() - SCROLL_EDGE_SLOP)
                || y < SCROLL_EDGE_SLOP || y >= (_textField.getContentHeight() - SCROLL_EDGE_SLOP);
    }

    private void scrollView(float distanceX, float distanceY) {
        if (!isScrolling) {
            isScrolling = true;
        }

        int newX = (int) distanceX + _textField.getScrollX();
        int newY = (int) distanceY + _textField.getScrollY();

        // 使用 Math.min/max 简化边界检查
        newX = Math.max(0, Math.min(newX, _textField.getMaxScrollX()));
        newY = Math.max(0, Math.min(newY, _textField.getMaxScrollY()));

        _textField.smoothScrollTo(newX, newY);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (_isCaretTouched || isZooming) {
            return false;
        }

        // 根据主要方向调整速度
        if (Math.abs(velocityX) > Math.abs(velocityY) * 1.5f) {
            velocityY = 0;
        } else if (Math.abs(velocityY) > Math.abs(velocityX) * 1.5f) {
            velocityX = 0;
        }

        _textField.flingScroll(
                (int) (-velocityX * FLING_VELOCITY_MULTIPLIER),
                (int) (-velocityY * FLING_VELOCITY_MULTIPLIER)
        );

        onUp(e2);
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private boolean onTouchZoom(MotionEvent event) {
        // 仅在移动且有两个触控点时进行处理
        if (event.getAction() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 2) {
            float dist = spacing(event);

            // 初始化缩放信息
            if (lastDist == 0) {
                initializeZoom(dist);
                return true;
            }

            // 计算缩放比例
            float scaleFactor = calculateScaleFactor(dist);
            // 仅在缩放比例变化显著时更新文本大小
            if (Math.abs(scaleFactor - 1) > 0.01) {
                // 使用插值器平滑过渡
                float newSize = lastSize * scaleFactor;
                updateTextSizeWithAnimation(newSize);
            }

            // 添加实时缩放处理
            if (isZooming) {
                // 这里可以添加实时缩放的逻辑，例如更新视图
                _textField.invalidate(); // 重新绘制视图
            }

            return true;
        }

        // 重置缩放信息
        resetZoom();
        return false;
    }

    // 初始化缩放信息
    private void initializeZoom(float dist) {
        lastDist = dist;
        lastSize = _textField.getTextSize();
        isZooming = true;
    }

    // 计算缩放比例
    private float calculateScaleFactor(float currentDist) {
        return currentDist / lastDist;
    }

    // 使用动画更新文本大小
    private void updateTextSizeWithAnimation(float newSize) {
        newSize = Math.max(MIN_TEXT_SIZE, Math.min(newSize, MAX_TEXT_SIZE));

        // 直接更新文本大小，不使用动画
        _textField.setTextSize((int) newSize);
    }

    // 重置缩放信息
    private void resetZoom() {
        lastDist = 0;
        isZooming = false;
    }

    /**
     * 子类重写此方法时必须调用父类方法
     */
    public boolean onTouchEvent(MotionEvent event) {
        onTouchZoom(event);
        boolean handled = _gestureDetector.onTouchEvent(event);
        if (!handled
                && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            handled = onUp(event);
        }
        return handled;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        onDoubleTap(e);
    }

    // 双击事件
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        _isCaretTouched = true;

        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);

        if (charOffset >= 0) {
            DocumentProvider doc = _textField.createDocumentProvider();

            if (_textField.isSelectText() && _textField.inSelectionRange(charOffset)) {
                // 双击选中整行
                selectLine(doc, charOffset);
            } else {
                // 双击选中单词
                selectWord(doc, charOffset);
            }
        }
        return true;
    }

    private void selectLine(DocumentProvider doc, int charOffset) {
        int line = doc.findLineNumber(charOffset);
        int start = doc.getLineOffset(line);
        int end = doc.getLineOffset(line + 1) - 1;
        _textField.setSelectionRange(start, end - start);
    }

    private void selectWord(DocumentProvider doc, int charOffset) {
        int start = findWordStart(doc, charOffset);
        int end = findWordEnd(doc, charOffset);
        _textField.selectText(true);
        _textField.setSelectionRange(start, end - start);
    }

    private int findWordStart(DocumentProvider doc, int offset) {
        int start = offset;
        while (start > 0 && Character.isJavaIdentifierPart(doc.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private int findWordEnd(DocumentProvider doc, int offset) {
        int end = offset;
        int length = doc.length();
        while (end < length && Character.isJavaIdentifierPart(doc.charAt(end))) {
            end++;
        }
        return end;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Android 生命周期事件。见。
     */
    void onPause() {
    }

    /**
     * Android 生命周期事件。见。
     */
    void onResume() {
    }

    /**
     * 当 FreeScrollingTextField 完成文本绘制时调用。
     * 扩展 TouchNavigationMethod 的类可以使用此方法绘制，例如自定义光标。
     * <p>
     * 画布中包含填充。
     *
     * @param canvas
     */
    public void onTextDrawComplete(Canvas canvas) {
        // 什么都不做。基础光标绘制由 FreeScrollingTextField 处理。
    }

    public void onColorSchemeChanged(ColorScheme colorScheme) {
        // 什么都不做。派生类可以使用此方法相应地更改其图形资产。
    }

    //*********************************************************************
    //**************************** 工具方法 ******************************
    //*********************************************************************

    public void onChiralityChanged(boolean isRightHanded) {
        // 什么都不做。派生类可以使用此方法相应地更改其输入处理和图形资产。
    }

    /**
     * 对于任何打印的字符，此方法返回绘制光标所需的空间
     * 超过字符的边界框。子类应重写此方法如果它们正在绘制自己的光标。
     */
    public Rect getCaretBloat() {
        return _caretBloat;
    }

    final protected int getPointerId(MotionEvent e) {
        int pointerIndex = (e.getActionIndex()); // 获取当前事件的指针索引
        return e.getPointerId(pointerIndex); // 使用 getPointerId 获取指针 ID
    }

    /**
     * 将 x 坐标从屏幕坐标转换为本地坐标，
     * 不包括填充
     */
    final protected int screenToViewX(int x) {
        return x - _textField.getPaddingLeft() + _textField.getScrollX();
    }

    /**
     * 将 y 坐标从屏幕坐标转换为本地坐标，
     * 不包括填充
     */
    final protected int screenToViewY(int y) {
        return y - _textField.getPaddingTop() + _textField.getScrollY();
    }

    final public boolean isRightHanded() {
        return true;
    }

    private boolean isDragSelect() {
        return false;
    }

    public boolean isNearChar(int x, int y, int charOffset) {
        Rect bounds = _textField.getBoundingBox(charOffset);

        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }
}
