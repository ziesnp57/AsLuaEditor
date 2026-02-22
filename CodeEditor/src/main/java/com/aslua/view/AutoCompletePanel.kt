package com.aslua.view

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ListPopupWindow
import android.widget.TextView
import com.aslua.Flag
import com.aslua.Lexer
import com.aslua.language.Language
import com.aslua.language.LanguageNonProg.Companion.getInstance
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Locale

class AutoCompletePanel(textField: FreeScrollingTextField) {
    private var _textField: WeakReference<FreeScrollingTextField> = WeakReference(textField)
    private var _context: WeakReference<Context> = WeakReference(textField.context)
    private var _autoCompletePanel: ListPopupWindow? = null // 自动完成面板
    private var _adapter: MyAdapter? = null // 自定义适配器
    private var _filter: Filter? = null // 过滤器
    private var _verticalOffset = 0 // 垂直偏移量
    private var _height = 0 // 面板高度
    private var _horizontal = 0 // 水平偏移量
    private var _constraint: CharSequence? = null // 当前约束条件
    private var gd: GradientDrawable? = null // 背景渐变绘制
    private var _textColor = 0 // 文本颜色
    private var isDestroyed = false

    init {
        initAutoCompletePanel() // 初始化自动完成面板
    }

    // 设置文本颜色
    fun setTextColor(color: Int) {
        _textColor = color
        gd?.setStroke(1, color) // 设置描边颜色
        _autoCompletePanel?.setBackgroundDrawable(gd) // 设置背景
    }

    // 设置背景颜色
    fun setBackgroundColor(color: Int) {
        gd?.setColor(color) // 设置背景颜色
        _autoCompletePanel?.setBackgroundDrawable(gd) // 应用背景
    }

    // 设置背景 drawable
    fun setBackground(color: Drawable?) {
        _autoCompletePanel?.setBackgroundDrawable(color)
    }

    // 初始化自动完成面板
    @SuppressLint("ResourceType")
    private fun initAutoCompletePanel() {
        _autoCompletePanel = ListPopupWindow(_context.get()!!) // 创建弹出窗口
        _autoCompletePanel?.anchorView = _textField.get() // 设置锚点为文本框
        _adapter = MyAdapter(_context.get()!!, R.layout.simple_list_item_1) // 创建适配器
        _autoCompletePanel?.setAdapter(_adapter) // 设置适配器
        _filter = _adapter?.filter // 获取过滤器
        setHeight(300) // 设置默认高度

        // 获取主题中的背景色和文本色
        val array = _context.get()!!.theme.obtainStyledAttributes(
            intArrayOf(
                R.attr.colorBackground,
                R.attr.textColorPrimary,
            )
        )
        val backgroundColor = array.getColor(0, 0xFF00FF)
        val textColor = array.getColor(1, 0xFF00FF)
        array.recycle() // 释放数组

        // 创建渐变绘制并设置颜色
        gd = GradientDrawable()
        gd?.setColor(backgroundColor)
        gd?.cornerRadius = 16f // 设置圆角
        gd?.setStroke(2, textColor) // 设置描边
        setTextColor(textColor) // 设置文本颜色
        _autoCompletePanel?.setBackgroundDrawable(gd) // 应用背景

        // 设置点击项监听器
        _autoCompletePanel?.setOnItemClickListener(OnItemClickListener { p1: AdapterView<*>, p2: View, p3: Int, p4: Long ->

            _constraint?.let {
                _textField.get()?.replaceText(
                    _textField.get()!!.caretPosition - it.length,
                    it.length,
                    (p2 as TextView).text.toString()
                )
            }
            _adapter?.abort() // 终止适配器
            dismiss() // 关闭自动完成面板
        })
    }

    // 设置面板宽度
    fun setWidth(width: Int) {
        if (_autoCompletePanel?.width != width) {
            _autoCompletePanel?.width = width
        }
    }

    // 设置面板高度
    private fun setHeight(height: Int) {
        if (_height != height) {
            _height = height
            _autoCompletePanel?.height = height // 应用高度
        }
    }

    // 设置水平偏移量
    private fun setHorizontalOffset(horizontal: Int) {
        var horizontal = horizontal
        horizontal = minOf(horizontal, (_textField.get()!!.width / 2)) // 限制最大值
        if (_horizontal != horizontal) {
            _horizontal = horizontal
            _autoCompletePanel?.let { panel ->
                panel.horizontalOffset = horizontal
            }
        }
    }

    // 设置垂直偏移量
    private fun setVerticalOffset(verticalOffset: Int) {
        var verticalOffset = verticalOffset
        val max = - _autoCompletePanel!!.height // 最大值
        if (verticalOffset > max) {
            _textField.get()?.scrollBy(0, verticalOffset - max) // 滚动文本框
            verticalOffset = max // 更新偏移量
        }
        if (_verticalOffset != verticalOffset) {
            _verticalOffset = verticalOffset
            _autoCompletePanel!!.verticalOffset = verticalOffset // 应用偏移量
        }
    }

    // 更新过滤条件
    fun update(constraint: CharSequence) {
        _constraint = constraint // 更新约束条件
        _adapter?.restart() // 重启适配器
        _filter?.filter(constraint) // 应用过滤
    }

