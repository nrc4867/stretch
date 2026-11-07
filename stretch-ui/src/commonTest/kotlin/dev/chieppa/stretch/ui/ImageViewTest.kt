package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ImageViewTest {
    @Test
    fun aLargePictureFitsUntilClickedThenShowsAtItsOwnSize() = runComposeUiTest {
        val image = ImageBitmap(2000, 1000)
        setContent { StretchTheme(StretchThemes.Ink) { ImageView(image, Modifier.size(400.dp, 400.dp), "picture") } }
        val fitted = onNodeWithContentDescription("picture", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(fitted.right - fitted.left <= 400.dp, "$fitted")
        onNodeWithContentDescription("picture").performClick()
        val actual = onNodeWithContentDescription("picture", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals((2000 / density.density).dp.value, (actual.right - actual.left).value, 1f)
    }
}
