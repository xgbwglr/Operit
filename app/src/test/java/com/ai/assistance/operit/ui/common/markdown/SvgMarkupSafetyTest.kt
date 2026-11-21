package com.ai.assistance.operit.ui.common.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SvgMarkupSafetyTest {
    @Test
    fun acceptsBasicSvgFromAnyModel() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100"><rect x="1" y="2" width="180" height="80" fill="#4f46e5"/><text x="10" y="35">AI</text></svg>"""
        assertEquals(svg, SafeSvgMarkup.validated(svg))
    }

    @Test
    fun acceptsSvgWithGradientsAndPaths() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><defs><linearGradient id="a"><stop offset="0" stop-color="#fff"/></linearGradient></defs><path d="M0 0L10 10" stroke="url(#a)"/></svg>"""
        assertNotNull(SafeSvgMarkup.validated(svg))
    }

    @Test
    fun rejectsScriptOrHtmlInjection() {
        assertNull(SafeSvgMarkup.validated("""<svg><script>alert(1)</script></svg>"""))
        assertNull(SafeSvgMarkup.validated("""<svg><foreignObject><div>html</div></foreignObject></svg>"""))
        assertNull(SafeSvgMarkup.validated("""<svg></svg><script>alert(1)</script>"""))
    }

    @Test
    fun rejectsEventHandlersAndNetworkReferences() {
        assertNull(SafeSvgMarkup.validated("""<svg onload="alert(1)"><circle r="4"/></svg>"""))
        assertNull(SafeSvgMarkup.validated("""<svg><use href="https://example.org/a.svg#b"/></svg>"""))
        assertNull(SafeSvgMarkup.validated("""<svg><rect style="fill:url(https://example.org/x)"/></svg>"""))
    }

    @Test
    fun rejectsDoctypeAndIncompleteSvg() {
        assertNull(SafeSvgMarkup.validated("""<!DOCTYPE svg [<!ENTITY x SYSTEM "file:///etc/passwd">]><svg/>"""))
        assertNull(SafeSvgMarkup.validated("""<svg><rect width="50" """))
    }

    @Test
    fun rejectsHtmlThatIsNotSvg() {
        assertNull(SafeSvgMarkup.validated("""<html><body>hello</body></html>"""))
    }
}