    // 显示自动完成面板
    fun show() {
        if (!_autoCompletePanel!!.isShowing) _autoCompletePanel!!.show() // 显示面板
        _autoCompletePanel?.listView?.setFadingEdgeLength(0) // 设置消隐边缘长度
    }

    // 关闭自动完成面板
    fun dismiss() {
        if (_autoCompletePanel!!.isShowing) {
            _autoCompletePanel?.dismiss()
        }
    }

    // 销毁面板，释放资源
    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        
        dismiss()
        _autoCompletePanel?.apply {
            setAdapter(null)
            setOnItemClickListener(null)
        }
        _autoCompletePanel = null
        _adapter?.clear()
        _adapter = null
        _filter = null
        gd = null
        _context.clear()
        _textField.clear()
    }

    /**
     * 自定义适配器定义
     */
    internal inner class MyAdapter(context: Context, resource: Int) :
        ArrayAdapter<String>(context, resource), Filterable {
        private var _h = 0 // 项目高度
        private val _abort: Flag = Flag() // 终止标志

        // 初始化终止标志
        private val dm: DisplayMetrics? // 显示参数

        init {
            setNotifyOnChange(false) // 禁止自动通知更改
            dm = context.resources.displayMetrics // 获取显示参数
        }

        // 终止适配器
        fun abort() {
            _abort.set()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView // 获取视图
            view.setTextColor(_textColor) // 设置文本颜色
            return view
        }

        // 重启适配器
        fun restart() {
            _abort.clear()
        }

        // 获取项目高度
        fun getItemHeight(): Int {
            if (_h != 0) return _h
            val inflater = LayoutInflater.from(context)
            @SuppressLint("InflateParams")
            val item = inflater.inflate(R.layout.simple_list_item_1, null)
            val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            item.measure(widthSpec, heightSpec) // 测量高度
            _h = item.measuredHeight // 获取高度
            return _h // 返回高度
        }

        /**
         * 实现自动完成的过滤算法
         */
        override fun getFilter(): Filter {
            return object : Filter() {
                /**
                 * 本方法在后台线程执行，定义过滤算法
                 */
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val buf = ArrayList<String>() // 存储过滤结果
                    var keyword = constraint.toString().lowercase(Locale.getDefault()) // 转小写
                    val ss = keyword.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray() // 按照点分割

                    if (ss.size == 2) {
                        val pkg: String = ss[0]
                        keyword = ss[1]
                        if (_globalLanguage.isBasePackage(pkg)) {
                            val keywords = _globalLanguage.getBasePackage(pkg)
                            for (k in keywords) {
                                if (k.indexOf(keyword) == 0)
                                    buf.add(k)
                            }
                        }
                    } else if (ss.size == 1) {
                        if (keyword[keyword.length - 1] == '.') {
                            val pkg = keyword.substring(0, keyword.length - 1)
                            keyword = ""
                            if (_globalLanguage.isBasePackage(pkg)) {
                                val keywords = _globalLanguage.getBasePackage(pkg)
                                buf.addAll(listOf<String>(*keywords)) // 添加所有关键字
                            }
                        } else {
                            val keywords = _globalLanguage.userWord
                            for (k in keywords) {
                                if (k.indexOf(keyword) == 0)
                                    buf.add(k)
                            }

                            for (k in _globalLanguage.keywords) {
                                if (k.indexOf(keyword) == 0)
                                    buf.add(k)
                            }

                            for (k in _globalLanguage.names) {
                                if (k.indexOf(keyword) == 0)
                                    buf.add(k)
                            }
                        }
                    }

                    _constraint = keyword // 更新约束条件
                    val results = FilterResults() // 过滤结果
                    results.values = buf // 存储值
                    results.count = buf.size // 存储数量
                    return results // 返回结果
                }

                /**
                 * 本方法在UI线程执行，用于更新自动完成列表
                 */
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    if (results != null && results.count > 0 && !_abort.isSet()) {
                        // 有过滤结果，显示自动完成列表
                        this@MyAdapter.clear() // 清空旧列表
                        val values = results.values
                        if (values is ArrayList<*>) {
                            @Suppress("UNCHECKED_CAST")
                            this@MyAdapter.addAll(values as ArrayList<String>)
                        }

                        val y =
                            _textField.get()!!.caretY + _textField.get()!!.rowHeight() / 2 - _textField.get()!!.scrollY
                        setHeight(getItemHeight() * minOf(3.0, results.count.toDouble()).toInt())

                        setHorizontalOffset(_textField.get()!!.caretX - _textField.get()!!.scrollX)
                        setVerticalOffset(y - _textField.get()!!.height)
                        notifyDataSetChanged()
                        show()
                    } else {
                        // 无过滤结果，关闭列表
                        notifyDataSetInvalidated()
                    }
                }
            }
        }
    }

    companion object {
        private var _globalLanguage: Language = getInstance() // 语言实例

        // 设置语言
        @JvmStatic
        @Synchronized
        fun setLanguage(lang: Language) {
            _globalLanguage = lang
        }

        // 获取当前语言
        @Synchronized
        fun getLanguage(): Language {
            return _globalLanguage
        }

        // 更新语言
        @Synchronized
        fun updateLanguage() {
            _globalLanguage = Lexer.getLanguage()
        }
    }
}
