package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LayoutTest {
    @Test
    fun gridFitsAsManyColumnsAsTheWidthAllows() {
        assertEquals(3, gridColumns(520f, 160f, 12f))
        assertEquals(2, gridColumns(500f, 160f, 12f))
        assertEquals(1, gridColumns(80f, 160f, 12f))
    }

    @Test
    fun gridPlacesCellsInRows() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Grid(Modifier.width(520.dp), minCell = 160.dp, gap = 10.dp) {
                    repeat(4) { Box(Modifier.height(40.dp).testTag("cell$it")) }
                }
            }
        }
        onNodeWithTag("cell0").assertWidthIsEqualTo(166.dp)
        onNodeWithTag("cell2").assertLeftPositionInRootIsEqualTo(352.dp)
        onNodeWithTag("cell3").assertTopPositionInRootIsEqualTo(50.dp).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun fieldsTakeIconsOnBothSides() = runComposeUiTest {
        var help = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                TextField(
                    "", {}, placeholder = "Name",
                    leading = { FieldIcon(StretchIcons.File) },
                    trailing = { FieldIcon(StretchIcons.Help, contentDescription = "About names", onClick = { help++ }) },
                )
                NumberField(4f, {}, "Size", trailing = { FieldIcon(StretchIcons.Info, contentDescription = "About size") })
            }
        }
        onNodeWithContentDescription("About names").performClick()
        assertEquals(1, help)
        onNodeWithContentDescription("About size").assertExists()
    }

    @Test
    fun loadersAreDescribed() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Spinner("Saving")
                Skeleton(Modifier.width(80.dp).height(12.dp).testTag("bone"))
                SkeletonText(3)
            }
        }
        onNodeWithContentDescription("Saving").assertExists()
        onNodeWithTag("bone").assertWidthIsEqualTo(80.dp)
    }
}
