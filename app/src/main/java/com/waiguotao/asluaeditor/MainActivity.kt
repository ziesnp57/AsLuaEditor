package com.waiguotao.asluaeditor

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.waiguotao.asluaeditor.ui.theme.AsLuaEditorTheme
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.InputStream
import java.io.Reader

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
    val codeEditor = remember { mutableStateOf<CodeEditor?>(null) }
    val symbolInputView = remember { mutableStateOf<SymbolInputView?>(null) }
    val context = LocalContext.current
    val typeface = Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf")


    LaunchedEffect(Unit) {
        // 注册 Assets 文件提供器
        FileProviderRegistry.getInstance()
            .addFileProvider(AssetsFileResolver(context.assets))

        // 加载语法定义
        GrammarRegistry.getInstance().loadGrammars("languages.json")

        // 创建 Lua 语言
        val luaLang = TextMateLanguage.create("source.lua", true)
        codeEditor.value?.setEditorLanguage(luaLang)

        // 创建 ThemeModel 并加载
        val themeSource = object : IThemeSource {
            override fun getFilePath(): String = "themes/darcula.json"
            override fun getReader(): Reader = context.assets.open(getFilePath()).bufferedReader()
        }
        val themeModel = ThemeModel(themeSource, "Darcula")
        themeModel.load()

        // 注册到 ThemeRegistry
        ThemeRegistry.getInstance().loadTheme(themeModel)

        // 创建 TextMateColorScheme 并应用
        val colorSchem = TextMateColorScheme.create(ThemeRegistry.getInstance(), themeModel)
        codeEditor.value?.apply {
            colorScheme = colorSchem
            typefaceText = Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf")
        }
    }

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
                IconButton(onClick = { codeEditor.value?.undo() }) {
                    Text("撤销")
                }
                IconButton(onClick = { codeEditor.value?.redo() }) {
                    Text("重做")
                }


                IconButton(onClick = { codeEditor.value?.formatCodeAsync() }) {
                    Text("格式")
                }


            }
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.8.dp,
            color = DividerDefaults.color
        )

        val SYMBOLS = arrayOf(
            "->", "{", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )

        /**
         * Texts to be committed to editor for symbols above
         */
        val SYMBOL_INSERT_TEXT = arrayOf(
            "\t", "{}", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )
        val thumbColor = colorScheme.outline.copy(alpha = 0.4f).toArgb()

        // 中间的内容：AndroidView
        AndroidView(
            factory = { context ->
                CodeEditor(context).also { editor ->
                    codeEditor.value = editor

                    editor.setText("Hello, world!") // 设置文本


//                    0 -> editor.colorScheme = EditorColorScheme()
//                    1 -> editor.colorScheme = SchemeGitHub()
//                    2 -> editor.colorScheme = SchemeEclipse()
//                    3 -> editor.colorScheme = SchemeDarcula()
//                    4 -> editor.colorScheme = SchemeVS2019()
//                    5 -> editor.colorScheme = SchemeNotepadXX()
//                    editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    // 设置颜色
//                    editor.colorScheme = SchemeDarcula()

//                    editor.typefaceText = Typeface.MONOSPACE
                    editor.isLineNumberEnabled = true // 显示行号
                    editor.isHorizontalScrollBarEnabled = false // 禁用横向滚动条
                    editor.isVerticalScrollBarEnabled = true
                    editor.verticalExtraSpaceFactor = 0.7f
                    editor.isDisplayLnPanel = false // 行号面板


                    editor.isCursorAnimationEnabled = false
                    editor.isStickyTextSelection = true

                    editor.props.drawSideBlockLine = false
                    editor.props.enableRoundTextBackground = false
                    editor.props.boldMatchingDelimiters = false

                    val density = context.resources.displayMetrics.density
                    editor.verticalScrollbarThumbDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8f * density
                        setColor(thumbColor)
                    }


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



        AndroidView(
            factory = { context ->
                SymbolInputView(context).also {
                    symbolInputView.value = it
                    it.bindEditor(codeEditor.value)
                    it.addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT)
                    it.forEachButton { button -> button.typeface = typeface }
                }
            },
            modifier = Modifier
                .horizontalScroll(rememberScrollState()) // 启用水平滚动
                .fillMaxWidth()

        )

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AsLuaEditorTheme {
        Greeting()
    }
}