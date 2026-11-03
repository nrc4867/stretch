package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CardTest {
    @Test
    fun artFillsWhatTheCaptionLeaves() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Box(Modifier.size(300.dp, 200.dp)) {
                    ArtCard("Shot 1", Modifier.fillMaxSize(), detail = "24 f") { Box(Modifier.fillMaxSize().testTag("art")) }
                }
            }
        }
        val art = onNodeWithTag("art").getUnclippedBoundsInRoot()
        val height = art.bottom - art.top
        assertTrue(height > 150.dp && height <= 164.dp, "art is $height tall")
    }

    @Test
    fun aCardInAScrollingColumnKeepsItsMinimum() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ArtCard("Shot 1", Modifier.width(200.dp).testTag("card")) { Box(Modifier.fillMaxSize()) }
                }
            }
        }
        onNodeWithTag("card").assertHeightIsEqualTo(120.dp)
    }

    @Test
    fun onlyACardWithAnOnClickIsAControl() = runComposeUiTest {
        var clicks = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                ArtCard("Shot 1", Modifier.size(200.dp), onClick = { clicks++ }) {}
                ArtCard("Fennec", Modifier.size(200.dp).testTag("still")) {}
            }
        }
        onNodeWithText("Shot 1").assertHasClickAction().performClick()
        assertEquals(1, clicks)
        onNodeWithTag("still").assertHasNoClickAction()
    }

    @Test
    fun aButtonInTheCaptionKeepsItsClickToItself() = runComposeUiTest {
        var card = 0
        var button = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                ArtCard(
                    "Fennec",
                    Modifier.size(400.dp, 240.dp),
                    size = CardSize.Large,
                    detail = "Build",
                    onClick = { card++ },
                    trailing = { Button("Continue", { button++ }) },
                ) {}
            }
        }
        onNodeWithText("Continue").performClick()
        assertEquals(1, button)
        assertEquals(0, card)
    }

    @Test
    fun selectedCardsAnnounceIt() = runComposeUiTest {
        var picked by mutableStateOf(false)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                ArtCard("Fennec", Modifier.size(200.dp), selected = picked, onClick = { picked = !picked }) {}
            }
        }
        onNodeWithText("Fennec").assertIsNotSelected().performClick()
        onNodeWithText("Fennec").assertIsSelected()
    }

    @Test
    fun aBadgeIsNamed() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                ArtCard("Ash fox", Modifier.size(200.dp), badge = "Newer version in the library") {}
            }
        }
        onNodeWithContentDescription("Newer version in the library").assertExists()
    }

    @Test
    fun addCardsClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                AddCard("New character", { clicks++ }, Modifier.width(200.dp).height(80.dp))
            }
        }
        onNodeWithText("New character").performClick()
        assertEquals(1, clicks)
    }
}
