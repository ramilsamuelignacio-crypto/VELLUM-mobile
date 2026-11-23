package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.VellumColors

@Composable
fun TopBar(
    roomId: String,
    connected: Boolean,
    collaboratorCount: Int,
    strokeCount: Int,
    isExpanded: Boolean,
    onNewRoom: () -> Unit,
    onExportPng: () -> Unit,
    onOpenRoomModal: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VellumColors.PaperWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(if (isExpanded) 56.dp else 48.dp)
                    .padding(horizontal = if (isExpanded) 20.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Zone 1: Brand wordmark + quick room code pill on compact/medium
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (isExpanded) 18.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = VellumColors.InkCharcoal,
                        maxLines = 1
                    )
                    if (!isExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .testTag("top_bar_room_chip")
                                .widthIn(max = 120.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.HoverSurface)
                                .clickable(onClick = onOpenRoomModal)
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = "Room code",
                                tint = VellumColors.SlateSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = roomId,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = VellumColors.InkCharcoal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Zone 2: Desktop / Tablet center metadata
                if (isExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .testTag("top_bar_room_button")
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenRoomModal)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = "Room QR",
                                tint = VellumColors.SlateMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Room $roomId",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = VellumColors.InkCharcoal
                            )
                        }
                        Text("·", color = VellumColors.SlateSecondary, fontSize = 12.sp)
                        Text(
                            text = "$collaboratorCount ${if (collaboratorCount == 1) "artist" else "artists"} online",
                            fontSize = 12.sp,
                            color = VellumColors.SlateSecondary
                        )
                        Text("·", color = VellumColors.SlateSecondary, fontSize = 12.sp)
                        Text(
                            text = "$strokeCount ${if (strokeCount == 1) "stroke" else "strokes"}",
                            fontSize = 12.sp,
                            color = VellumColors.SlateSecondary
                        )
                        if (!connected) {
                            Text("·", color = VellumColors.SlateSecondary, fontSize = 12.sp)
                            Text(
                                text = "Reconnecting",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VellumColors.AmberWarn
                            )
                        }
                    }
                }

                // Zone 3: Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isExpanded) {
                        TopActionPill(
                            label = stringResource(R.string.action_guide),
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            containerColor = Color.Transparent,
                            contentColor = VellumColors.SlateSecondary,
                            onClick = onOpenGuide,
                            testTag = "guide_button"
                        )
                        TopActionPill(
                            label = stringResource(R.string.action_new_room),
                            icon = Icons.Filled.Add,
                            containerColor = VellumColors.HoverSurface,
                            contentColor = VellumColors.InkCharcoal,
                            onClick = onNewRoom,
                            testTag = "new_room_button"
                        )
                        TopActionPill(
                            label = stringResource(R.string.action_export_png),
                            icon = Icons.Filled.Download,
                            containerColor = VellumColors.HoverSurface,
                            contentColor = VellumColors.InkCharcoal,
                            onClick = onExportPng,
                            testTag = "export_png_button"
                        )
                        TopActionPill(
                            label = stringResource(R.string.action_share_room),
                            icon = Icons.Filled.Share,
                            containerColor = VellumColors.CobaltBlue,
                            contentColor = Color.White,
                            onClick = onOpenRoomModal,
                            testTag = "share_room_button"
                        )
                    } else {
                        IconButton(
                            onClick = onOpenGuide,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.action_guide),
                                tint = VellumColors.SlateSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onNewRoom,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.HoverSurface)
                                .testTag("new_room_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.action_new_room),
                                tint = VellumColors.InkCharcoal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .testTag("share_room_button")
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(VellumColors.CobaltBlue)
                                .clickable(onClick = onOpenRoomModal)
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.action_share_short),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_share_short),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = VellumColors.HairlineBorder,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun TopActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .testTag(testTag)
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
