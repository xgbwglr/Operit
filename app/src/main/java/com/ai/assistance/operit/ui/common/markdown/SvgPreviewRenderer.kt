package com.ai.assistance.operit.ui.common.markdown

import android.graphics.Color as AndroidColor
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Node
import org.xml.sax.InputSource

/**
 * An intentionally restrictive, provider-independent SVG preview.
 *
 * Only well-formed standalone SVG markup is accepted. Scripts, HTML via
 * foreignObject, navigable links, event handlers and nonlocal references
 * are rejected. The WebView additionally disables JavaScript and all
 * network/file/content loads. The assistant's source code remains visible
 * in EnhancedCodeBlock when a preview is rejected.
 */
internal object SafeSvgMarkup {
    private const val MAX_SVG_CHARACTERS = 200_000
    private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
    private val xmlDeclaration = Regex("""^\s*<\?xml[^>]*\?>\s*""")
    private val forbiddenElements = setOf(
        "script", "foreignobject", "iframe", "object", "embed",
        "audio", "video", "image", "a", "animate", "animatemotion",
        "animatetransform", "set", "discard"
    )
    private val externalCssUrl = Regex(
        """(?i)url\s*\(\s*['"]?\s*(?:https?:|//|file:|content:|javascript:)"""
    )

    internal data class Validation(val markup: String?, val reason: String? = null)

    // Preserve the old API for existing call sites and tests.
    fun validated(svg: String): String? = inspect(svg).markup

    fun inspect(svg: String): Validation {
        if (svg.isBlank()) return Validation(null, "没有找到 SVG 代码")
        if (svg.length > MAX_SVG_CHARACTERS) {
            return Validation(null, "SVG 内容过长（超过 20 万字符）")
        }

        val cleaned = svg.trim().removePrefix("\uFEFF")
            .replaceFirst(xmlDeclaration, "").trim()

        // Allow harmless XML comments before the root <svg> element.
        // The DOM check below remains authoritative, not this preflight test.
        if (!Regex("""(?i)<svg(?=\s|/|>)""").containsMatchIn(cleaned)) {
            return Validation(null, "代码中没有 <svg> 根元素")
        }
        if (cleaned.contains("<!DOCTYPE", ignoreCase = true) ||
            cleaned.contains("<!ENTITY", ignoreCase = true) ||
            cleaned.contains("<?xml-stylesheet", ignoreCase = true)
        ) {
            return Validation(null, "SVG 使用了不允许的 DTD、实体或外部样式")
        }
        if (externalCssUrl.containsMatchIn(cleaned)) {
            return Validation(null, "SVG 引用了不允许的外部资源")
        }

        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                // Android's standard DocumentBuilderFactory does NOT support
                // Xerces-only setFeature() calls (e.g. disallow-doctype-decl).
                // Calling them throws ParserConfigurationException and caused
                // every valid SVG to show "preview unavailable" on devices.
                //
                // For security we reject all DTDs before parsing, provide an
                // empty entity resolver, validate the DOM, and prevent WebView
                // network/file/content access and JavaScript execution.
            }
            val parser = factory.newDocumentBuilder().apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
            }
            val document = parser.parse(InputSource(StringReader(cleaned)))
            val root = document.documentElement
                ?: return Validation(null, "SVG XML 没有根节点")
            if (!root.localName.orEmpty().equals("svg", ignoreCase = true) &&
                !root.nodeName.equals("svg", ignoreCase = true)
            ) {
                return Validation(null, "XML 根节点不是 <svg>")
            }
            if (isUnsafeNode(root)) {
                Validation(null, "包含脚本、事件、外部链接或其他不安全元素")
            } else {
                Validation(cleaned)
            }
        } catch (e: Exception) {
            Validation(null, "SVG XML 语法错误、标签未闭合或格式不完整")
        }
    }

    private fun isUnsafeNode(node: Node): Boolean {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val name = (node.localName ?: node.nodeName).lowercase()
            if (name in forbiddenElements) return true
            val namespace = node.namespaceURI
            if (namespace != null && namespace != SVG_NAMESPACE) return true

            val attributes = node.attributes
            for (i in 0 until attributes.length) {
                val attr = attributes.item(i)
                val attrName = (attr.localName ?: attr.nodeName).lowercase()
                val attrValue = attr.nodeValue?.trim().orEmpty()
                if (attrName.startsWith("on") ||
                    attrName in setOf("base", "src", "srcdoc", "formaction")
                ) return true
                if (attrName == "href" && !attrValue.startsWith("#")) return true
                if (attrName == "style" && externalCssUrl.containsMatchIn(attrValue)) return true
                if (attrValue.contains("javascript:", ignoreCase = true)) return true
            }
        }

        val children = node.childNodes
        for (i in 0 until children.length) {
            if (isUnsafeNode(children.item(i))) return true
        }
        return false
    }
}

/**
 * Draw SVG as native Android Picture instead of rendering a transparent WebView
 * on top of Operit's dark code block. AndroidSVG is already an app dependency.
 *
 * A white SVG canvas is intentional: scientific illustrations generated by
 * models often use black text/axes with no explicit background. Transparent
 * WebView previews made those elements invisible on dark code-block surfaces.
 */
private sealed interface SvgPreviewState {
    data object Loading : SvgPreviewState
    data class Ready(val picture: Picture) : SvgPreviewState
    data class Failed(val reason: String) : SvgPreviewState
}

private fun renderNativeSvg(code: String): SvgPreviewState {
    val validation = SafeSvgMarkup.inspect(code)
    val svg = validation.markup
        ?: return SvgPreviewState.Failed(validation.reason ?: "SVG 内容未通过校验")

    return try {
        val picture = SVG.getFromString(svg).renderToPicture()
        require(picture.width > 0 && picture.height > 0) {
            "SVG 画布尺寸无效"
        }
        SvgPreviewState.Ready(picture)
    } catch (e: Exception) {
        SvgPreviewState.Failed(
            "SVG 图形无法绘制（可能使用了不支持的 SVG 语法）：" +
                (e.localizedMessage?.take(140) ?: "未知解析问题")
        )
    }
}

@Composable
internal fun SvgPreviewRenderer(code: String, modifier: Modifier = Modifier) {
    // Parsing and render-to-picture can be expensive for large generated SVG.
    // Do it off the main thread and restart only when the SVG source changes.
    val renderState by produceState<SvgPreviewState>(
        initialValue = SvgPreviewState.Loading,
        key1 = code,
    ) {
        value = withContext(Dispatchers.Default) { renderNativeSvg(code) }
    }

    when (val state = renderState) {
        SvgPreviewState.Loading -> {
            Text(
                text = "正在解析 SVG 图形…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
        }
        is SvgPreviewState.Failed -> {
            Text(
                text = "SVG 预览不可用：${state.reason}。可切回源码检查或另存为 SVG 文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
        }
        is SvgPreviewState.Ready -> {
            val picture = state.picture
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        // PictureDrawable requires software rendering on Android.
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setBackgroundColor(AndroidColor.WHITE)
                        adjustViewBounds = false
                        setImageDrawable(PictureDrawable(picture))
                    }
                },
                update = { view ->
                    val oldPicture = (view.drawable as? PictureDrawable)?.picture
                    if (oldPicture !== picture) {
                        view.setImageDrawable(PictureDrawable(picture))
                    }
                },
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}
