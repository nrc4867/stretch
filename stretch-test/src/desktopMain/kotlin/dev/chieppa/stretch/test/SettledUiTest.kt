package dev.chieppa.stretch.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import dev.chieppa.stretch.core.Settled
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs a Compose UI test whose `waitForIdle`, and every wait built on it, also waits until each of [settled] is: work
 * Compose cannot see, such as stretch-web's `EngineWork` while the browser engine draws a page. The test then waits for
 * that work as it waits for composition, never for time to pass. It fails when it takes longer than [testTimeout].
 */
@OptIn(ExperimentalTestApi::class)
fun runComposeUiTestAwaiting(vararg settled: Settled, testTimeout: Duration = 60.seconds, block: suspend ComposeUiTest.() -> Unit) =
    runTest(timeout = testTimeout) {
        with(DesktopComposeUiTest(testTimeout = testTimeout, useStandardTestDispatcherForComposition = true)) {
            for (each in settled) {
                registerIdlingResource(object : IdlingResource {
                    override val isIdleNow: Boolean get() = each.settled.value
                })
            }
            runTest { block() }
        }
    }
