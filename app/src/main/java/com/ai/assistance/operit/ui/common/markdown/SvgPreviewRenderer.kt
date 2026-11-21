package com.ai.assistance.operit.ui.common.markdown

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
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

@Composable
internal fun SvgPreviewRenderer(code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val validation = remember(code) { SafeSvgMarkup.inspect(code) }
    val validatedMarkup = validation.markup

    if (validatedMarkup == null) {
        Text(
            text = "SVG 预览不可用：${validation.reason ?: "未知错误"}。请切回代码查看原文。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
        return
    }

    val markup = validatedMarkup
    val html = remember(markup) {
        """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <meta http-equiv="Content-Security-Policy"
                content="default-src 'none'; script-src 'none'; connect-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'">
          <style>
            html,body { margin:0; padding:0; width:100%; height:100%; background:transparent; }
            main { width:100%; height:100%; display:flex; justify-content:center; align-items:center; overflow:auto; }
            main > svg { display:block; max-width:100%; max-height:100%; }
          </style>
        </head>
        <body><main>$markup</main></body>
        </html>
        """.trimIndent()
    }

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.blockNetworkLoads = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setBackgroundColor(AndroidColor.TRANSPARENT)
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = true

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = true

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse {
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
            }
        }
    }

    LaunchedEffect(html) {
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier.fillMaxWidth())
}
