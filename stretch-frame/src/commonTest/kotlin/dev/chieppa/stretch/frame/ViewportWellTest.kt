package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ViewportWellTest {
    /** The color in the middle of a viewport laid over a red surface. */
    private fun middleOver(well: Boolean): Pair<Color, Color> {
        var shown = Color.Unspecified
        var wellColor = Color.Unspecified
        runComposeUiTest {
            setStretchContent(StretchThemes.Ink) {
                wellColor = Stretch.colors.surfaceWell
                Box(Modifier.size(100.dp).background(Color.Red)) {
                    Viewport(grid = false, well = well) {}
                }
            }
            val pixels = onRoot().captureToImage().toPixelMap()
            shown = pixels[pixels.width / 2, pixels.height / 2]
        }
        return shown to wellColor
    }

    @Test
    fun aViewportPaintsItsWellByDefault() {
        val (shown, well) = middleOver(well = true)
        assertEquals(well, shown)
    }

    @Test
    fun aViewportWithoutItsWellShowsWhatIsBeneath() {
        val (shown, _) = middleOver(well = false)
        assertEquals(Color.Red, shown)
    }
}
