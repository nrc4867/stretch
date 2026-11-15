package dev.chieppa.stretch.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.handPointer

/**
 * A [picture] loaded through [images], no wider than the width it asks for or else its own, and opening its link
 * through [onLinkClick] when it has one. One that has no source or does not load shows as a marked placeholder.
 */
@Composable
internal fun PictureView(picture: Picture, images: ImageSource?, onLinkClick: (String) -> Unit) {
    PictureView(picture, rememberPictureLoad(picture.url, images), onLinkClick)
}

/** [picture] as far as [load] has come: nothing yet while it loads, a marked placeholder when it failed. */
@Composable
internal fun PictureView(picture: Picture, load: PictureLoad, onLinkClick: (String) -> Unit) {
    val opens = Modifier.opensLink(picture.link, onLinkClick)
    when (load) {
        PictureLoad.Loading -> Unit
        PictureLoad.Failed -> Box(opens) { MissingPicture(StretchIcons.Image, picture.alt.ifEmpty { picture.url }) }
        is PictureLoad.Shown -> {
            val width = pictureSize(picture, load.image, LocalDensity.current).width
            Image(load.image, picture.alt, opens.widthIn(max = width).fillMaxWidth())
        }
    }
}

/** Opens [link] through [onLinkClick] on a click, under a pointing hand; nothing when there is no link. */
internal fun Modifier.opensLink(link: String?, onLinkClick: (String) -> Unit): Modifier =
    if (link != null) handPointer().clickable(role = Role.Button) { onLinkClick(link) } else this
