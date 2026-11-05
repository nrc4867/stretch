package dev.chieppa.stretch.code

/** What the tokenizer needs to know about a language. Find one with [CodeLanguages.named] or [CodeLanguages.forPath]. */
data class CodeLanguage(
    val id: String,
    val aliases: Set<String> = emptySet(),
    val extensions: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet(),
    val constants: Set<String> = setOf("true", "false", "null"),
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    /** Delimiters of strings on one line. */
    val strings: List<String> = listOf("\"", "'"),
    /** Delimiters of strings that may cross lines. */
    val multilineStrings: List<String> = emptyList(),
    val ignoreCase: Boolean = false,
    /** Markup, where `<name` starts a keyword-coloured tag. */
    val tags: Boolean = false,
    /** Key-value data, where a name before `:` or `=` is a key. */
    val keyed: Boolean = false,
)

private fun words(text: String): Set<String> = text.trim().split(Regex("\\s+")).toSet()

private val cLike = words(
    "if else for while do switch case default break continue return goto struct union enum typedef static const " +
        "extern sizeof void int char short long float double signed unsigned auto register volatile inline",
)
private val jsWords = words(
    "async await break case catch class const continue debugger default delete do else export extends finally for " +
        "from function if implements import in instanceof interface let new of return static super switch this throw " +
        "try typeof var void while with yield type enum as declare namespace readonly public private protected abstract",
)

