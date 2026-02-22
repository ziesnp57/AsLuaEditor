package com.aslua.view;


import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.content.ClipboardManager;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.CharacterPickerDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.aslua.DocumentProvider;
import com.aslua.Lexer;
import com.aslua.Pair;
import com.aslua.TextWarriorException;
import com.aslua.language.DefFormatter;
import com.aslua.language.Language;
import com.aslua.view.ColorScheme.Colorable;
import com.aslua.view.FreeScrollingTextAbstract;
import com.aslua.view.listener.OnRowChangedListener;
import com.aslua.view.listener.OnSelectionChangedListener;
import com.aslua.language.lua.LuaLanguage;
import com.aslua.view.listener.*;

public abstract class FreeScrollingTextField extends FreeScrollingTextAbstract {

    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f; // 空光标宽度缩放比例
    protected static float SEL_CARET_HEIGHT_SCALE = 0.5f; // 选择光标高度缩放比例
    protected static int DEFAULT_TAB_LENGTH_SPACES = 3; // 默认制表符长度（空格数）
    protected static int BASE_TEXT_SIZE_PIXELS = 16; // 基本文本大小（像素）
    protected static long SCROLL_PERIOD = 250; // 滚动周期（毫秒）
    private EditorScroller _scroller; // 滚动器
    protected boolean _isEdited = false; // 文本框是否已被修改
    protected TouchNavigationMethod _navMethod; // 触摸导航方法
    protected DocumentProvider _hDoc; // MVC中的文档模型
    protected int _caretPosition = 0; // 光标位置
    protected int _selectionAnchor = -1; // 选择锚点（包含）
    protected int _selectionEdge = -1; // 选择边缘（不包含）
    protected int _tabLength = DEFAULT_TAB_LENGTH_SPACES; // 制表符长度
    protected ColorScheme _colorScheme = new ColorSchemeLight(); // 颜色方案
    protected boolean _isHighlightRow = true; // 是否高亮当前行
    protected boolean _showNonPrinting = false; // 是否显示不可打印字符
    protected boolean _isAutoIndent = true; // 是否自动缩进
    protected int _autoIndentWidth = 1; // 自动缩进宽度
    protected boolean _isLongPressCaps = false; // 是否长按大写
    protected AutoCompletePanel _autoCompletePanel; // 自动补全面板
    protected boolean _isAutoComplete = true; // 是否启用自动补全
    private TextFieldController _fieldController; // MVC中的控制器
    private TextFieldInputConnection _inputConnection; // 输入连接
    private OnRowChangedListener _rowLis; // 行改变监听器
    private OnSelectionChangedListener _selModeLis; // 选择模式改变监听器
    private int _caretRow = 0; // 光标行（为提高效率而存储）
    private Paint _brush; // 用于绘制文本的画笔
    private int _xExtent = 0; // 水平滚动范围
    private int _leftOffset = 0; // 左偏移量
    private boolean _showLineNumbers = false; // 是否显示行号
    private ClipboardPanel _clipboardPanel; // 剪贴板面板
    private ClipboardManager _clipboardManager; // 剪贴板管理器
    private float _zoomFactor = 1; // 缩放因子
    private int _caretX; // 光标的X坐标
    private int _caretY; // 光标的Y坐标
    private TextChangeListener _textLis; // 文本变化监听器
    private int _topOffset; // 顶部偏移量
    private Typeface _defTypeface = Typeface.DEFAULT; // 默认字体
    private Typeface _boldTypeface = Typeface.DEFAULT_BOLD; // 粗体字体
    private Typeface _italicTypeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); // 斜体字体
    private char _emoji; // 表情字符
    private boolean _isLayout; // 布局状态
    private Paint _brushLine; // 用于绘制行的画笔
    private int _alphaWidth; // 字母宽度
    private final Runnable _scrollCaretDownTask = new Runnable() { // 向下滚动光标的任务
        @Override
        public void run() {
            _fieldController.moveCaretDown(); // 移动光标向下
            if (!caretOnLastRowOfFile()) { // 如果光标不在文件最后一行
                postDelayed(_scrollCaretDownTask, SCROLL_PERIOD); // 延迟执行下一次向下滚动
            }
        }
    };
    private final Runnable _scrollCaretUpTask = new Runnable() { // 向上滚动光标的任务
        @Override
        public void run() {
            _fieldController.moveCaretUp(); // 移动光标向上
            if (!caretOnFirstRowOfFile()) { // 如果光标不在文件第一行
                postDelayed(_scrollCaretUpTask, SCROLL_PERIOD); // 延迟执行下一次向上滚动
            }
        }
    };
    private final Runnable _scrollCaretLeftTask = new Runnable() { // 向左滚动光标的任务
        @Override
        public void run() {
            _fieldController.moveCaretLeft(false); // 移动光标向左
            if (_caretPosition > 0 && // 如果光标位置大于0
                    _caretRow == _hDoc.findRowNumber(_caretPosition - 1)) { // 如果光标行号与目标行号相同
                postDelayed(_scrollCaretLeftTask, SCROLL_PERIOD); // 延迟执行下一次向左滚动
            }
        }
    };
    private final Runnable _scrollCaretRightTask = new Runnable() { // 向右滚动光标的任务
        @Override
        public void run() {
            _fieldController.moveCaretRight(false); // 移动光标向右
            if (!caretOnEOF() && // 如果光标不在文件末尾
                    _caretRow == _hDoc.findRowNumber(_caretPosition + 1)) { // 如果光标行号与目标行号相同
                postDelayed(_scrollCaretRightTask, SCROLL_PERIOD); // 延迟执行下一次向右滚动
            }
        }
    };
    private int _spaceWidth; // 空格宽度


    public FreeScrollingTextField(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initTextField(context); // 初始化文本框
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initTextField(context); // 初始化文本框
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        initTextField(context); // 初始化文本框
    }

    protected void initTextField(Context context) {
        _hDoc = new DocumentProvider(this); // 创建文档提供者
        _navMethod = new TouchNavigationMethod(this); // 创建触摸导航方法
        // 使用新的滚动控制器
        _scroller = new EditorScroller(this);
        initView(); // 初始化视图
    }

    public int getTopOffset() {
        return _topOffset; // 获取顶部偏移量
    }

    public int getAutoIndentWidth() {
        return _autoIndentWidth; // 获取自动缩进宽度
    }

    // 设置换行空格数目
    public void setAutoIndentWidth(int autoIndentWidth) {
        _autoIndentWidth = autoIndentWidth; // 更新自动缩进宽度
    }

    public int getCaretY() {
        return _caretY; // 获取光标的Y坐标
    }

    public int getCaretX() {
        return _caretX; // 获取光标的X坐标
    }

    public boolean isShowLineNumbers() {
        return _showLineNumbers; // 是否显示行号
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        _showLineNumbers = showLineNumbers; // 更新是否显示行号
    }

    public int getLeftOffset() {
        return _leftOffset; // 获取左偏移量
    }

    public float getTextSize() {
        return _brush.getTextSize(); // 获取文本大小
    }

    public void setTextSize(int newTextSize) {
        // 检查文本大小是否在有效范围内
        if (newTextSize <= 20 || newTextSize >= 72 || newTextSize == _brush.getTextSize()) {
            return; // 无需更新
        }

        double oldHeight = rowHeight(); // 记录原行高
        double oldWidth = getAdvance('a'); // 记录原字符宽度
        _zoomFactor = (float) newTextSize / BASE_TEXT_SIZE_PIXELS; // 计算缩放因子

        // 更新画笔文本大小
        _brush.setTextSize(newTextSize);
        _brushLine.setTextSize(newTextSize);

        // 处理换行逻辑
        if (_hDoc.isWordWrap()) {
            _hDoc.analyzeWordWrap();
        }

        _fieldController.updateCaretRow(); // 更新光标行

        // 计算新的滚动位置
        double x = getScrollX() * (getAdvance('a') / oldWidth);
        double y = getScrollY() * (rowHeight() / oldHeight);
        scrollTo((int) x, (int) y); // 滚动视图

        _alphaWidth = (int) _brush.measureText("a"); // 计算字符宽度
        _spaceWidth = (int) _brush.measureText(" "); // 计算空格宽度

        // 刷新视图
        invalidate();
    }

    /**
     * 替换文本的方法
     *
     * @param from      开始替换的位置
     * @param charCount 要替换的字符数量
     * @param text      用于替换的新文本
     */
    public void replaceText(int from, int charCount, String text) {
        _hDoc.beginBatchEdit();

        // 调用控制器执行实际的文本替换操作
        // 包含删除原有文本和插入新文本
        _fieldController.replaceText(from, charCount, text);

        // 停止文本输入法的组合状态
        // 确保输入法不会继续组合文本
        _fieldController.stopTextComposing();

        // 结束批量编辑操作
        // 此时会触发必要的重绘和更新操作
        _hDoc.endBatchEdit();
    }


    // 获取文本
    public int getLength() {
        return _hDoc.docLength();
    }

    private void initView() {
        _fieldController = this.new TextFieldController(); // 创建文本框控制器
        _clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE); // 获取剪贴板管理器
        _brush = new Paint();
        _brush.setAntiAlias(true);  // 抗锯齿
        _brush.setTextSize(BASE_TEXT_SIZE_PIXELS); // 设置文本大小
        _brushLine = new Paint();
        _brushLine.setAntiAlias(true);  // 设置抗锯齿
        _brushLine.setTextSize(BASE_TEXT_SIZE_PIXELS); // 设置文本大小
        //setBackgroundColor(_colorScheme.getColor(Colorable.BACKGROUND)); // 设置背景颜色
        setFocusableInTouchMode(true); // 设置触摸模式下可获取焦点

        _rowLis = newRowIndex -> {
            // Do nothing
        };

        _selModeLis = (active, selStart, selEnd) -> {
            if (active)
                _clipboardPanel.show();
            else
                _clipboardPanel.hide();
        };


        // 输入文本变化监听器
        _textLis = new TextChangeListener() {
            @Override
            public void onNewLine(String c, int _caretPosition, int p2) {
                // 关闭自动完成面板
                _autoCompletePanel.dismiss();
            }

            @Override
            public void onDel(CharSequence text, int caretPosition, int delCount) {
                // 关闭自动完成面板
                _autoCompletePanel.dismiss();
            }

            @Override
            public void onAdd(CharSequence text, int caretPosition, int addCount) {

                // 如果未启用自动完成功能，直接返回
                if (!_isAutoComplete)
                    return;

                // 从当前光标位置向前搜索
                int curr = _caretPosition;
                for (; curr >= 0; curr--) {
                    // 获取前一个字符
                    char c = _hDoc.charAt(curr - 1);
                    // 如果不是字母、数字、下划线或点号，则停止搜索
                    if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) {
                        break;
                    }
                }
                // 如果找到有效的自动完成触发文本
                if (_caretPosition - curr > 0)
                    // 更新自动完成面板，传入当前单词作为参数
                    _autoCompletePanel.update(_hDoc.subSequence(curr, _caretPosition - curr));
                else
                    _autoCompletePanel.dismiss();
            }
        };

        resetView();
        _clipboardPanel = new ClipboardPanel(this);
        _autoCompletePanel = new AutoCompletePanel(this);
        AutoCompletePanel.setLanguage(LuaLanguage.getInstance());
       // setScrollContainer(true); // 设置滚动容器
        invalidate();
    }


    private void resetView() {
        _caretPosition = 0;
        _caretRow = 0;
        _xExtent = 0;
        _fieldController.setSelectText(false);
        _fieldController.stopTextComposing();
        _hDoc.clearSpans();
        if (getContentWidth() > 0 || !_hDoc.isWordWrap()) {
            _hDoc.analyzeWordWrap();
        }
        _rowLis.onRowChanged(0);
        scrollTo(0, 0);
    }

    // 设置文档提供者
    public void setDocumentProvider(DocumentProvider hDoc) {
        _hDoc = hDoc;
        resetView();
        _fieldController.cancelSpanning();
        _fieldController.determineSpans();
        invalidate();
    }

    /**
     * Returns a DocumentProvider that references the same Document used by the
     * FreeScrollingTextField.
     */

    public DocumentProvider createDocumentProvider() {
        return new DocumentProvider(_hDoc);
    }

    // 获取文档提供者
    public void setRowListener(OnRowChangedListener rLis) {
        _rowLis = rLis;
    }

    // 获取文档提供者
    public void setOnSelectionChangedListener(OnSelectionChangedListener sLis) {
        _selModeLis = sLis;
    }

    /**
     * Sets the caret navigation method used by this text field
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        _navMethod = navMethod;
    }

    public void setChirality(boolean isRightHanded) {
        _navMethod.onChiralityChanged(isRightHanded);
    }

    // this used to be isDirty(), but was renamed to avoid conflicts with Android API 11
    public boolean isEdited() {
        return _isEdited;
    }

    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------

    public void setEdited(boolean set) {
        _isEdited = set;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // 设置输入类型为多行文本
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;

        // 设置输入法选项
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_ACTION_DONE // 添加完成操作
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI; // 禁用提取UI
        // 检查是否已经创建了输入连接
        if (_inputConnection == null) {
            // 创建新的输入连接
            _inputConnection = this.new TextFieldInputConnection(this);
        } else {
            // 重置正在组成的状态
            _inputConnection.resetComposingState();
        }

        // 返回输入连接
        return _inputConnection;
    }

    //---------------------------------------------------------------------
    //------------------------- 布局方法 ----------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置测量尺寸
        setMeasuredDimension(
                useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // 检查布局是否发生变化
        if (changed) {
            Rect rect = new Rect();
            // 获取当前窗口可见区域
            getWindowVisibleDisplayFrame(rect);
            // 计算顶部偏移量
            _topOffset = rect.top + rect.height() - getHeight();

            // 如果之前未进行布局，则重新计算文本的排版
            if (!_isLayout) {
                try {
                    respan(); // 重新排版
                } catch (Exception e) {
                    // 捕获可能的异常并记录错误日志
                    Log.e("OnLayout", "排版时发生错误", e);
                }
            }

            // 更新布局状态
            _isLayout = right > 0;
            // 请求重绘视图
            invalidate();
            // 设置自动完成面板的宽度为视图宽度的0.5倍
            _autoCompletePanel.setWidth(getWidth() / 2);
        }

        // 调用父类方法以执行其他布局逻辑
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 如果启用了换行并且宽度发生变化，则分析换行
        if (_hDoc.isWordWrap() && oldw != w)
            _hDoc.analyzeWordWrap();
        _fieldController.updateCaretRow(); // 更新光标行
        // 如果高度变小，则确保光标所在字符可见
        if (h < oldh)
            makeCharVisible(_caretPosition);
    }

    // 使用所有维度
    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE; // 将结果设置为最大值
            TextWarriorException.fail("MeasureSpec不能为UNSPECIFIED。设置尺寸为最大。");
        }

        return result;
    }

    // 获取可见行数
    protected int getNumVisibleRows() {
        return (int) Math.ceil((double) getContentHeight() / rowHeight());
    }

    // 获取每行高度
    protected int rowHeight() {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        return (metrics.descent - metrics.ascent); // 计算行高
    }

    // 获取内容高度（减去顶部和底部填充）
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    // 获取内容宽度（减去左右填充）
    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * 确定视图是否已布局或仍在构造中
     */
    public boolean hasLayout() {
        return (getWidth() == 0); // 简单实现，但在大多数情况下应该有效
    }

    /**
     * 绘制文本的第一行，可能部分可见。
     * 从传递给onDraw()的裁剪矩形中推导
     */
    private int getBeginPaintRow(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return bounds.top / rowHeight(); // 返回起始绘制行
    }

    /**
     * 绘制文本的最后一行，可能部分可见。
     * 从传递给onDraw()的裁剪矩形中推导
     */
    private int getEndPaintRow(Canvas canvas) {
        // 上下和左侧边界为包含性；右侧和底部边界为排他性
        Rect bounds = canvas.getClipBounds();
        return (bounds.bottom - 1) / rowHeight(); // 返回结束绘制行
    }

    /**
     * @return 绘制给定行文本的基线的x值
     */
    public int getPaintBaseline(int row) {
        // 使用局部变量存储行高和字体度量
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int rowHeight = rowHeight(); // 计算行高
        return (row + 1) * rowHeight - metrics.descent; // 返回基线
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas.isHardwareAccelerated()) {
            // 硬件加速模式下的优化
            canvas.save();
            canvas.clipRect(getScrollX() + getPaddingLeft(),
                    getScrollY() + getPaddingTop(),
                    getScrollX() + getWidth() - getPaddingRight(),
                    getScrollY() + getHeight() - getPaddingBottom());
            canvas.translate(getPaddingLeft(), getPaddingTop());
            realDraw(canvas);
            canvas.restore();
            _navMethod.onTextDrawComplete(canvas);
        } else {
            // 软件渲染模式下的原有逻辑
            super.onDraw(canvas);
        }
    }


    private void realDraw(Canvas canvas) {
        //----------------------------------------------
        // 初始化并设置边界
        //----------------------------------------------
        int currRowNum = getBeginPaintRow(canvas); // 当前绘制的行数
        int currIndex = _hDoc.getRowOffset(currRowNum); // 当前行的索引
        if (currIndex < 0) {
            return; // 如果索引无效，则返回
        }

        int len = _hDoc.length();
        int currLineNum = isWordWrap() ? _hDoc.findLineNumber(currIndex) + 1 : currRowNum + 1; // 当前行号
        int lastLineNum = -1; // 上一行号
        if (_showLineNumbers) {
            _leftOffset = (int) _brushLine.measureText(_hDoc.getRowCount() + " "); // 计算左偏移
        }
        int endRowNum = getEndPaintRow(canvas); // 结束绘制行数
        int paintX; // 绘制X坐标
        int paintY = getPaintBaseline(currRowNum); // 绘制Y坐标

        //----------------------------------------------
        // 设置初始的文本样式
        //----------------------------------------------
        int spanIndex = 0; // 当前样式索引
        List<Pair> spans = _hDoc.getSpans(); // 获取文本样式列表

        if (spans.isEmpty())
            return; // 如果没有样式，则返回
        // 需要至少有一个样式才能绘制，即使是空文件，其中样式仅包含EOF字符
        TextWarriorException.assertVerbose(!spans.isEmpty(),
                "在TextWarrior.paint()中没有可绘制的样式");

        Pair nextSpan = spans.get(spanIndex++); // 获取下一个样式
        Pair currSpan; // 当前样式
        int spanOffset = 0; // 当前样式偏移
        int spanSize = spans.size(); // 样式列表大小

        // 处理当前样式并开始绘制
        do {
            currSpan = nextSpan;
            spanOffset += currSpan.first; // 更新样式偏移
            if (spanIndex < spanSize) {
                nextSpan = spans.get(spanIndex++);
            } else {
                nextSpan = null; // 如果没有下一个样式，则设置为null
            }
        } while (nextSpan != null && spanOffset <= currIndex); // 查找当前索引对应的样式
        int currType = currSpan.second; // 当前样式类型
        int lastType; // 上一个样式类型

        // 根据当前样式设置画笔
        switch (currSpan.second) {
            case Lexer.KEYWORD:
                _brush.setTypeface(_boldTypeface); // 设置粗体
                break;
            case Lexer.DOUBLE_SYMBOL_LINE:
                _brush.setTypeface(_italicTypeface); // 设置斜体
                break;
            default:
                _brush.setTypeface(_defTypeface); // 默认字体
        }
        int spanColor = _colorScheme.getTokenColor(currSpan.second); // 获取样式颜色
        _brush.setColor(spanColor); // 设置画笔颜色

        //----------------------------------------------
        // 开始绘制！
        //----------------------------------------------
        int rowCount = _hDoc.getRowCount(); // 获取行数
        if (_showLineNumbers) {
            _brushLine.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH)); // 设置行号颜色
            // 绘制行号分隔线
            canvas.drawLine(_leftOffset - (float) _spaceWidth / 2, getScrollY(), _leftOffset - (float) _spaceWidth / 2, getScrollY() + getHeight(), _brushLine);
        }

        Typeface lastTypeface = switch (currType) {
            case Lexer.KEYWORD -> _boldTypeface; // 当前样式为关键字，设置粗体
            case Lexer.DOUBLE_SYMBOL_LINE -> _italicTypeface; // 当前样式为双符号行，设置斜体
            default -> _defTypeface; // 默认字体
        };

        _brush.setTypeface(lastTypeface); // 设置画笔字体
        while (currRowNum <= endRowNum) {
            // 计算当前行的跨度长度
            int spanLen = spanOffset - currIndex;

            // 获取当前行的长度
            int rowLen = _hDoc.getRowSize(currRowNum);
            if (currRowNum >= rowCount) {
                break; // 如果当前行超过总行数，退出循环
            }

            // 计算行号的绘制位置
            int padx = (int) (_leftOffset - _brushLine.measureText(currLineNum + "") - (float) _spaceWidth / 2) / 2;

            // 如果显示行号且当前行与上一次绘制的行号不同，则绘制行号
            if (_showLineNumbers && currRowNum != lastLineNum) {
                lastLineNum = currRowNum;
                String num = String.valueOf(currLineNum);
                drawLineNum(canvas, num, padx, paintY); // 绘制行号
            }

            paintX = _leftOffset; // 重置绘制 X 位置

            int i = 0;

            // 遍历当前行的所有字符
            while (i < rowLen) {
                // 检查是否需要改变格式
                if (nextSpan != null && currIndex >= spanOffset) {
                    currSpan = nextSpan;

                    spanLen = currSpan.first; // 当前跨度长度
                    spanOffset += spanLen; // 更新跨度偏移量
                    lastType = currType; // 保存上一个类型
                    currType = currSpan.second; // 更新当前类型

                    // 如果类型发生变化，则更新字体和颜色
                    if (lastType != currType) {
                        Typeface currTypeface = switch (currType) {
                            case Lexer.KEYWORD -> _boldTypeface; // 关键字使用粗体
                            case Lexer.DOUBLE_SYMBOL_LINE -> _italicTypeface; // 双符号行使用斜体
                            default -> _defTypeface; // 默认字体
                        };

                        // 如果当前字体与上一个不同，则更新字体
                        if (lastTypeface != currTypeface) {
                            _brush.setTypeface(currTypeface);
                            lastTypeface = currTypeface; // 更新上一个字体
                        }

                        // 设置当前类型的颜色
                        spanColor = _colorScheme.getTokenColor(currType);
                        _brush.setColor(spanColor);
                    }

                    // 更新下一个跨度
                    if (spanIndex < spanSize) {
                        nextSpan = spans.get(spanIndex++);
                    } else {
                        nextSpan = null; // 没有更多跨度
                    }
                }

                // 获取当前字符
                char c = _hDoc.charAt(currIndex);
                int x = paintX; // 保存当前绘制 X 位置，以解决光标被选择遮挡的问题

                // 如果当前字符在选中范围内，则绘制选中文本
                if (_fieldController.inSelectionRange(currIndex)) {
                    paintX += drawSelectedText(canvas, c, paintX, paintY);
                } else {
                    // 否则绘制普通字符
                    paintX += drawChar(canvas, c, paintX, paintY);
                }

                // 如果当前索引是光标位置，则绘制光标
                if (currIndex == _caretPosition) {
                    drawCaret(canvas, x, paintY);
                }
                ++currIndex; // 移动到下一个字符
                ++i; // 增加字符计数
                spanLen--; // 减少当前跨度长度
            }

            // 如果当前字符是换行符，则增加行计数
            if (_hDoc.charAt(currIndex - 1) == Language.NEWLINE)
                ++currLineNum;

            paintY += rowHeight(); // 更新 Y 位置，准备绘制下一行
            if (paintX > _xExtent) {
                // 记录当前绘制行的最大宽度
                _xExtent = paintX;
            }
            ++currRowNum; // 移动到下一行
        } // 结束循环
        doOptionHighlightRow(canvas);
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int advanceWidth;
        if (Character.valueOf(DefFormatter.indentChar).equals('\t')) {
            advanceWidth = getTabAdvance(0);
        } else {
            advanceWidth = _spaceWidth;
        }

        if (isShowRegion && !isWordWrap()) {
            doBlockLine(canvas);
//            _brush.setColor(o);
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private void doBlockLine(Canvas canvas) {
        ArrayList<Rect> lines = Lexer.mLines;
        if (lines.isEmpty())
            return;

        Rect bounds = canvas.getClipBounds();
        int bt = bounds.top;
        int bb = bounds.bottom;
        Rect curr = null;
        boolean foundCaretBlock = false;

        for (Rect rect : lines) {
            if (foundCaretBlock) break;

            int top = (rect.top + 1) * rowHeight();
            int bottom = rect.bottom * rowHeight();
            if (bottom <= bt || top >= bb)
                continue;

            int leftExtent = getCharExtent(rect.left).first;
            int rightExtent = getCharExtent(rect.right).first;
            int left = Math.min(leftExtent, rightExtent);

            if (rect.left < _caretPosition && rect.right >= _caretPosition) {
                curr = rect;
                foundCaretBlock = true;
            }

            drawBlockLine(canvas, left, top, bottom, _brushLine); // 绘制常规块线条
        }

        if (curr != null) {
            int top = (curr.top + 1) * rowHeight();
            int bottom = curr.bottom * rowHeight();
            if (bottom > bt && top < bb) {
                int left = Math.min(getCharExtent(curr.left).first, getCharExtent(curr.right).first);
                _brushLine.setColor(_colorScheme.getColor(Colorable.CARET_FOREGROUND));
                drawBlockLine(canvas, left, top, bottom, _brushLine);
                _brushLine.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH)); // 恢复颜色
            }
        }
    }

    private void drawBlockLine(Canvas canvas, int left, int top, int bottom, Paint paint) {
        canvas.drawLine(left, top, left, bottom, paint);
    }

    public boolean isShowRegion = true;

    /**
     * Underline the caret row if the option for highlighting it is set
     */
    //高亮当前行
    private void doOptionHighlightRow(Canvas canvas) {
        if (_isHighlightRow) {
            int y = getPaintBaseline(_caretRow);
            int originalColor = _brush.getColor();
            _brush.setColor(_colorScheme.getColor(Colorable.LINE_HIGHLIGHT));

            int lineLength = Math.max(_xExtent, getContentWidth());
            drawTextBackground(canvas, 0, y, lineLength + 50);
            _brush.setColor(originalColor);
        }
    }

    // 绘制字符
    private int drawChar(Canvas canvas, char c, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        int charWidth = getAdvance(c, paintX);

        if (paintX > getScrollX() || paintX < (getScrollX() + getContentWidth()))
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    _emoji = c;
                    break;
                case ' ':
                    if (_showNonPrinting) {
                        _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_SPACE, 0, 1, paintX, paintY, _brush);
                        _brush.setColor(originalColor);
                    } else {
                        canvas.drawText(" ", 0, 1, paintX, paintY, _brush);
                    }
                    break;

                case Language.EOF: //fall-through
                case Language.NEWLINE:
                    if (_showNonPrinting) {
                        _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_NEWLINE, 0, 1, paintX, paintY, _brush);
                        _brush.setColor(originalColor);
                    }
                    break;

                case Language.TAB:
                    if (_showNonPrinting) {
                        _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                        canvas.drawText(Language.GLYPH_TAB, 0, 1, paintX, paintY, _brush);
                        _brush.setColor(originalColor);
                    }
                    break;

                default:
                    if (_emoji != 0) {
                        canvas.drawText(new char[]{_emoji, c}, 0, 2, paintX, paintY, _brush);
                        _emoji = 0;
                    } else {
                        char[] ca = {c};
                        canvas.drawText(ca, 0, 1, paintX, paintY, _brush);
                    }
                    break;
            }

        return charWidth;
    }

    // 绘制选中文本背景
    private void drawTextBackground(Canvas canvas, int paintX, int paintY, int advance) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                _brush);
    }

    // 绘制选中文本
    private int drawSelectedText(Canvas canvas, char c, int paintX, int paintY) {
        int oldColor = _brush.getColor();
        int advance = getAdvance(c);

        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);

        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_FOREGROUND));
        drawChar(canvas, c, paintX, paintY);

        _brush.setColor(oldColor);
        return advance;
    }

    //光标宽度
    public int caretWidth = 5;

    //光标
    private void drawCaret(Canvas canvas, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        _caretX = paintX - caretWidth / 2;
        _caretY = paintY;
        int caretColor = _colorScheme.getColor(Colorable.CARET_DISABLED);
        _brush.setColor(caretColor);
        // draw full caret
        drawTextBackground(canvas, _caretX, paintY, caretWidth);
        _brush.setColor(originalColor);
    }

    private int drawLineNum(Canvas canvas, String s, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
        canvas.drawText(s, paintX, paintY, _brushLine);
        _brush.setColor(originalColor);
        return 0;
    }

    @Override
    final public int getRowWidth() {
        return getContentWidth() - _leftOffset;
    }

    /**
     * Returns printed width of c.
     * <p>
     * Takes into account user-specified tab width and also handles
     * application-defined widths for NEWLINE and EOF
     *
     * @param c Character to measure
     * @return Advance of character, in pixels
     */
    @Override
    public int getAdvance(char c) {
        return getAdvance(c, 0);
    }

    public int getAdvance(char c, int x) {
        int advance;
        switch (c) {
            case 0xd83c:
            case 0xd83d:
                advance = 0;
                break;
            case ' ':
                advance = getSpaceAdvance();
                break;
            case Language.NEWLINE:
            case Language.EOF:
                advance = getEOLAdvance();
                break;
            case Language.TAB:
                advance = getTabAdvance(x);
                break;
            default:
                if (_emoji != 0) {
                    char[] ca = {_emoji, c};
                    advance = (int) _brush.measureText(ca, 0, 2);
                } else {
                    char[] ca = {c};
                    advance = (int) _brush.measureText(ca, 0, 1);
                }
                break;
        }

        return advance;
    }

    public int getCharAdvance(char c) {
        int advance;
        char[] ca = {c};
        advance = (int) _brush.measureText(ca, 0, 1);
        return advance;
    }

    protected int getSpaceAdvance() {
        if (_showNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            return _spaceWidth;
        }
    }

