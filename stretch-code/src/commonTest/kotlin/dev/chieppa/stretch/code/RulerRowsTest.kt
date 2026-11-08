package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class RulerRowsTest {
    @Test
    fun aShortViewPutsItsUnitsBesideItsItems() = runComposeUiTest {
        val list = LazyListState()
        var count by mutableStateOf(3)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LazyColumn(Modifier.size(100.dp, 400.dp), list) {
                    items(count) { Box(Modifier.height(if (it == 1) 40.dp else 20.dp)) }
                }
            }
        }
        waitForIdle()
        val rows = RulerRows(floatArrayOf(1f, 1f, 1f))
        val fit = assertNotNull(runOnIdle { rows.fit(list) })
        assertEquals(80f, fit.end)
        assertEquals(20f, fit.at(1f))
        assertEquals(40f, fit.at(1.5f))
        assertEquals(60f, fit.at(2f))
        count = 100
        waitForIdle()
        assertNull(runOnIdle { RulerRows(FloatArray(100) { 1f }).fit(list) })
    }

    @Test
    fun countsWrappedRows() {
        assertEquals(1, wrappedRows("", 10))
        assertEquals(1, wrappedRows("x".repeat(10), 10))
        assertEquals(2, wrappedRows("x".repeat(11), 10))
        assertEquals(2, wrappedRows("\t\t\tx", 10))
        assertEquals(1, wrappedRows("x".repeat(500), 0))
    }

    @Test
    fun placesItemsByTheirExtents() {
        val rows = RulerRows(floatArrayOf(1f, 3f, 1f))
        assertEquals(5f, rows.length)
        assertEquals(1f, rows.start(1))
        assertEquals(4f, rows.end(1))
        assertEquals(0, rows.indexAt(0.5f))
        assertEquals(1, rows.indexAt(1f))
        assertEquals(1, rows.indexAt(3.9f))
        assertEquals(2, rows.indexAt(4.5f))
        assertEquals(2, rows.indexAt(99f))
        assertEquals(0, RulerRows(FloatArray(0)).indexAt(3f))
    }
}
