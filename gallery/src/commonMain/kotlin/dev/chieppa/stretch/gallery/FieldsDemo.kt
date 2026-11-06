package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.SearchableDropdown
import dev.chieppa.stretch.ui.Breadcrumbs
import dev.chieppa.stretch.ui.NumberField
import dev.chieppa.stretch.ui.PathLabel
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.NumberSteppers
import dev.chieppa.stretch.ui.TextArea

private val bones = listOf("Head", "Neck", "Spine", "Hips", "Tail base", "Tail tip", "Left ear", "Right ear", "Left shoulder", "Right shoulder", "Left elbow", "Right elbow", "Left wrist", "Right wrist", "Left knee", "Right knee")

@Composable
internal fun BreadcrumbsDemo() {
    var path by remember { mutableStateOf(listOf("Dune walk", "Fennec", "Rig", "Left ear")) }
    Breadcrumbs(path, { it }, { item -> path = path.take(path.indexOf(item) + 1) })
    PathLabel("characters/fennec/rig/ears/left/LeftEarControls.rig", Stretch.type.valueSm, Stretch.colors.inkMuted, Modifier.width(220.dp))
}

@Composable
internal fun MoreFieldsDemo() {
    var notes by remember { mutableStateOf("") }
    var bone by remember { mutableStateOf(bones.first()) }
    var frames by remember { mutableFloatStateOf(24f) }
    TextArea(notes, { notes = it }, Modifier.fillMaxWidth(), placeholder = "Notes for this shot")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        SearchableDropdown(bones, bone, { bone = it }, { it }, "Bone", Modifier.width(200.dp))
        NumberField(frames, { frames = it }, "Frames", range = 1f..120f, steppers = NumberSteppers("Fewer frames", "More frames"))
    }
}
