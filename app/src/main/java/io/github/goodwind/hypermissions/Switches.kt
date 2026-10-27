package io.github.goodwind.hypermissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A group of permissions is all on, all off, or some of each (the slider rests in the middle). */
enum class Position { On, Off, Partial }

val Ink.Accent get() = Color(0xFF0A84FF)
private val OffTrack = Color(0xFF7C7C82)

/** The slider of the app's icon: a pill with a white knob, blue when on. */
@Composable
fun Slider(position: Position, modifier: Modifier = Modifier) {
    val track = when (position) {
        Position.On -> Ink.Accent
        Position.Partial -> Ink.Orange
        Position.Off -> OffTrack
    }
    Box(modifier.size(width = 46.dp, height = 28.dp).clip(RoundedCornerShape(14.dp)).background(track).padding(2.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .align(
                    when (position) {
                        Position.On -> Alignment.CenterEnd
                        Position.Partial -> Alignment.Center
                        Position.Off -> Alignment.CenterStart
                    },
                )
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** A setting: its name and its slider, under the remote's focus plate. OK turns it. */
@Composable
fun SwitchRow(label: String, position: Position, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    FocusRow(onClick, modifier = modifier, focusRequester = focusRequester) { focused ->
        BasicText(
            label,
            style = Text.Body.copy(color = if (focused) Ink.OnFocus else Ink.Label),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
        )
        Slider(position)
    }
}
