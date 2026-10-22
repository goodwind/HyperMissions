package io.github.goodwind.hypermissions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors after Aerial's tvOS-like set: dark surfaces, white text, a near-white plate on the focused control. */
object Ink {
    val Background = Color(0xFF111113)
    val Panel = Color(0xFF242427)
    val Label = Color(0xFFFFFFFF)
    val Secondary = Color(0x99EBEBF5)
    val Fill = Color(0x2E787880)
    val Focus = Color(0xFFF5F5F7)
    val OnFocus = Color(0xFF1D1D1F)
    val Red = Color(0xFFFF453A)
    val Orange = Color(0xFFFF9F0A)
}

object Text {
    val LargeTitle = TextStyle(color = Ink.Label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val Headline = TextStyle(color = Ink.Label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(color = Ink.Label, fontSize = 16.sp)
    val Callout = TextStyle(color = Ink.Secondary, fontSize = 14.sp)
    val Caption = TextStyle(color = Ink.Label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

/** A Material Symbols glyph (960-unit box, y from -960 to 0) in [color]. */
@Composable
fun Glyph(path: String, color: Color, size: Dp) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(Modifier.size(size)) {
        scale(this.size.minDimension / 960f, pivot = Offset.Zero) {
            translate(0f, 960f) { drawPath(parsed, color) }
        }
    }
}

/** A row that can be focused with the remote and pressed with OK: a near-white plate while focused. */
@Composable
fun FocusRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Ink.Focus else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content(focused) }
}

/** A glyph over a short name, as in Android's own App Info. */
@Composable
fun ActionButton(
    path: String,
    label: String,
    focusRequester: FocusRequester? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val fg = when {
        focused -> Ink.OnFocus
        destructive -> Ink.Red
        else -> Ink.Label
    }
    Column(
        Modifier
            .width(140.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) Ink.Focus else Ink.Fill)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Glyph(path, fg, 26.dp)
        Spacer(Modifier.height(4.dp))
        BasicText(label, style = Text.Caption.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One setting: a name and its state; a null state makes it a link (a chevron). */
@Composable
fun AccessRow(label: String, state: String?, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    FocusRow(onClick, focusRequester = focusRequester) { focused ->
        val fg = if (focused) Ink.OnFocus else Ink.Label
        val secondary = if (focused) Ink.OnFocus else Ink.Secondary
        BasicText(label, style = Text.Body.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (state != null) {
            BasicText(state, style = Text.Body.copy(color = secondary))
        } else {
            Glyph(GlyphPaths.CHEVRON_RIGHT, secondary, 16.dp)
        }
    }
}

/** What went wrong (orange) or what was done (grey) with the last change. */
@Composable
fun StatusLines(message: String?, notice: String?) {
    message?.let { BasicText(it, style = Text.Caption.copy(color = Ink.Orange), modifier = Modifier.padding(top = 12.dp)) }
    notice?.let { BasicText(it, style = Text.Caption.copy(color = Ink.Secondary), modifier = Modifier.padding(top = 12.dp)) }
}

@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}
