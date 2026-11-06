package dev.chieppa.stretch.web

import dev.chieppa.stretch.core.AppLaunch
import dev.chieppa.stretch.core.Requirement
import dev.chieppa.stretch.core.RequirementState
import dev.chieppa.stretch.core.stretchUserFolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** The id of the browser engine in [dev.chieppa.stretch.core.Requirements]. */
const val ENGINE_REQUIREMENT = "stretch-web.engine"

private const val RUNTIMES_FOLDER = "runtimes"

/**
 * The browser engine, for an app running on a Java runtime without JCEF: a JetBrains Runtime with JCEF, fetched into
 * the user's Stretch folder, which the app then runs on. [engineHere] says whether this runtime already has one.
 */
internal class EngineRequirement(private val engineHere: () -> Boolean, private val installer: RuntimeInstaller?) : Requirement {
    override val id = ENGINE_REQUIREMENT
    override val downloadBytes: Long? = installer?.download?.bytes

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val current = MutableStateFlow(startingState())
    override val state: StateFlow<RequirementState> = current.asStateFlow()
    private var job: Job? = null

    private fun startingState(): RequirementState = when {
        engineHere() -> RequirementState.Ready
        installer == null -> RequirementState.Failed("No browser engine is published for this computer")
        installer.installedHome() != null -> RequirementState.NeedsRestart
        else -> RequirementState.Missing
    }

    /** The Java home of the installed runtime, or null before an install. */
    fun installedHome(): File? = installer?.installedHome()

    @Synchronized
    override fun install() {
        val installer = installer ?: return
        if (current.value != RequirementState.Missing && current.value !is RequirementState.Failed) return
        val previous = job
        current.value = RequirementState.Installing(0, installer.download.bytes)
        job = scope.launch {
            // A cancelled install may still be clearing its files.
            previous?.join()
            try {
                installer.install { fetched -> current.value = RequirementState.Installing(fetched, installer.download.bytes) }
                current.value = RequirementState.NeedsRestart
            } catch (e: CancellationException) {
                current.value = RequirementState.Missing
                throw e
            } catch (e: Exception) {
                current.value = RequirementState.Failed(e.message ?: e.toString())
            }
        }
    }

    @Synchronized
    override fun cancel() {
        job?.cancel()
    }

    override fun restart() {
        if (current.value != RequirementState.NeedsRestart) return
        AppLaunch.startAgain(installedHome() ?: return, waitAndExit = false)
    }
}

/** The one engine requirement in the process. */
internal val engineRequirement: EngineRequirement by lazy {
    EngineRequirement({ WebEngine.available }, runtimeForThisComputer()?.let { RuntimeInstaller(it, File(stretchUserFolder(), RUNTIMES_FOLDER)) })
}

/** Lists the engine in [dev.chieppa.stretch.core.Requirements]. Use [WebEngine.requirement] to reach it. */
class WebEngineRequirement : Requirement by engineRequirement
