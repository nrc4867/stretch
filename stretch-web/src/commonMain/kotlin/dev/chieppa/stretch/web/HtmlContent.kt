package dev.chieppa.stretch.web

/** What an [HtmlView] shows. */
sealed interface HtmlContent {
    /** A document held in memory. Relative links and images resolve against [baseUrl]. */
    data class Inline(val html: String, val baseUrl: String? = null) : HtmlContent

    /** A page at an address. */
    data class Url(val url: String) : HtmlContent
}

/**
 * What an [HtmlView]'s inline document may do. A page from a [HtmlContent.Url] runs with the web's usual rules, plus
 * the same limits on leaving the page.
 */
data class HtmlPolicy(
    /** The document may run its scripts. */
    val scripts: Boolean = true,
    /** The document may load anything from the network, such as images, styles and scripts. */
    val network: Boolean = true,
)

/** The directive allowing everything a document embeds to come only from itself. */
private const val SELF_CONTAINED_SOURCES = "data: blob: 'unsafe-inline' 'unsafe-eval'"

/** The Content-Security-Policy that enforces [policy], or null when it allows everything. */
internal fun contentSecurityPolicy(policy: HtmlPolicy): String? {
    if (policy.scripts && policy.network) return null
    return buildList {
        add(if (policy.network) "default-src * $SELF_CONTAINED_SOURCES" else "default-src $SELF_CONTAINED_SOURCES")
        if (!policy.scripts) add("script-src 'none'")
    }.joinToString("; ")
}

private val headOpen = Regex("<head(\\s[^>]*)?>", RegexOption.IGNORE_CASE)
private val htmlOpen = Regex("<html(\\s[^>]*)?>", RegexOption.IGNORE_CASE)
private val doctype = Regex("^\\s*<!doctype[^>]*>", RegexOption.IGNORE_CASE)

private fun escapeAttribute(value: String) = value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")

/**
 * [html] with its policy, base address, page look and [script] (when scripts may run) added at the very start of its head. A browser applies the first
 * policy it reads and a later one can only tighten it, so nothing in the document can undo this.
 */
internal fun prepareDocument(html: String, policy: HtmlPolicy, baseUrl: String? = null, look: HtmlLook? = null, script: String? = null): String {
    val head = buildString {
        append("<meta charset=\"utf-8\">")
        contentSecurityPolicy(policy)?.let { append("<meta http-equiv=\"Content-Security-Policy\" content=\"${escapeAttribute(it)}\">") }
        baseUrl?.let { append("<base href=\"${escapeAttribute(it)}\">") }
        look?.let { l ->
            val variables = l.variables.entries.joinToString("") { (name, value) -> ";$name:$value" }
            append("<style>:root{color-scheme:${if (l.dark) "dark" else "light"};color:${l.ink}$variables}</style>")
        }
        if (script != null && policy.scripts) append("<script>$script</script>")
    }
    headOpen.find(html)?.let { return html.replaceRange(it.range.last + 1, it.range.last + 1, head) }
    htmlOpen.find(html)?.let { return html.replaceRange(it.range.last + 1, it.range.last + 1, "<head>$head</head>") }
    val prologue = doctype.find(html)?.range?.last?.plus(1) ?: 0
    return html.replaceRange(prologue, prologue, "<head>$head</head>")
}

/**
 * The theme a document starts from until its own styles say otherwise. Its background is the view itself, behind the
 * page. [variables] are CSS custom properties, by name, for the page's own styles to use.
 */
internal data class HtmlLook(val dark: Boolean, val ink: String, val variables: Map<String, String> = emptyMap())

/** Whether going to [target] from [current] stays on the page, as a jump to an anchor does. */
internal fun isSamePage(current: String, target: String): Boolean = current.substringBefore('#') == target.substringBefore('#')
