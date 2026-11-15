package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** How far loading a picture has come. */
internal sealed interface PictureLoad {
    data object Loading : PictureLoad

    class Shown(val image: ImageBitmap) : PictureLoad

    /** It has no source, no [ImageSource] was given, or it did not load. */
    data object Failed : PictureLoad
}

/** Loads [url] through [images], [PictureLoad.Loading] until it has. */
@Composable
internal fun rememberPictureLoad(url: String, images: ImageSource?): PictureLoad =
    produceState<PictureLoad>(PictureLoad.Loading, url, images) {
        val image = if (url.isEmpty()) null else runCatching { images?.load(url) }.getOrNull()
        value = image?.let(PictureLoad::Shown) ?: PictureLoad.Failed
    }.value

/** The size [picture] shows at: the width it asks for, or else its own, keeping its shape. */
internal fun pictureSize(picture: Picture, image: ImageBitmap, density: Density): DpSize {
    val natural = with(density) { image.width.toDp() }
    val width = picture.width?.dp ?: natural
    val height = if (image.width == 0) 0.dp else width * (image.height.toFloat() / image.width)
    return DpSize(width, height)
}

/** The tallest picture that shows in its line of text, as a badge or an icon does, rather than under it. */
internal val INLINE_PICTURE_HEIGHT = 32.dp
