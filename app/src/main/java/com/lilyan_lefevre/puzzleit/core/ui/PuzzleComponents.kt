package com.lilyan_lefevre.puzzleit.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleColors
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleText

/*
 * The small building blocks every screen is made of. A screen never styles a raw Material button:
 * it calls one of these, so the look of the app is decided here and nowhere else.
 */

/** The 48 dp round button of the headers and of the map (back, edit, delete...): dark translucent disc, light icon. */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = PuzzleColors.scrim, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(iconSize))
    }
}

/** Back button + big title (+ optional small line under it): the header of the account, history and progress screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onBack)
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = PuzzleText.display(26), color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The citron call-to-action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    ) { ButtonLabel(text, icon) }
}

/** The quiet button next to a primary one (dismiss, sign out, "yes" / "no"...). */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    corner: Dp = 18.dp,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
    ) { ButtonLabel(text, null) }
}

@Composable
private fun ButtonLabel(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

/** A surface with a thin outline: the cards of the lists. Pass [onClick] to make it tappable. */
@Composable
fun PuzzleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    corner: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val shape = RoundedCornerShape(corner)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val inner: @Composable () -> Unit = { Column(Modifier.padding(contentPadding)) { content() } }
    if (onClick != null) Card(onClick, modifier, shape = shape, colors = colors, border = border) { inner() }
    else Card(modifier, shape = shape, colors = colors, border = border) { inner() }
}

/** Centred grey text: the "nothing here yet" message of the lists. */
@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(40.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
    )
}

/** "Are you sure?" before something is deleted. Shown while it is in the composition; both buttons and a tap outside close it. */
@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }) { Text(confirmText) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
