package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DARK = 0.3f
private const val DENSITY = 2f
private const val CORNER_DP = 12
private const val SIZE_DP = 100
private const val PAGE_DP = 20
private const val RAY_LENGTH = 60

@OptIn(ExperimentalTestApi::class)
class FloatingEdgeTest {
    /** No light line between the border and a hard shadow: outward from the corner, dark comes in one unbroken run. */
    @Test
    fun aCornerShowsNoLightLineBetweenBorderAndShadow() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(DENSITY)) {
                Box(Modifier.testTag("page").background(Color.White).padding(PAGE_DP.dp)) {
                    Box(Modifier.size(SIZE_DP.dp).floating(RoundedCornerShape(CORNER_DP.dp), StretchShadow(4.dp, 4.dp, 0.dp, Color.Black)))
                }
            }
        }
        val pixels = onNodeWithTag("page").captureToImage().toPixelMap()
        val center = (PAGE_DP + SIZE_DP - CORNER_DP) * DENSITY
        for (degrees in 0..90 step 5) {
            val angle = degrees * PI / 180
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            val luma = (0 until RAY_LENGTH).map { step ->
                val at = Offset(center, center) + direction * step.toFloat()
                val color = pixels[at.x.toInt().coerceIn(0, pixels.width - 1), at.y.toInt().coerceIn(0, pixels.height - 1)]
                (color.red + color.green + color.blue) / 3
            }
            val darkRuns = luma.indices.count { it > 0 && luma[it] < DARK && luma[it - 1] >= DARK }
            assertEquals(1, darkRuns, "at $degrees degrees: $luma")
        }
    }
}