//---------------------------------------------------------------------
//------------------- 滚动和触摸 -----------------------------

    // 滚动视图以使光标可见
    protected int getEOLAdvance() {
        if (_showNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_NEWLINE,
                    0, Language.GLYPH_NEWLINE.length());
        } else {
            return (int) (EMPTY_CARET_WIDTH_SCALE * _brush.measureText(" ", 0, 1));
        }
    }

    // 滚动视图以使光标可见
    protected int getTabAdvance(int x) {
        if (_showNonPrinting) {
            return _tabLength * (int) _brush.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            int i = 0;
            if (x != 0)
                i = (x - _leftOffset) / _spaceWidth % _tabLength;
            return (_tabLength - i) * _spaceWidth;
        }
    }

    /**
     * 从 startRow（包含）到 endRow（不包含）无效化行
     */
    // 滚动视图以使光标可见
    private void invalidateRows(int startRow, int endRow) {
        TextWarriorException.assertVerbose(startRow <= endRow && startRow >= 0,
                "无效的 startRow 和/或 endRow");

        Rect caretSpill = _navMethod.getCaretBloat();

        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endRow * rowHeight() + getPaddingTop() + caretSpill.bottom);
    }

    /**
     * 从 startRow（包含）到字段的末尾无效化行
     */
    private void invalidateFromRow(int startRow) {
        TextWarriorException.assertVerbose(startRow >= 0,
                "无效的 startRow");

        Rect caretSpill = _navMethod.getCaretBloat();

        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }


    // 滚动视图以使光标可见
    private void invalidateCaretRow() {
        invalidateRows(_caretRow, _caretRow + 1);
    }


    // 滚动视图以使光标可见
    private void invalidateSelectionRows() {
        int startRow = _hDoc.findRowNumber(_selectionAnchor);
        int endRow = _hDoc.findRowNumber(_selectionEdge);

        invalidateRows(startRow, endRow + 1);
    }

    /**
     * 如果指定的字符（charOffset）不在可见文本区域内，
     * 则水平和/或垂直滚动文本。
     * 如果进行了滚动，视图将被无效化。
     *
     * @param charOffset 要使其可见的字符索引
     * @return 如果绘图区域在水平和/或垂直方向上滚动，则为 true
     */
    private boolean makeCharVisible(int charOffset) {
        TextWarriorException.assertVerbose(
                charOffset >= 0 && charOffset < _hDoc.docLength(),
                "给定的 charOffset 无效");
        int scrollVerticalBy = makeCharRowVisible(charOffset);
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);

        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            scrollBy(scrollHorizontalBy, scrollVerticalBy);
            return true;
        }
    }

    /**
     * 计算如果字符不在可见区域内，垂直滚动的量。
     *
     * @param charOffset 要使其可见的字符索引
     * @return 垂直滚动的量
     */
    private int makeCharRowVisible(int charOffset) {
        int scrollBy = 0;
        int charTop = _hDoc.findRowNumber(charOffset) * rowHeight();
        int charBottom = charTop + rowHeight();

        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight();
        }

        return scrollBy;
    }

    /**
     * 计算如果字符不在可见区域内，水平滚动的量。
     *
     * @param charOffset 要使其可见的字符索引
     * @return 水平滚动的量
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = getCharExtent(charOffset);

        int charLeft = visibleRange.first;
        int charRight = visibleRange.second;

        if (charRight > (getScrollX() + getContentWidth())) {
            scrollBy = charRight - getScrollX() - getContentWidth();
        }

        if (charLeft < getScrollX() + _alphaWidth) {
            scrollBy = charLeft - getScrollX() - _alphaWidth;
        }

        return scrollBy;
    }

    /**
     * 计算 charOffset 的 x 坐标范围。
     *
     * @return charOffset 左右边缘的 x 值。Pair.first
     * 包含左边缘，Pair.second 包含右边缘
     */
    protected Pair getCharExtent(int charOffset) {
        int row = _hDoc.findRowNumber(charOffset);
        int rowOffset = _hDoc.getRowOffset(row);
        int left = _leftOffset;
        int right = _leftOffset;
        boolean isEmoji = false;
        String rowText = _hDoc.getRow(row);
        int i = 0;

        int len = rowText.length();
        while (rowOffset + i <= charOffset && i < len) {
            char c = rowText.charAt(i);
            left = right;
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    right += (int) _brush.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    right += getEOLAdvance();
                    break;
                case ' ':
                    right += getSpaceAdvance();
                    break;
                case Language.TAB:
                    right += getTabAdvance(right);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        right += getCharAdvance(c);
                    break;
            }
            ++i;
        }
        return new Pair(left, right);
    }

    /**
     * 返回文本框中字符的边界框。
     * 使用的坐标系是 (0, 0) 位于文本的左上角，
     * 在添加填充之前。
     *
     * @param charOffset 感兴趣字符的字符偏移量
     * @return Rect(left, top, right, bottom) 字符的边界，
     * 如果该坐标没有字符，则返回 Rect(-1, -1, -1, -1)。
     */
    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= _hDoc.docLength()) {
            return new Rect(-1, -1, -1, -1);
        }

        int row = _hDoc.findRowNumber(charOffset);
        int top = row * rowHeight();
        int bottom = top + rowHeight();

        Pair xExtent = getCharExtent(charOffset);
        int left = xExtent.first;
        int right = xExtent.second;

        return new Rect(left, top, right, bottom);
    }

    public ColorScheme getColorScheme() {
        return _colorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        _colorScheme = colorScheme;
        _navMethod.onColorSchemeChanged(colorScheme);
        setBackgroundColor(colorScheme.getColor(Colorable.BACKGROUND));
    }

    /**
     * 将坐标映射到字符上。如果坐标位于空白区域，
     * 则返回对应行上最近的字符。如果该行没有字符，则返回 -1。
     * <p>
     * 传入的坐标不应应用填充。
     *
     * @param x x坐标
     * @param y y坐标
     * @return 最近字符的索引，如果该坐标没有字符或最近字符则返回 -1
     */
    int coordToCharIndex(int x, int y) {
        int row = y / rowHeight();
        if (row > _hDoc.getRowCount())
            return _hDoc.docLength() - 1;

        int charIndex = _hDoc.getRowOffset(row);
        if (charIndex < 0) {
            // 不存在的行
            return -1;
        }

        if (x < 0) {
            return charIndex; // 坐标在视图的左侧
        }

        String rowText = _hDoc.getRow(row);

        int extent = _leftOffset;
        int i = 0;
        boolean isEmoji = false;

        // x-=getAdvance('a')/2; // 如果需要，可以在这里进行 x 坐标的调整
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) _brush.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case Language.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);
            }

            if (extent >= x) {
                break;
            }

            ++i;
        }

        if (i < rowText.length()) {
            return charIndex + i;
        }
        // 最近的字符是行的最后一个字符
        return charIndex + i - 1;
    }

    /**
     * 将坐标映射到字符上。
     * 如果坐标上没有字符，则返回 -1。
     * <p>
     * 传入的坐标不应应用填充。
     *
     * @param x x坐标
     * @param y y坐标
     * @return 在该坐标上的字符索引，
     * 如果该坐标没有字符，则返回 -1。
     */
    int coordToCharIndexStrict(int x, int y) {
        int row = y / rowHeight();
        int charIndex = _hDoc.getRowOffset(row);

        if (charIndex < 0 || x < 0) {
            // 不存在的行
            return -1;
        }

        String rowText = _hDoc.getRow(row);

        int extent = 0;
        int i = 0;
        boolean isEmoji = false;

        // x-=getAdvance('a')/2; // 如果需要，可以在这里进行 x 坐标的调整
        int len = rowText.length();
        while (i < len) {
            char c = rowText.charAt(i);
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    isEmoji = true;
                    char[] ca = {c, rowText.charAt(i + 1)};
                    extent += (int) _brush.measureText(ca, 0, 2);
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    extent += getEOLAdvance();
                    break;
                case ' ':
                    extent += getSpaceAdvance();
                    break;
                case Language.TAB:
                    extent += getTabAdvance(extent);
                    break;
                default:
                    if (isEmoji)
                        isEmoji = false;
                    else
                        extent += getCharAdvance(c);
            }

            if (extent >= x) {
                break;
            }

            ++i;
        }

        if (i < rowText.length()) {
            return charIndex + i;
        }

        // 没有字符包围 x
        return -1;
    }

    /**
     * 不公开以允许 {@link TouchNavigationMethod} 访问
     *
     * @return 当前文本行在视口内可以滚动到的最大 x 值。
     */
    int getMaxScrollX() {
        if (isWordWrap())
            return _leftOffset;
        else
            return Math.max(0,
                    _xExtent - getContentWidth() + _navMethod.getCaretBloat().right + _alphaWidth);
    }

    /**
     * 不是私有的，以便 TouchNavigationMethod 可以访问
     *
     * @return 可以滚动到的最大 y 值。
     */
    int getMaxScrollY() {
        return Math.max(0,
                _hDoc.getRowCount() * rowHeight() - getContentHeight() / 2 + _navMethod.getCaretBloat().bottom);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return _hDoc.getRowCount() * rowHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {
        // 委托给 EditorScroller 处理滚动
        _scroller.updateEditorScroll();
    }

    public final void smoothScrollTo(int x, int y) {
        _scroller.smoothScrollTo(x, y);
    }

    void flingScroll(int velocityX, int velocityY) {
        _scroller.fling(velocityX, velocityY);
        postInvalidate();
    }

    public boolean isFlingScrolling() {
        return !_scroller.isFinished();
    }

    public void stopFlingScrolling() {
        _scroller.forceFinished(); // 停止滑动操作
    }


    /**
     * 停止由 autoScrollCaret(int) 启动的自动滚动。
     * 不是私有的，以便 TouchNavigationMethod 可以访问
     */
    void stopAutoScrollCaret() {
        removeCallbacks(_scrollCaretDownTask);
        removeCallbacks(_scrollCaretUpTask);
        removeCallbacks(_scrollCaretLeftTask);
        removeCallbacks(_scrollCaretRightTask);
    }


    public int getCaretRow() {
        return _caretRow; // 获取光标所在行
    }

    public int getCaretPosition() {
        return _caretPosition; // 获取光标位置
    }

    /**
     * 将光标设置到位置 i，滚动到视图并无效化
     * 需要重绘的区域
     *
     * @param i 要设置的字符索引
     */
    public void moveCaret(int i) {
        _fieldController.moveCaret(i);
    }

    /**
     * 将光标向左移动一个位置，滚动到屏幕上，并
     * 无效化需要重绘的区域。
     * <p>
     * 如果光标已经在第一个字符上，则不会发生任何操作。
     */
    public void moveCaretLeft() {
        _fieldController.moveCaretLeft(false);
    }

    /**
     * 将光标向右移动一个位置，滚动到屏幕上，并
     * 无效化需要重绘的区域。
     * <p>
     * 如果光标已经在最后一个字符上，则不会发生任何操作。
     */
    public void moveCaretRight() {
        _fieldController.moveCaretRight(false);
    }

    /**
     * 将光标向下一行移动，滚动到屏幕上，并
     * 无效化需要重绘的区域。
     * <p>
     * 如果光标已经在最后一行，则不会发生任何操作。
     */
    public void moveCaretDown() {
        _fieldController.moveCaretDown();
    }

    /**
     * 将光标向上一行移动，滚动到屏幕上，并
     * 无效化需要重绘的区域。
     * <p>
     * 如果光标已经在第一行，则不会发生任何操作。
     */
    public void moveCaretUp() {
        _fieldController.moveCaretUp();
    }

    /**
     * 如果光标不在屏幕上，则将光标滚动到视图中
     */
    public void focusCaret() {
        makeCharVisible(_caretPosition);
    }

//---------------------------------------------------------------------
//------------------------- 文本选择 ----------------------------

    /**
     * @return charOffset 出现的列号
     */
    protected int getColumn(int charOffset) {
        int row = _hDoc.findRowNumber(charOffset);
        TextWarriorException.assertVerbose(row >= 0,
                "传递给 getColumn 的字符偏移无效");
        int firstCharOfRow = _hDoc.getRowOffset(row);
        return charOffset - firstCharOfRow;
    }

    protected boolean caretOnFirstRowOfFile() {
        return (_caretRow == 0); // 判断光标是否在文件的第一行
    }

    protected boolean caretOnLastRowOfFile() {
        return (_caretRow == (_hDoc.getRowCount() - 1)); // 判断光标是否在文件的最后一行
    }

    protected boolean caretOnEOF() {
        return (_caretPosition == (_hDoc.docLength() - 1)); // 判断光标是否在文件末尾
    }

    public final boolean isSelectText() {
        return _fieldController.isSelectText(); // 检查是否处于选择文本模式
    }

    public final boolean isSelectText2() {
        return _fieldController.isSelectText2(); // 检查是否处于第二种选择文本模式
    }

    /**
     * 进入或退出选择模式。
     * 无效化需要重绘的区域。
     *
     * @param mode 如果为 true，进入选择模式；否则退出选择模式
     */
    public void selectText(boolean mode) {
        if (_fieldController.isSelectText() && !mode) {
            invalidateSelectionRows();  // 退出选择模式
            _fieldController.setSelectText(false);
        } else if (!_fieldController.isSelectText() && mode) {
            invalidateCaretRow();  // 进入选择模式
            _fieldController.setSelectText(true);
        }
    }

    public void selectAll() {
        _fieldController.setSelectionRange(0, _hDoc.docLength() - 1, false, true); // 选择全部文本
    }

    public void setSelection(int beginPosition, int numChars) {
        _fieldController.setSelectionRange(beginPosition, numChars, true, false); // 设置选择范围
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        _fieldController.setSelectionRange(beginPosition, numChars, true, true); // 设置选择范围
    }

    public boolean inSelectionRange(int charOffset) {
        return _fieldController.inSelectionRange(charOffset); // 判断字符偏移是否在选择范围内
    }

    public int getSelectionStart() {
        if (_selectionAnchor < 0)
            return _caretPosition; // 返回选择起始位置
        else
            return _selectionAnchor;
    }

    public int getSelectionEnd() {
        if (_selectionEdge < 0)
            return _caretPosition; // 返回选择结束位置
        else
            return _selectionEdge;
    }

    public void focusSelectionStart() {
        _fieldController.focusSelection(true); // 聚焦选择的起始位置
    }

    public void focusSelectionEnd() {
        _fieldController.focusSelection(false); // 聚焦选择的结束位置
    }

    public void cut() {
        if (_selectionAnchor != _selectionEdge)
            _fieldController.cut(_clipboardManager); // 剪切选中的文本
    }

    public void copy() {
        if (_selectionAnchor != _selectionEdge)
            _fieldController.copy(_clipboardManager); // 复制选中的文本
        selectText(false); // 退出选择模式
    }


    public void paste() {
        // 检查是否有剪贴板内容
        if (_clipboardManager.hasPrimaryClip() &&
                Objects.requireNonNull(_clipboardManager.getPrimaryClipDescription())
                        .hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = Objects.requireNonNull(_clipboardManager
                    .getPrimaryClip()).getItemAt(0);
            CharSequence text = item.getText();
            if (text != null) {
                // 粘贴剪贴板内容
                _fieldController.paste(text.toString());
            }
        }
    }

    public void cut(ClipboardManager cb) {
        _fieldController.cut(cb); // 剪切选中的文本
    }

    public void copy(ClipboardManager cb) {
        _fieldController.copy(cb); // 复制选中的文本
    }

    public void paste(String text) {
        _fieldController.paste(text); // 使用指定文本进行粘贴
    }

    private boolean reachedNextSpan(int charIndex, Pair span) {
        return span != null && (charIndex == span.first); // 检查字符索引是否到达下一个跨度
    }

    public void respan() {
        _fieldController.determineSpans(); // 重新确定文本跨度
    }

    public void cancelSpanning() {
        _fieldController.cancelSpanning(); // 取消当前的跨度操作
    }

    /**
     * 设置文本使用新字体类型，如果需要，则滚动视图以显示光标，并无效化整个视图
     */
    public void setTypeface(Typeface typeface) {
        _defTypeface = typeface;
        _boldTypeface = Typeface.create(typeface, Typeface.BOLD);
        _italicTypeface = Typeface.create(typeface, Typeface.ITALIC);
        _brush.setTypeface(typeface);
        _brushLine.setTypeface(typeface);
        if (_hDoc.isWordWrap())
            _hDoc.analyzeWordWrap(); // 如果启用换行，分析换行
        _fieldController.updateCaretRow(); // 更新光标所在行
        if (!makeCharVisible(_caretPosition)) {
            invalidate(); // 如果光标不可见，则无效化视图
        }
    }

    public void setItalicTypeface(Typeface typeface) {
        _italicTypeface = typeface; // 设置斜体字体
    }

    public void setBoldTypeface(Typeface typeface) {
        _boldTypeface = typeface; // 设置粗体字体
    }

    public boolean isWordWrap() {
        return _hDoc.isWordWrap(); // 检查是否启用换行
    }

    public void setWordWrap(boolean enable) {
        _hDoc.setWordWrap(enable); // 设置换行
        if (enable) {
            _xExtent = 0; // 重置 x 方向的扩展
            scrollTo(0, 0); // 滚动到顶部
        }
        _fieldController.updateCaretRow(); // 更新光标所在行
        if (!makeCharVisible(_caretPosition)) {
            invalidate(); // 如果光标不可见，则无效化视图
        }
    }

    public float getZoom() {
        return _zoomFactor; // 获取当前缩放因子
    }

    /**
     * 设置文本大小为基础文本大小的倍数，如果需要，则滚动视图以显示光标，并无效化整个视图
     */
    public void setZoom(float factor) {
        if (factor <= 0.5 || factor >= 5 || factor == _zoomFactor) {
            return; // 如果因子不在有效范围内，则返回
        }
        _zoomFactor = factor; // 更新缩放因子
        int newSize = (int) (factor * BASE_TEXT_SIZE_PIXELS); // 计算新文本大小
        _brush.setTextSize(newSize);
        _brushLine.setTextSize(newSize);
        if (_hDoc.isWordWrap())
            _hDoc.analyzeWordWrap(); // 如果启用换行，分析换行
        _fieldController.updateCaretRow(); // 更新光标所在行
        _alphaWidth = (int) _brush.measureText("a"); // 计算字符"a"的宽度
        invalidate(); // 无效化视图以重绘
    }

    /**
     * 设置制表符的长度，如果需要，滚动视图以显示光标，并使整个视图无效
     *
     * @param spaceCount 制表符表示的空格数
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }
        _tabLength = spaceCount;
        if (_hDoc.isWordWrap())
            _hDoc.analyzeWordWrap();
        _fieldController.updateCaretRow();
        if (!makeCharVisible(_caretPosition)) {
            invalidate();
        }
    }

    /**
     * 启用/禁用自动缩进
     */
    public void setAutoIndent(boolean enable) {
        _isAutoIndent = enable;
    }

    /**
     * 启用/禁用自动补全
     */
    public void setAutoComplete(boolean enable) {
        _isAutoComplete = enable;
    }

    /**
     * 启用/禁用当前行的高亮显示。当前行也会被无效化
     */
    public void setHighlightCurrentRow(boolean enable) {
        _isHighlightRow = enable;
        invalidateCaretRow();
    }

    /**
     * 启用/禁用非打印字符（如空格、制表符和行结束符）的可见表示
     * 如果启用状态发生变化，则使视图无效
     */
    public void setNonPrintingCharVisibility(boolean enable) {
        if (enable ^ _showNonPrinting) {
            _showNonPrinting = enable;
            if (_hDoc.isWordWrap())
                _hDoc.analyzeWordWrap();
            _fieldController.updateCaretRow();
            if (!makeCharVisible(_caretPosition)) {
                invalidate();
            }
        }
    }

//---------------------------------------------------------------------
//------------------------- 事件处理程序 ----------------------------

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 让触摸导航方法首先拦截按键事件
        if (_navMethod.onKeyDown(keyCode, event)) {
            return true;
        }

        // 检查是否为方向键或符号键
        if (KeysInterpreter.isNavigationKey(event)) {
            if (event.isShiftPressed() && !isSelectText()) {
                invalidateCaretRow();
                _fieldController.setSelectText(true);
            } else if (!event.isShiftPressed() && isSelectText()) {
                invalidateSelectionRows();
                _fieldController.setSelectText(false);
            }

            // 根据按下的导航键执行相应的光标移动操作
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:  // 光标向右移动
                    _fieldController.moveCaretRight(false);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:  // 光标向左移动
                    _fieldController.moveCaretLeft(false);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:  // 光标向下移动
                    _fieldController.moveCaretDown();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP: // 光标向上移动
                    _fieldController.moveCaretUp();
                    break;
                default:
                    break;
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SYM ||
                keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            // 如果按下符号键或字符选择对话框输入键，显示字符选择器
            showCharacterPicker(
                    PICKER_SETS.get(KeyCharacterMap.PICKER_DIALOG_INPUT), false);
            return true;
        }

        // 检查字符是否可打印
        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == Language.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }


        // 处理按键事件
        int repeatCount = event.getRepeatCount();
        boolean isPrintableChar = (repeatCount == 1) ||
                (repeatCount == 0 &&
                        (_isLongPressCaps && !Character.isLowerCase(c) ||
                                !_isLongPressCaps && PICKER_SETS.get(c) == null));

        if (isPrintableChar) {
            _fieldController.onPrintableChar(c);
        }

        return true;
    }


    /**
     * @param candidates 用户可以选择的字符字符串
     * @param replace    如果为 true，则光标前的字符将被用户选择的字符替换。
     *                   如果为 false，用户选择的字符将插入到光标位置。
     */
    private void showCharacterPicker(String candidates, boolean replace) {
        final boolean shouldReplace = replace;
        final SpannableStringBuilder dummyString = new SpannableStringBuilder();
        Selection.setSelection(dummyString, 0);

        CharacterPickerDialog dialog = new CharacterPickerDialog(getContext(),
                this, dummyString, candidates, true);

        dialog.setOnDismissListener(dialog1 -> {
            if (dummyString.length() > 0) {
                if (shouldReplace) {
                    _fieldController.onPrintableChar(Language.BACKSPACE);
                }
                _fieldController.onPrintableChar(dummyString.charAt(0));
            }
        });
        dialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (_navMethod.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            _fieldController.moveCaretRight(false);
            --deltaX;
        }
        while (deltaX < 0) {
            _fieldController.moveCaretLeft(false);
            ++deltaX;
        }
        while (deltaY > 0) {
            _fieldController.moveCaretDown();
            --deltaY;
        }
        while (deltaY < 0) {
            _fieldController.moveCaretUp();
            ++deltaY;
        }
        return true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (_navMethod.onTouchEvent(event)) {
            requestFocus();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP && isPointInView((int)
                event.getX(), (int) event.getY())) {
            // 请求焦点以确保输入法能够正常工作
            requestFocus();
        }

        return true;
    }

    private boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
    }

    /**
     * 不公开以允许 {@link TouchNavigationMethod} 访问
     */
    void showIME() {
        InputMethodManager im = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        im.showSoftInput(this, 0);
    }

    /**
     * 一些导航方法使用传感器或有其小部件的状态。
     * 它们应该被通知应用程序生命周期事件，以便可以
     * 开始/停止感应并加载/存储它们的 GUI 状态。
     */
    void onPause() {
        _navMethod.onPause();
    }

    void onResume() {
        _navMethod.onResume();
    }

    void onDestroy() {
        _fieldController.cancelSpanning();
    }

    //*********************************************************************
    // ************************ 控制器逻辑 ***************************
    // *********************************************************************
    private class TextFieldController
            implements Lexer.LexCallback {
        private final Lexer _lexer = new Lexer(this);
        private boolean _isInSelectionMode = false;
        private boolean _isInSelectionMode2;

        /**
         * 分析文本中的编程语言关键字，并在完成时重新绘制文本视图。
         * 全局编程语言通过静态方法 Lexer.setLanguage(Language) 设置
         * <p>
         * 如果 Lexer 语言不是编程语言，则不执行任何操作
         */
        public void determineSpans() {
            isShowRegion = false;
            _lexer.tokenize(_hDoc);
        }

        public void cancelSpanning() {
            isShowRegion = true;
            _lexer.cancelTokenize();
        }

        @Override
        // 通常从非 UI 线程调用
        public void lexDone(final List<Pair> results) {
            post(() -> {
                isShowRegion = true;
                _hDoc.setSpans(results);
                invalidate();
            });
        }


        //---------------------------- 按键事件 ----------------------------

        public void onPrintableChar(char c) {
            isShowRegion = false;

            // 删除选中文本
            boolean selectionDeleted = false;
            if (_isInSelectionMode) {
                selectionDelete();
                selectionDeleted = true;
            }

            int originalRow = _caretRow;
            int originalOffset = _hDoc.getRowOffset(originalRow);

            switch (c) {
                case Language.BACKSPACE:
                    if (selectionDeleted) {
                        break;
                    }

                    if (_caretPosition > 0) {
                        // 删除选中文本
                        _textLis.onDel(c + "", _caretPosition, 1);

                        // 删除单个字符
                        _hDoc.deleteAt(_caretPosition - 1, System.nanoTime());
                        if (isSurrogatePair(_caretPosition - 2)) {
                            _hDoc.deleteAt(_caretPosition - 2, System.nanoTime());
                            moveCaretLeft(true);
                        }

                        moveCaretLeft(true);

                        // 如果行发生变化，则无效化视图
                        if (_caretRow < originalRow || (_hDoc.isWordWrap() && originalOffset != _hDoc.getRowOffset(originalRow))) {
                            invalidateFromRow(Math.min(originalRow, _caretRow));
                        }
                    }
                    break;

                case Language.NEWLINE:
                    char[] indent = _isAutoIndent ? createAutoIndent() : new char[]{c};
                    _hDoc.insertBefore(indent, _caretPosition, System.nanoTime());
                    moveCaret(_caretPosition + indent.length);

                    if (_hDoc.isWordWrap() && originalOffset != _hDoc.getRowOffset(originalRow)) {
                        --originalRow;
                    }


                    // 通知文本监听器
                    _textLis.onNewLine(c + "", _caretPosition, 1);

                    invalidateFromRow(originalRow);
                    break;

                default:
                    _hDoc.insertBefore(c, _caretPosition, System.nanoTime());
                    moveCaretRight(true);

                    _textLis.onAdd(c + "", _caretPosition, 1);

                    if (_hDoc.isWordWrap() && originalOffset != _hDoc.getRowOffset(originalRow)) {
                        invalidateFromRow(originalRow);
                    }
                    break;
            }

            setEdited(true);
            determineSpans();
        }

        /**
         * 判断指定位置是否为代理对（Surrogate Pair）。
         */
        private boolean isSurrogatePair(int position) {
            return position >= 0 &&
                    Character.isHighSurrogate(_hDoc.charAt(position)) &&
                    Character.isLowSurrogate(_hDoc.charAt(position + 1));
        }

        /**
         * 返回一个 char[]，第一个元素是换行符，后跟光标所在行的
         * 前导空格和制表符
         */

        // 创建自动缩进
        private char[] createAutoIndent() {
            int lineNum = _hDoc.findLineNumber(_caretPosition);
            int startOfLine = _hDoc.getLineOffset(lineNum);
            int whitespaceCount = 0;
            _hDoc.seekChar(startOfLine);
            while (_hDoc.hasNext()) {
                char c = _hDoc.next();
                if ((c != ' ' && c != Language.TAB) || startOfLine + whitespaceCount >= _caretPosition) {
                    break;
                }
                ++whitespaceCount;
            }

            whitespaceCount += _autoIndentWidth * Lexer.getFormatter().createAutoIndent(_hDoc.subSequence(startOfLine, _caretPosition - startOfLine));
            if (whitespaceCount < 0)
                return new char[]{Language.NEWLINE};

            char[] indent = new char[1 + whitespaceCount];
            indent[0] = Language.NEWLINE;

            _hDoc.seekChar(startOfLine);
            for (int i = 0; i < whitespaceCount; ++i) {
                indent[1 + i] = DefFormatter.indentChar;
            }
            return indent;
        }

        // 删除选中文本
        public void moveCaretDown() {
            if (!caretOnLastRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow + 1;
                int currColumn = getColumn(currCaret);
                int currRowLength = _hDoc.getRowSize(currRow);
                int newRowLength = _hDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // 在旧行的同一列上定位。
                    _caretPosition += currRowLength;
                } else {
                    // 列在新行中不存在（新行太短）。
                    // 在新行末尾定位。
                    _caretPosition += currRowLength - currColumn + newRowLength - 1;
                }
                ++_caretRow;

                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(currRow, newRow + 1);
                }
                _rowLis.onRowChanged(newRow);
                stopTextComposing();
            }
        }

        // 移动光标到上一行
        public void moveCaretUp() {
            if (!caretOnFirstRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow - 1;
                int currColumn = getColumn(currCaret);
                int newRowLength = _hDoc.getRowSize(newRow);

                if (currColumn < newRowLength) {
                    // 在旧行的同一列上定位。
                    _caretPosition -= newRowLength;
                } else {
                    // 列在新行中不存在（新行太短）。
                    // 在新行末尾定位。
                    _caretPosition -= (currColumn + 1);
                }
                --_caretRow;

                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(newRow, currRow + 1);
                }
                _rowLis.onRowChanged(newRow);
                stopTextComposing();
            }
        }

        /**
         * @param isTyping 是否因输入文本而将光标移动到连续位置
         */

        // 向右移动光标
        public void moveCaretRight(boolean isTyping) {
            if (!caretOnEOF()) {
                int originalRow = _caretRow;
                ++_caretPosition;
                updateCaretRow();
                updateSelectionRange(_caretPosition - 1, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(originalRow, _caretRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        /**
         * 向左移动光标
         */
        public void moveCaretLeft(boolean isTyping) {
            if (_caretPosition > 0) {
                int originalRow = _caretRow;
                --_caretPosition;
                updateCaretRow();
                updateSelectionRange(_caretPosition + 1, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(_caretRow, originalRow + 1);
                }

                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        // 移动光标
        public void moveCaret(int i) {
            if (i < 0 || i >= _hDoc.docLength()) {
                TextWarriorException.fail("光标位置无效");
                return;
            }
            updateSelectionRange(_caretPosition, i);

            _caretPosition = i;
            updateAfterCaretJump();
        }

        // 更新光标位置
        private void updateAfterCaretJump() {
            int oldRow = _caretRow;
            updateCaretRow();
            if (!makeCharVisible(_caretPosition)) {
                invalidateRows(oldRow, oldRow + 1); // 旧光标行
                invalidateCaretRow();
            }
            stopTextComposing();
        }

        /**
         * 该辅助方法仅应由内部方法在设置 _caretPosition 后使用，以重新计算光标所在的新行。
         */
        // 更新光标所在行
        void updateCaretRow() {
            int newRow = _hDoc.findRowNumber(_caretPosition);
            if (_caretRow != newRow) {
                _caretRow = newRow;
                _rowLis.onRowChanged(newRow);
            }
        }


        // 无效化需要重绘的区域
        public void stopTextComposing() {
            InputMethodManager im = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            im.restartInput(FreeScrollingTextField.this);

            if (_inputConnection != null && _inputConnection.isComposingStarted()) {
                _inputConnection.resetComposingState();
            }
        }

        //-------------------------- 选择模式 ---------------------------
        public final boolean isSelectText() {
            return _isInSelectionMode;
        }

        /**
         * 进入或退出选择模式。
         * 不会使视图失效。
         *
         * @param mode 如果为 true，则进入选择模式；否则退出选择模式
         */
        // 选择文本
        public void setSelectText(boolean mode) {
            if (mode == _isInSelectionMode) {
                return;
            }

            if (mode) {
                _selectionAnchor = _caretPosition;
                _selectionEdge = _caretPosition;
            } else {
                _selectionAnchor = -1;
                _selectionEdge = -1;
            }
            _isInSelectionMode = mode;
            _isInSelectionMode2 = mode;
            _selModeLis.onSelectionChanged(mode, getSelectionStart(), getSelectionEnd());
        }

        // 选择文本
        public final boolean isSelectText2() {
            return _isInSelectionMode2;
        }

        // 选择文本
        public boolean inSelectionRange(int charOffset) {
            if (_selectionAnchor < 0) {
                return false;
            }

            return (_selectionAnchor <= charOffset &&
                    charOffset < _selectionEdge);
        }

        // 选择文本
        public void setSelectionRange(int beginPosition, int numChars,
                                      boolean scrollToStart, boolean mode) {
            TextWarriorException.assertVerbose(
                    (beginPosition >= 0) && numChars <= (_hDoc.docLength() - 1) && numChars >= 0,
                    "选择范围无效");

            if (_isInSelectionMode) {
                // 取消高亮之前的选择
                invalidateSelectionRows();
            } else {
                // 取消高亮光标
                invalidateCaretRow();
                if (mode)
                    setSelectText(true);
                else
                    _isInSelectionMode = true;
            }

            _selectionAnchor = beginPosition;
            _selectionEdge = _selectionAnchor + numChars;

            _caretPosition = _selectionEdge;
            stopTextComposing();
            updateCaretRow();
            if (mode)
                _selModeLis.onSelectionChanged(isSelectText(), _selectionAnchor, _selectionEdge);
            boolean scrolled = makeCharVisible(_selectionEdge);
            if (scrollToStart) {
                scrolled = makeCharVisible(_selectionAnchor);
            }

            if (!scrolled) {
                invalidateSelectionRows();
            }
        }

        /**
         * 将光标移动到选中文本的一端并滚动到视图中。
         *
         * @param start 如果为 true，则将光标移动到选择的开始位置。
         *              否则，将光标移动到选择的结束位置。
         *              在所有情况下，如果光标不可见，则光标将滚动到视图中。
         */

        // 聚焦选择的位置
        public void focusSelection(boolean start) {
            if (_isInSelectionMode) {
                if (start && _caretPosition != _selectionAnchor) {
                    _caretPosition = _selectionAnchor;
                    updateAfterCaretJump();
                } else if (!start && _caretPosition != _selectionEdge) {
                    _caretPosition = _selectionEdge;
                    updateAfterCaretJump();
                }
            }
        }

        /**
         * 用于内部方法在设置新光标位置时更新选择边界。
         * 如果不在选择模式下，则什么都不做。
         */

        // 更新选择范围
        private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {
            if (!_isInSelectionMode) {
                return;
            }

            if (oldCaretPosition < _selectionEdge) {
                if (newCaretPosition > _selectionEdge) {
                    _selectionAnchor = _selectionEdge;
                    _selectionEdge = newCaretPosition;
                } else {
                    _selectionAnchor = newCaretPosition;
                }

            } else {
                if (newCaretPosition < _selectionAnchor) {
                    _selectionEdge = _selectionAnchor;
                    _selectionAnchor = newCaretPosition;
                } else {
                    _selectionEdge = newCaretPosition;
                }
            }
        }

//------------------------ 剪切、复制、粘贴 ---------------------------

        /**
         * 方便的连续剪切和粘贴调用方法
         */
        public void cut(ClipboardManager cb) {
            copy(cb);
            selectionDelete();
        }

        /**
         * 将选中的文本复制到剪贴板。
         * <p>
         * 如果不在选择模式下，则不执行任何操作。
         */
        public void copy(ClipboardManager cb) {
            if (_isInSelectionMode &&
                    _selectionAnchor < _selectionEdge) {
                CharSequence contents = _hDoc.subSequence(_selectionAnchor,
                        _selectionEdge - _selectionAnchor);

                // 创建新的剪贴板数据并设置到剪贴板
                ClipData clip = ClipData.newPlainText("selected_text", contents);
                cb.setPrimaryClip(clip); // 将剪贴板数据放入剪贴板
            }
        }

        /**
         * 在光标位置插入文本。
         * 将删除现有选中的文本并结束选择模式。
         * 将使删除区域失效。
         * <p>
         * 插入后，将使插入区域失效。
         */
        public void paste(String text) {
            if (text == null) {
                return;
            }

            _hDoc.beginBatchEdit();
            selectionDelete();

            int originalRow = _caretRow;
            int originalOffset = _hDoc.getRowOffset(originalRow);
            _hDoc.insertBefore(text.toCharArray(), _caretPosition, System.nanoTime());
            //_textLis.onAdd(text, _caretPosition, text.length());
            _hDoc.endBatchEdit();

            _caretPosition += text.length();
            updateCaretRow();

            setEdited(true);
            determineSpans();
            stopTextComposing();

            if (!makeCharVisible(_caretPosition)) {
                int invalidateStartRow = originalRow;
                // 如果行包裹发生变化，则也使之前的行失效
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (originalRow == _caretRow && !_hDoc.isWordWrap()) {
                    // 粘贴文本仅影响光标行
                    invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                } else {
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * 删除选中的文本，退出选择模式并使删除区域失效。
         * 如果选定的范围为空，则该方法退出选择模式并使光标失效。
         * 如果不在选择模式下，则不执行任何操作。
         */

        // 删除选中的文本
        public void selectionDelete() {
            // 如果未处于选中模式，直接返回
            if (!_isInSelectionMode) {
                return;
            }

            // 计算选中字符数
            int totalChars = _selectionEdge - _selectionAnchor;

            // 如果选中字符数小于等于 0，则直接取消选中状态并返回
            if (totalChars <= 0) {
                setSelectText(false);
                invalidateCaretRow();
                return;
            }

            // 获取原始行号和偏移量
            int originalRow = _hDoc.findRowNumber(_selectionAnchor);
            int originalOffset = _hDoc.getRowOffset(originalRow);
            boolean isSingleRowSel = _hDoc.findRowNumber(_selectionEdge) == originalRow;

            // 通知监听器文本删除事件
            _textLis.onDel("", _caretPosition, totalChars);

            // 删除选中内容
            _hDoc.beginBatchEdit(); // 开始批处理
            _hDoc.deleteAt(_selectionAnchor, totalChars, System.nanoTime());
            _hDoc.endBatchEdit(); // 结束批处理

            // 更新光标位置
            _caretPosition = _selectionAnchor;
            updateCaretRow();

            // 标记文档已编辑，并重新计算跨度
            setEdited(true);
            determineSpans();

            // 取消选中模式
            setSelectText(false);
            stopTextComposing();

            // 确保光标可见并更新视图
            if (!makeCharVisible(_caretPosition)) {
                int invalidateStartRow = originalRow;

                // 如果行包裹发生变化，则使之前的行失效
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (isSingleRowSel) {
                    // 如果选中内容在同一行，更新该行的可见区域
                    invalidateRows(originalRow, originalRow + 1);
                } else {
                    // 如果选中多行，从起始行开始刷新
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        void replaceText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            // 删除选择的文本
            if (_isInSelectionMode) {
                invalidateStartRow = _hDoc.findRowNumber(_selectionAnchor);
                originalOffset = _hDoc.getRowOffset(invalidateStartRow);

                int totalChars = _selectionEdge - _selectionAnchor;

                if (totalChars > 0) {
                    _caretPosition = _selectionAnchor;
                    _hDoc.deleteAt(_selectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != _caretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = _caretRow;
                originalOffset = _hDoc.getRowOffset(_caretRow);
            }

            // 删除请求的字符
            if (charCount > 0) {
                int delFromRow = _hDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = _hDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != _caretRow) {
                    isInvalidateSingleRow = false;
                }

                _caretPosition = from;
                _hDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            // 插入文本
            if (text != null && !text.isEmpty()) {
                int insFromRow = _hDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = _hDoc.getRowOffset(insFromRow);
                }

                _hDoc.insertBefore(text.toCharArray(), _caretPosition, System.nanoTime());
                _caretPosition += text.length();
                dirty = true;
            }

            // 通知文本监听器
            _textLis.onAdd(text, _caretPosition, text.length() - charCount);
            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = _caretRow;
            updateCaretRow();
            if (originalRow != _caretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(_caretPosition)) {
                // 如果行包裹发生变化，则也使之前的行失效
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !_hDoc.isWordWrap()) {
                    // 替换文本仅影响当前行
                    invalidateRows(_caretRow, _caretRow + 1);
                } else {
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

//----------------- 输入连接的辅助方法 ----------------

        /**
         * 删除现有选中的文本，然后从指定位置开始删除 charCount 数量的字符，并在其位置插入文本。
         * 与粘贴或选择删除不同，不会向输入法信号结束文本组成。
         */
        void replaceComposingText(int from, int charCount, String text) {
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;

            // 删除选择的文本
            if (_isInSelectionMode) {
                invalidateStartRow = _hDoc.findRowNumber(_selectionAnchor);
                originalOffset = _hDoc.getRowOffset(invalidateStartRow);

                int totalChars = _selectionEdge - _selectionAnchor;

                if (totalChars > 0) {
                    _caretPosition = _selectionAnchor;
                    _hDoc.deleteAt(_selectionAnchor, totalChars, System.nanoTime());

                    if (invalidateStartRow != _caretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }

                setSelectText(false);
            } else {
                invalidateStartRow = _caretRow;
                originalOffset = _hDoc.getRowOffset(_caretRow);
            }

            // 删除请求的字符
            if (charCount > 0) {
                int delFromRow = _hDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = _hDoc.getRowOffset(delFromRow);
                }

                if (invalidateStartRow != _caretRow) {
                    isInvalidateSingleRow = false;
                }

                _caretPosition = from;
                _hDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }

            // 插入文本
            if (text != null && !text.isEmpty()) {
                int insFromRow = _hDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = _hDoc.getRowOffset(insFromRow);
                }

                _hDoc.insertBefore(text.toCharArray(), _caretPosition, System.nanoTime());
                _caretPosition += text.length();
                dirty = true;
            }

            // 通知文本监听器
            _textLis.onAdd(text, _caretPosition, text.length() - charCount);
            if (dirty) {
                setEdited(true);
                determineSpans();
            }

            int originalRow = _caretRow;
            updateCaretRow();
            if (originalRow != _caretRow) {
                isInvalidateSingleRow = false;
            }

            if (!makeCharVisible(_caretPosition)) {
                // 如果行包裹发生变化，则也使之前的行失效
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }

                if (isInvalidateSingleRow && !_hDoc.isWordWrap()) {
                    // 替换文本仅影响当前行
                    invalidateRows(_caretRow, _caretRow + 1);
                } else {
                    // TODO 仅使受影响的行失效
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * 删除当前光标位置之前的 leftLength 个字符和之后的 rightLength 个字符。
         * <p>
         * 与粘贴或选择删除不同，该方法不会向 IME 发出文本组合结束的信号。
         */
        void deleteAroundComposingText(int left, int right) {
            int start = _caretPosition - left;
            if (start < 0) {
                start = 0;
            }
            int end = _caretPosition + right;
            int docLength = _hDoc.docLength();
            if (end > (docLength - 1)) { //排除结束符 EOF
                end = docLength - 1;
            }
            replaceComposingText(start, end - start, "");
        }

        String getTextAfterCursor(int maxLen) {
            int docLength = _hDoc.docLength();
            if ((_caretPosition + maxLen) > (docLength - 1)) {
                //排除结束符 EOF
                return _hDoc.subSequence(_caretPosition, docLength - _caretPosition - 1).toString();
            }

            return _hDoc.subSequence(_caretPosition, maxLen).toString();
        }

        String getTextBeforeCursor(int maxLen) {
            int start = _caretPosition - maxLen;
            if (start < 0) {
                start = 0;
            }
            return _hDoc.subSequence(start, _caretPosition - start).toString();
        }
    }


    //*********************************************************************
    // ************************** InputConnection **************************
    // *********************************************************************
    private class TextFieldInputConnection extends BaseInputConnection {
        private boolean _isComposing = false;
        private int _composingCharCount = 0;

        public TextFieldInputConnection(FreeScrollingTextField v) {
            super(v, true);
        }

        public void resetComposingState() {
            _composingCharCount = 0;
            _isComposing = false;
            _hDoc.endBatchEdit();
        }


        @Override
        public boolean performContextMenuAction(int id) {
            return switch (id) {
                case android.R.id.selectAll -> {
                    selectAll();
                    yield true;
                }
                case android.R.id.cut -> {
                    cut();
                    yield true;
                }
                case android.R.id.pasteAsPlainText -> {
                    paste();
                    yield true;
                }
                case android.R.id.copy -> {
                    copy();
                    yield true;
                }
                default -> false;
            };
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                    selectText(!isSelectText());
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    moveCaretLeft();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveCaretUp();
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    moveCaretRight();
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveCaretDown();
                    break;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    moveCaret(0);
                    break;
                case KeyEvent.KEYCODE_MOVE_END:
                    moveCaret(_hDoc.length());
                    break;
                case KeyEvent.KEYCODE_ESCAPE:
                    if (isSelectText()) {
                        selectText(false);
                    }
                    break;
                default:
                    return super.sendKeyEvent(event);
            }
            return true;
        }

        /**
         * 只有在 InputConnection 尚未被 IME 使用时才为真。
         * 可以通过 resetComposingState() 方法程序化清除。
         */
        public boolean isComposingStarted() {
            return _isComposing;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            if (!_hDoc.isBatchEdit()) {
                _hDoc.beginBatchEdit();
            }

            _fieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = text.length();

            if (newCursorPosition > 1) {
                _fieldController.moveCaret(_caretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                _fieldController.moveCaret(_caretPosition - text.length() - newCursorPosition);
            }
            return true;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            _fieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = 0;
            _hDoc.endBatchEdit();

            if (newCursorPosition > 1) {
                _fieldController.moveCaret(_caretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                _fieldController.moveCaret(_caretPosition - text.length() - newCursorPosition);
            }
            _isComposing = false;
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            if (_composingCharCount != 0) {
                Log.i("lua",
                        "警告: InputConnection.deleteSurroundingText 的实现" +
                                " 将不会跳过组合文本");
            }

            _fieldController.deleteAroundComposingText(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            resetComposingState();
            return true;
        }

        @Override
        public int getCursorCapsMode(int reqModes) {
            int capsMode = 0;

            // 忽略 InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS；在 TextWarrior 中未使用

            if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                    == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
                int prevChar = _caretPosition - 1;
                if (prevChar < 0 || Lexer.getLanguage().isWhitespace(_hDoc.charAt(prevChar))) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                    //如果客户端对此感兴趣，则设置 CAP_SENTENCES
                    if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                        capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                    }
                }
            }

            // 奇怪的是，Android 软件键盘即使有兴趣进行自动大写，也不会在 reqModes 中设置 TYPE_TEXT_FLAG_CAP_SENTENCES。
            // Android bug？因此，我们假设 TYPE_TEXT_FLAG_CAP_SENTENCES
            // 始终设置为确保安全。
            else {
                Language lang = Lexer.getLanguage();

                int prevChar = _caretPosition - 1;
                int whitespaceCount = 0;
                boolean capsOn = true;

                // 仅在句子的第一个字符处开启大写模式。
                // 新的一行也被视为开始新句子。
                // 紧接句点之后的位置被视为小写。
                // 示例: "abc.com" 但 "abc. Com"
                while (prevChar >= 0) {
                    char c = _hDoc.charAt(prevChar);
                    if (c == Language.NEWLINE) {
                        break;
                    }

                    if (!lang.isWhitespace(c)) {
                        if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                            capsOn = false;
                        }
                        break;
                    }

                    ++whitespaceCount;
                    --prevChar;
                }

                if (capsOn) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }

            return capsMode;
        }

        @Override
        public CharSequence getTextAfterCursor(int maxLen, int flags) {
            return _fieldController.getTextAfterCursor(maxLen); //忽略标志
        }

        @Override
        public CharSequence getTextBeforeCursor(int maxLen, int flags) {
            return _fieldController.getTextBeforeCursor(maxLen); //忽略标志
        }

        @Override
        public boolean setSelection(int start, int end) {
            if (start == end) {
                _fieldController.moveCaret(start);
            } else {
                _fieldController.setSelectionRange(start, end - start, false, true);
            }
            return true;
        }

        @Override
        public boolean reportFullscreenMode(boolean enabled) {
            return false;
        }
    }
}

