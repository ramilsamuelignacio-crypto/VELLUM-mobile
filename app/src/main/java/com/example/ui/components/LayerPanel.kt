package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Layer
import com.example.theme.VellumColors

@Composable
fun LayerPanel(
    layers: List<Layer>,
    activeLayerId: String,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onMoveLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLayers = layers.reversed()

    Surface(
        color = VellumColors.PaperWhite,
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(VellumColors.HairlineBorder)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.action_layers),
                            tint = VellumColors.SlateSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_layers),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VellumColors.InkCharcoal
                        )
                    }
                    IconButton(
                        onClick = onAddLayer,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("layer_panel_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add_layer),
                            tint = VellumColors.InkCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(color = VellumColors.HairlineBorder)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    itemsIndexed(displayLayers, key = { _, l -> l.id }) { displayIdx, layer ->
                        val actualIdx = layers.size - 1 - displayIdx
                        val isActive = layer.id == activeLayerId
                        val canMoveUp = actualIdx < layers.size - 1
                        val canMoveDown = actualIdx > 0

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) VellumColors.CobaltTint else VellumColors.PaperWhite)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) VellumColors.CobaltBorder else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectLayer(layer.id) }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .testTag("layer_row_${layer.id}")
                        ) {
                            IconButton(
                                onClick = { onToggleVisibility(layer.id) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("layer_visibility_${layer.id}")
                            ) {
                                Icon(
                                    imageVector = if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle visibility for ${layer.name}",
                                    tint = if (layer.visible) VellumColors.SlateSecondary else VellumColors.SlateMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Text(
                                text = layer.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) VellumColors.InkCharcoal else VellumColors.SlateSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { onMoveLayer(actualIdx, actualIdx + 1) },
                                enabled = canMoveUp,
                                modifier = Modifier
                                    .size(26.dp)
                                    .alpha(if (canMoveUp) 1f else 0.25f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Move ${layer.name} up",
                                    tint = VellumColors.SlateSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveLayer(actualIdx, actualIdx - 1) },
                                enabled = canMoveDown,
                                modifier = Modifier
                                    .size(26.dp)
                                    .alpha(if (canMoveDown) 1f else 0.25f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Move ${layer.name} down",
                                    tint = VellumColors.SlateSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