/** Every language Stretch highlights. Anything else shows as plain text. */
object CodeLanguages {
    val all: List<CodeLanguage> = listOf(
        CodeLanguage(
            "kotlin", setOf("kt", "kts"), setOf("kt", "kts"),
            words(
                "abstract actual annotation as break by catch class companion const constructor continue data do else enum " +
                    "expect external final finally for fun get if import in infix init inline inner interface internal is " +
                    "lateinit noinline object open operator out override package private protected public reified return " +
                    "sealed set super suspend tailrec this throw try typealias val value var vararg when where while",
            ),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("\"\"\""),
        ),
        CodeLanguage(
            "java", extensions = setOf("java"),
            keywords = words(
                "abstract assert boolean break byte case catch char class const continue default do double else enum extends " +
                    "final finally float for goto if implements import instanceof int interface long native new package private " +
                    "protected public record return short static strictfp super switch synchronized this throw throws transient " +
                    "try var void volatile while yield",
            ),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("\"\"\""),
        ),
        CodeLanguage(
            "typescript", setOf("ts", "tsx", "javascript", "js", "jsx", "mjs", "cjs"), setOf("ts", "tsx", "js", "jsx", "mjs", "cjs"),
            jsWords, setOf("true", "false", "null", "undefined", "NaN"),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("`"),
        ),
        CodeLanguage(
            "python", setOf("py", "python3"), setOf("py", "pyi"),
            words(
                "and as assert async await break class continue def del elif else except finally for from global if import in " +
                    "is lambda match nonlocal not or pass raise return try while with yield",
            ),
            setOf("True", "False", "None"),
            lineComments = listOf("#"), multilineStrings = listOf("\"\"\"", "'''"),
        ),
        CodeLanguage(
            "rust", setOf("rs"), setOf("rs"),
            words(
                "as async await break const continue crate dyn else enum extern fn for if impl in let loop match mod move mut " +
                    "pub ref return self Self static struct super trait type unsafe use where while",
            ),
            setOf("true", "false", "None", "Some"),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"),
        ),
        CodeLanguage(
            "go", setOf("golang"), setOf("go"),
            words(
                "break case chan const continue default defer else fallthrough for func go goto if import interface map package " +
                    "range return select struct switch type var",
            ),
            setOf("true", "false", "nil", "iota"),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("`"),
        ),
        CodeLanguage(
            "cpp", setOf("c", "c++", "h", "hpp", "cc"), setOf("c", "h", "cpp", "cc", "cxx", "hpp", "hh", "m", "mm"),
            cLike + words(
                "class namespace template typename public private protected virtual override new delete this throw try catch " +
                    "using bool nullptr constexpr noexcept operator friend explicit",
            ),
            setOf("true", "false", "NULL", "nullptr"),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"),
        ),
        CodeLanguage(
            "csharp", setOf("cs", "c#"), setOf("cs"),
            words(
                "abstract as async await base bool break byte case catch char class const continue decimal default delegate do " +
                    "double else enum event explicit extern finally fixed float for foreach goto if implicit in int interface " +
                    "internal is lock long namespace new object operator out override params private protected public readonly " +
                    "record ref return sealed short static string struct switch this throw try typeof uint ulong using var virtual " +
                    "void volatile while yield",
            ),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("\"\"\""),
        ),
        CodeLanguage(
            "swift", extensions = setOf("swift"),
            keywords = words(
                "actor as associatedtype async await break case catch class continue default defer deinit do else enum extension " +
                    "fallthrough fileprivate final for func guard if import in init inout internal is let open operator private " +
                    "protocol public repeat return self Self static struct subscript super switch throw throws try typealias var " +
                    "where while",
            ),
            constants = setOf("true", "false", "nil"),
            lineComments = listOf("//"), blockComments = listOf("/*" to "*/"), multilineStrings = listOf("\"\"\""),
        ),
        CodeLanguage(
            "ruby", setOf("rb"), setOf("rb", "rake"),
            words(
                "alias and begin break case class def defined do else elsif end ensure for if in module next not or redo rescue " +
                    "retry return self super then undef unless until when while yield",
            ),
            setOf("true", "false", "nil"),
            lineComments = listOf("#"),
        ),
        CodeLanguage(
            "php", extensions = setOf("php"),
            keywords = words(
                "abstract and array as break case catch class clone const continue declare default do echo else elseif empty " +
                    "extends final finally fn for foreach function global if implements include instanceof interface isset list " +
                    "match namespace new or print private protected public require return static switch throw trait try unset use " +
                    "var while yield",
            ),
            lineComments = listOf("//", "#"), blockComments = listOf("/*" to "*/"),
        ),
        CodeLanguage(
            "shell", setOf("bash", "sh", "zsh", "console"), setOf("sh", "bash", "zsh"),
            words(
                "if then else elif fi for while until do done case esac in function return exit local export readonly source " +
                    "echo cd set unset",
            ),
            setOf("true", "false"),
            lineComments = listOf("#"),
        ),
        CodeLanguage(
            "powershell", setOf("ps1", "pwsh"), setOf("ps1", "psm1"),
            words("if else elseif for foreach while do switch function param return try catch finally throw begin process end"),
            setOf("\$true", "\$false", "\$null"),
            lineComments = listOf("#"), blockComments = listOf("<#" to "#>"), ignoreCase = true,
        ),
        CodeLanguage(
            "sql", setOf("postgres", "postgresql", "mysql", "sqlite"), setOf("sql"),
            words(
                "select from where insert into values update set delete create table view index drop alter add join inner left " +
                    "right outer full on group by order having limit offset union all distinct as and or not in is like between " +
                    "case when then else end primary key foreign references default exists with returning begin commit rollback",
            ),
            setOf("true", "false", "null"),
            lineComments = listOf("--"), blockComments = listOf("/*" to "*/"), strings = listOf("'", "\""), ignoreCase = true,
        ),
        CodeLanguage(
            "html", setOf("xml", "svg", "xhtml", "vue"), setOf("html", "htm", "xml", "svg", "xhtml", "vue", "plist"),
            constants = emptySet(), blockComments = listOf("<!--" to "-->"), tags = true,
        ),
        CodeLanguage(
            "css", setOf("scss", "less"), setOf("css", "scss", "less"),
            words("important media import supports keyframes font-face"),
            emptySet(),
            lineComments = emptyList(), blockComments = listOf("/*" to "*/"), keyed = true,
        ),
        CodeLanguage("json", setOf("jsonc", "json5"), setOf("json", "jsonc", "json5"), lineComments = listOf("//"), keyed = true),
        CodeLanguage(
            "yaml", setOf("yml"), setOf("yaml", "yml"),
            constants = setOf("true", "false", "null", "yes", "no"), lineComments = listOf("#"), keyed = true,
        ),
        CodeLanguage(
            "toml", setOf("ini", "properties"), setOf("toml", "ini", "properties", "cfg"),
            lineComments = listOf("#", ";"), multilineStrings = listOf("\"\"\"", "'''"), keyed = true,
        ),
    )

    private val byName = all.flatMap { language -> (language.aliases + language.id).map { it to language } }.toMap()
    private val byExtension = all.flatMap { language -> language.extensions.map { it to language } }.toMap()

    /** The language a markdown fence or an app calls [name], ignoring case. */
    fun named(name: String?): CodeLanguage? = name?.trim()?.lowercase()?.let(byName::get)

    /** The language of a file at [path], from its extension. */
    fun forPath(path: String?): CodeLanguage? {
        val extension = path?.substringAfterLast('/')?.substringAfterLast('\\')?.substringAfterLast('.', "") ?: return null
        return byExtension[extension.lowercase()]
    }
}
