package com.lilyan_lefevre.puzzleit.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A picture stored in a file (box photo, scanned piece, progress photo). Glide does the decoding off the main thread,
 * the EXIF rotation and the downsizing to [maxSizePx]; nothing is drawn until it is ready.
 */
@Composable
fun FileImage(path: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, maxSizePx: Int = 1024) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, path) {
        value = if (path.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { Glide.with(context).asBitmap().load(File(path)).override(maxSizePx).submit().get() }.getOrNull()
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription, modifier, contentScale = contentScale) }
}
