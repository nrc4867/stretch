package dev.chieppa.stretch.mermaid

import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.markdown.DiagramPicture
import dev.chieppa.stretch.markdown.DiagramRenderer
import dev.chieppa.stretch.markdown.DiagramStyle
import dev.chieppa.stretch.mermaid.generated.Res
import dev.chieppa.stretch.web.EngineWork
import dev.chieppa.stretch.web.ScriptPage
import dev.chieppa.stretch.web.WebEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val LANGUAGE = "mermaid"
private const val SCRIPT_FILE = "files/mermaid.min.js"
private const val MAX_CACHED_PICTURES = 64

/**
 * Draws `mermaid` code blocks with Mermaid itself, in a hidden page, in the colors of the current theme. One instance
 * can serve every editor in an app; it starts the engine and loads the script on the first diagram. Where there is no
 * engine (see [WebEngine]) it handles nothing, so diagrams show as their code. Drawing counts as [EngineWork].
 */
class MermaidDiagrams : DiagramRenderer {
    private val lock = Mutex()
    private var page: ScriptPage? = null
    private val cache = LinkedHashMap<Pair<String, DiagramStyle>, DiagramPicture>()

    override fun handles(language: String): Boolean = WebEngine.available && language.equals(LANGUAGE, ignoreCase = true)

    override suspend fun render(source: String, style: DiagramStyle): DiagramPicture = EngineWork.during { draw(source, style) }

    @OptIn(ExperimentalResourceApi::class, ExperimentalEncodingApi::class)
    private suspend fun draw(source: String, style: DiagramStyle): DiagramPicture = lock.withLock {
        val key = source to style
        cache[key]?.let { return@withLock it }
        val ready = page ?: ScriptPage().also {
            it.load(Res.readBytes(SCRIPT_FILE).decodeToString())
            it.load(MERMAID_BRIDGE)
            page = it
        }
        val call = "stretchMermaid(${jsString(source)}, ${mermaidThemeVariables(style.colors, style.dark)}, ${style.density})"
        val (width, height, png) = ready.call(call).split('|', limit = 3)
        val picture = DiagramPicture(Base64.decode(png).decodeToImageBitmap(), width.toInt().dp, height.toInt().dp)
        if (cache.size >= MAX_CACHED_PICTURES) cache.remove(cache.keys.first())
        cache[key] = picture
        picture
    }

    /** Closes the hidden page. The next diagram opens it again. */
    fun close() {
        page?.close()
        page = null
    }
}
