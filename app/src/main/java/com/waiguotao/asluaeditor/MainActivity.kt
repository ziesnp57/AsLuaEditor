package com.waiguotao.asluaeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aslua.CodeEditor
import com.aslua.language.lua.LuaLanguage.Companion.getInstance
import com.waiguotao.asluaeditor.ui.theme.AsLuaEditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsLuaEditorTheme {
                Greeting()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {

    // 创建 CodeEditor 实例
    var codeEditor: CodeEditor? = null
    Column(
        modifier = Modifier
            .padding()
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "AsLuaEditor",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }, // 显示项目名称
            actions = {
                IconButton(onClick = { codeEditor?.undo() }) {
                    Text("撤销")
                }
                IconButton(onClick = { codeEditor?.redo() }) {
                    Text("重做")
                }
            }
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.8.dp,
            color = DividerDefaults.color
        )
        // 中间的内容：AndroidView
        AndroidView(
            factory = {
                CodeEditor(it).apply {
                    codeEditor = this  // 绑定 CodeEditor 实例
                    setText("")
//                                    setTextSize(71) // 设置字体大小
//                                    setWordWrap(true) // 自动换行
//                                    setAutoIndent(true) // 自动缩进
//                                    isShowLineNumbers = false // 显示行号

                    setLanguage(getInstance())
                }
            },
            modifier = Modifier
                .fillMaxWidth()  // 确保它占满宽度
            .weight(1f)  // 让这个区域占据剩余空间
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.8.dp,
            color = DividerDefaults.color
        )

        // 最下面的符号 Tab Row
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()) // 启用水平滚动
                .fillMaxWidth()
                .height(36.dp) // Tab Row 高度
        ) {
            arrayOf(
                "(",
                ")",
                "[",
                "]",
                "{",
                "}",
                "\"",
                "'",
                ":",
                ";",
                ",",
                ".",
                "=",
                "+",
                "-",
                "*",
                "/",
                "%",
                "<",
                ">",
                "&",
                "|",
                "~",
                "!"
            ).forEachIndexed { index, title ->
                Tab(
                    selected = index == -1,  // 这里不需要选中效果
                    onClick = { codeEditor?.paste(title) },
                    modifier = Modifier
                        .fillMaxSize(),
                    text = { Text(title) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AsLuaEditorTheme {
        Greeting()
    }
}