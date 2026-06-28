package com.novelchat.ui.creation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelchat.data.model.Message
import com.novelchat.data.model.Role
import com.novelchat.ui.theme.HiddenNoteDot
import com.novelchat.ui.theme.NarratorBg
import com.novelchat.ui.theme.NarratorText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    role: Role?,
    isProtagonist: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNarrator = message.type == Message.TYPE_NARRATOR
    var showHiddenNote by remember { mutableStateOf(false) }

    if (isNarrator) {
        // 旁白：居中显示
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NarratorBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (message.hasHiddenNote) showHiddenNote = !showHiddenNote
                        }
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NarratorText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    // 隐藏附注小圆点
                    if (message.hasHiddenNote) {
                        HiddenNoteDotIndicator(showHiddenNote, message.hiddenNote)
                    }
                }
            }
        }
    } else {
        // 对话：左对齐=他人，右对齐=主角
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

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp, horizontal = 8.dp),
            horizontalArrangement = if (isRight) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isRight) {
                // 他人：左侧显示头像
                RoleAvatar(role, Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                horizontalAlignment = if (isRight) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                // 角色名
                if (!isRight && role != null) {
                    Text(
                        text = role.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = try {
                            Color(android.graphics.Color.parseColor(role.color))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isRight) 12.dp else 4.dp,
                        topEnd = if (isRight) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    color = bubbleColor,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (message.hasHiddenNote) showHiddenNote = !showHiddenNote
                                else onLongClick()
                            }
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                        if (message.hasHiddenNote) {
                            HiddenNoteDotIndicator(showHiddenNote, message.hiddenNote)
                        }
                    }
                }
            }

            if (isRight) {
                Spacer(modifier = Modifier.width(6.dp))
                // 主角：右侧显示头像
                RoleAvatar(role, Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun HiddenNoteDotIndicator(
    expanded: Boolean,
    noteText: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!expanded) {
            Text(
                text = "●",
                color = HiddenNoteDot,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { it }
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = noteText ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun RoleAvatar(
    role: Role?,
    modifier: Modifier = Modifier
) {
    if (role == null) {
        // 无角色时的占位
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
