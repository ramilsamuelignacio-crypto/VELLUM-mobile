package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.math.RoomUtils
import com.example.theme.VellumColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomShareModal(
    roomId: String,
    collaboratorCount: Int,
    onClose: () -> Unit,
    onJoinRoom: (String) -> Unit,
    onCreateNewRoom: () -> Unit,
    onShareQr: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val shareUrl = remember(roomId) { RoomUtils.buildRoomShareUrl(roomId) }
    var copiedCode by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var joinInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }
    val errorText = stringResource(R.string.room_join_error)

    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, shareUrl) {
        value = withContext(Dispatchers.IO) {
            RoomUtils.generateQrBitmap(shareUrl, 448)?.asImageBitmap()
        }
    }

    LaunchedEffect(copiedCode) {
        if (copiedCode) {
            delay(2000)
            copiedCode = false
        }
    }

    LaunchedEffect(copiedLink) {
        if (copiedLink) {
            delay(2000)
            copiedLink = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = VellumColors.PaperWhite,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(20.dp))
                .testTag("room_share_modal")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VellumColors.CobaltTint)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = "Room QR",
                                tint = VellumColors.CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Room Code & QR Invite",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VellumColors.InkCharcoal
                            )
                            Text(
                                text = "$collaboratorCount ${if (collaboratorCount == 1) "artist" else "artists"} currently in $roomId",
                                fontSize = 12.sp,
                                color = VellumColors.SlateSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("room_modal_close")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close room dialog",
                            tint = VellumColors.SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = VellumColors.HairlineBorder)

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // QR & Room Code card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(VellumColors.Subsurface)
                            .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(144.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            val bmp = qrBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "QR code for room $roomId",
                                    modifier = Modifier
                                        .size(128.dp)
                                        .testTag("room_qr_image")
                                )
                            } else {
                                Text("Generating QR...", fontSize = 11.sp, color = VellumColors.SlateMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Room Code", fontSize = 11.sp, color = VellumColors.SlateSecondary)
                            Text(
                                text = roomId,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = VellumColors.InkCharcoal,
                                modifier = Modifier.testTag("room_code_display")
                            )
                        }

                        Text(
                            text = "Scan the QR code from another device or share the room code to sketch together.",
                            fontSize = 12.sp,
                            color = VellumColors.SlateSecondary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboard.setText(AnnotatedString(roomId))
                                        copiedCode = true
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("copy_room_code")
                            ) {
                                Icon(
                                    imageVector = if (copiedCode) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = if (copiedCode) VellumColors.EmeraldGreen else VellumColors.InkCharcoal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (copiedCode) "Code Copied" else "Copy Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VellumColors.InkCharcoal
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VellumColors.CobaltBlue)
                                    .clickable {
                                        clipboard.setText(AnnotatedString(shareUrl))
                                        copiedLink = true
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("copy_room_link")
                            ) {
                                Icon(
                                    imageVector = if (copiedLink) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Link",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (copiedLink) "Link Copied" else "Copy Link",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, VellumColors.HairlineBorder, RoundedCornerShape(8.dp))
                                    .clickable(onClick = onShareQr)
                                    .padding(horizontal = 10.dp)
                                    .testTag("share_room_qr_image")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share QR image",
                                    tint = VellumColors.InkCharcoal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Share QR",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VellumColors.InkCharcoal
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = VellumColors.HairlineBorder)

                    // Join by Code
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Join another room by code",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VellumColors.InkCharcoal
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        onCreateNewRoom()
                                        onClose()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("create_fresh_room")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create fresh room",
                                    tint = VellumColors.CobaltBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Create fresh room",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VellumColors.CobaltBlue
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = joinInput,
                                onValueChange = {
                                    joinInput = it
                                    if (joinError != null) joinError = null
                                },
                                placeholder = {
                                    Text("e.g. k7m2px", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_input")
                            )

                            Button(
                                onClick = {
                                    val normalized = RoomUtils.normalizeRoomCode(joinInput)
                                    if (normalized.isEmpty()) {
                                        joinError = errorText
                                    } else {
                                        joinError = null
                                        onJoinRoom(normalized)
                                        onClose()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VellumColors.InkCharcoal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("join_room_submit")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Join Room",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        if (joinError != null) {
                            Text(
                                text = joinError!!,
                                fontSize = 12.sp,
                                color = VellumColors.DangerRed
                            )
                        }
                    }
                }
            }
        }
    }
}
