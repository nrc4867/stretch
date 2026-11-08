package dev.chieppa.stretch.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.handPointer

/**
 * A [picture] loaded through [images], no wider than the width it asks for or else its own, and opening its link
 * through [onLinkClick] when it has one. One that has no source or does not load shows as a marked placeholder.
 */
@Composable
internal fun PictureView(picture: Picture, images: ImageSource?, onLinkClick: (String) -> Unit) {
    val loaded: ImageBitmap? by produceState<ImageBitmap?>(null, picture.url, images) {
        value = runCatching { images?.load(picture.url) }.getOrNull()
    }
    val link = picture.link
    val opens = if (link != null) Modifier.handPointer().clickable(role = Role.Button) { onLinkClick(link) } else Modifier
    val shown = loaded
    if (shown == null) {
        Box(opens) { MissingPicture(StretchIcons.Image, picture.alt.ifEmpty { picture.url }) }
        return
    }
    val width = picture.width?.dp ?: (shown.width / LocalDensity.current.density).dp
    Image(shown, picture.alt, opens.widthIn(max = width).fillMaxWidth())
}
