package com.novelchat.ui.creation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novelchat.data.model.Message
import com.novelchat.data.model.Role
import com.novelchat.ui.theme.HiddenNoteDot
import com.novelchat.ui.theme.NarratorBg
import com.novelchat.ui.theme.NarratorText

val dotSize = (16f / 4).sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    role: Role?,
    isProtagonist: Boolean,
    onDoubleTap: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isNarrator = message.type == Message.TYPE_NARRATOR
    var showHiddenNote by remember { mutableStateOf(false) }

    if (isNarrator) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NarratorBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onDoubleTap() },
                        onLongClick = {
                            if (message.hasHiddenNote) showHiddenNote = !showHiddenNote
                        }
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    TextWithDot(
                        text = message.text,
                        showDot = message.hasHiddenNote && !showHiddenNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = NarratorText,
                        textAlign = TextAlign.Center
                    )
                    if (message.hasHiddenNote && showHiddenNote) {
                        HiddenNoteContent(message.hiddenNote)
                    }
                }
            }
        }
    } else {
        val isRight = isProtagonist
        val bubbleColor = if (isRight) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
        val textColor = if (isRight) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        val avatar = RoleAvatar(role, Modifier.size(44.dp))

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRight) {
                avatar
                Spacer(Modifier.width(12.dp))
            }

            Column(
                horizontalAlignment = if (isRight) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                // 名字
                if (role != null) {
                    Text(role.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = try { Color(android.graphics.Color.parseColor(role.color)) }
                                catch (_: Exception) { MaterialTheme.colorScheme.outline },
                        modifier = Modifier.padding(bottom = 2.dp))
                }

                // 气泡（宽度自适应文字）
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isRight) 16.dp else 4.dp,
                        topEnd = if (isRight) 4.dp else 16.dp,
                        bottomStart = 16.dp, bottomEnd = 16.dp
                    ),
                    color = bubbleColor,
                    modifier = Modifier
                        .widthIn(min = 40.dp, max = 280.dp)
                        .combinedClickable(
                            onClick = { onDoubleTap() },
                            onLongClick = {
                                if (message.hasHiddenNote) showHiddenNote = !showHiddenNote
                            }
                        )
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        TextWithDot(text = message.text,
                            showDot = message.hasHiddenNote && !showHiddenNote,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = textColor,
                            textAlign = if (isRight) TextAlign.End else TextAlign.Start)
                    }
                }

                // 隐藏标注展开
                if (message.hasHiddenNote && showHiddenNote) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)) {
                        Text(message.hiddenNote ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = if (isRight) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.padding(8.dp))
                    }
                }
            }

            if (isRight) {
                Spacer(Modifier.width(8.dp))
                avatar
            }
        }
    }
}

@Composable
private fun TextWithDot(
    text: String,
    showDot: Boolean,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start
) {
    if (showDot) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "●",
                color = HiddenNoteDot,
                fontSize = dotSize,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = text,
                style = style,
                color = color,
                textAlign = textAlign,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HiddenNoteContent(noteText: String?) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = noteText ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun RoleAvatar(
    role: Role?,
    modifier: Modifier = Modifier
) {
    if (role == null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ) {}
        return
    }

    val bgColor = try {
        Color(android.graphics.Color.parseColor(role.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (role.avatarType == Role.AVATAR_TEXT && role.avatarValue.isNotBlank()) {
                    role.avatarValue.take(2)
                } else {
                    role.name.take(1)
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
