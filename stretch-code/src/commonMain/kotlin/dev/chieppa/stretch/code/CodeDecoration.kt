package dev.chieppa.stretch.code

/**
 * How a [CodeDecoration] draws, each in the theme's colors. Washes go behind the text and underlines under it.
 * [Reference] and [Definition] wash; [Accent], [Warning] and [Error] underline.
 */
enum class CodeDecorationStyle {
    /** A light wash, such as for each place a name is read. */
    Reference,

    /** A stronger wash, such as for where a name is defined or written. Over a [Reference] it wins. */
    Definition,

    /** A dotted underline in the accent, for marks an app or a plugin adds. The other underlines win over it. */
    Accent,

    /** A wavy underline in the warning color. Over an [Accent] it wins. */
    Warning,

    /** A wavy underline in the error color. It wins over every other underline. */
    Error,
}

/**
 * A run of code an app sets apart: [range] drawn in [style], with [tooltip] shown as plain text while the pointer rests
 * on it. All plain data, so it can come from anywhere, such as a plugin in another process. Decorations may overlap:
 * where they do, each character takes the strongest wash and the strongest underline over it (see
 * [CodeDecorationStyle]), and a wash and an underline draw together. Find matches draw over washes. Where several
 * decorations with a tooltip meet under the pointer, their tooltips show together, the most severe first.
 */
data class CodeDecoration(val range: CodeRange, val style: CodeDecorationStyle, val tooltip: String? = null)
