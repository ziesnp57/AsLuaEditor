package com.aslua.view

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import java.lang.ref.WeakReference

/**
 * 用于显示剪贴板操作的面板
 */
class ClipboardPanel(private var textField: FreeScrollingTextField) {
    private val contextRef: WeakReference<Context> = WeakReference(textField.context)
    private var clipboardActionMode: ActionMode? = null

    // 获取上下文，避免内存泄漏
    fun getContext(): Context? = contextRef.get()

    // 显示剪贴板面板
    fun show() {
        if (clipboardActionMode == null) {
            startClipboardAction()
        }
    }

    // 隐藏剪贴板面板
    fun hide() {
        stopClipboardAction()
    }

    // 开始剪贴板操作
    private fun startClipboardAction() {
        clipboardActionMode = textField.startActionMode(object : ActionMode.Callback {
            @SuppressLint("ResourceType")
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                clipboardActionMode = mode
                mode.title = getContext()?.getString(R.string.selectTextMode)

                mode.setTitle(R.string.selectTextMode)
                val array = getContext()?.theme?.obtainStyledAttributes(
                    intArrayOf(
                        R.attr.actionModeSelectAllDrawable,
                        R.attr.actionModeCutDrawable,
                        R.attr.actionModeCopyDrawable,
                        R.attr.actionModePasteDrawable,
                    )
                )

                val selectAllItem = menu.add(0, 0, 0, getContext()?.getString(R.string.selectAll)).apply {
                    setShowAsActionFlags(2)
                    alphabeticShortcut = 'a'
                    icon = array?.getDrawable(0)
                }
                val cutItem = menu.add(0, 1, 0, getContext()?.getString(R.string.cut)).apply {
                    setShowAsActionFlags(2)
                    alphabeticShortcut = 'x'
                    icon = array?.getDrawable(1)
                }
                val copyItem = menu.add(0, 2, 0, getContext()?.getString(R.string.copy)).apply {
                    setShowAsActionFlags(2)
                    alphabeticShortcut = 'c'
                    icon = array?.getDrawable(2)
                }
                val pasteItem = menu.add(0, 3, 0, getContext()?.getString(R.string.paste)).apply {
                    setShowAsActionFlags(2)
                    alphabeticShortcut = 'v'
                    icon = array?.getDrawable(3)
                }

                // 设置点击事件
                selectAllItem.setOnMenuItemClickListener {
                    textField.selectAll()
                    true
                }
                cutItem.setOnMenuItemClickListener {
                    textField.cut()
                    mode.finish()
                    true
                }
                copyItem.setOnMenuItemClickListener {
                    textField.copy()
                    mode.finish()
                    true
                }
                pasteItem.setOnMenuItemClickListener {
                    textField.paste()
                    mode.finish()
                    true
                }

                array?.recycle()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                textField.selectText(false)
                clipboardActionMode = null
            }
        })
    }

    // 停止剪贴板操作
    private fun stopClipboardAction() {
        clipboardActionMode?.finish()
        clipboardActionMode = null
    }
}
