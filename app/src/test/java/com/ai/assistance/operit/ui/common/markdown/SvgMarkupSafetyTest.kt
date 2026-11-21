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
    @Test
    fun acceptsRealPhysicsDiagramWithMarkersAndGradients() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="800" height="450" viewBox="0 0 800 450">
              <defs>
                <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
                  <path d="M0,0 L0,6 L9,3 Z" fill="#4c69d8"/>
                </marker>
                <linearGradient id="trajectory"><stop offset="0%" stop-color="#1aa8bb"/><stop offset="100%" stop-color="#4c69d8"/></linearGradient>
              </defs>
              <path d="M40 400 Q400 -400 760 400" fill="none" stroke="url(#trajectory)" stroke-width="3"/>
              <line x1="40" y1="400" x2="760" y2="400" stroke="#333" marker-end="url(#arrow)"/>
              <text x="360" y="55">最高点：5m</text>
              <text x="360" y="420">射程：34.64m</text>
            </svg>
        """.trimIndent()
        assertNotNull(SafeSvgMarkup.validated(svg))
        assertEquals(null, SafeSvgMarkup.inspect(svg).reason)
    }

    @Test
    fun commentsBeforeSvgRootDoNotCauseFalseRejection() {
        val svg = """
            <!-- Generated physics experiment graphic -->
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50">
                <circle cx="25" cy="25" r="20" fill="royalblue"/>
            </svg>
        """.trimIndent()
        assertNotNull(SafeSvgMarkup.validated(svg))
    }

    @Test
    fun failureReportsSpecificReasonInsteadOfSilentlySwallowingIt() {
        val unsafe = SafeSvgMarkup.inspect("""<svg onload="alert(1)"></svg>""")
        assertNull(unsafe.markup)
        assertNotNull(unsafe.reason)

        val unfinished = SafeSvgMarkup.inspect("""<svg><rect width="50" """)
        assertNull(unfinished.markup)
        assertNotNull(unfinished.reason)
    }


}
