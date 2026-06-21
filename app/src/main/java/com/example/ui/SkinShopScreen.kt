package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.GameViewModel

@Composable
fun SkinShopScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    onStartGame: () -> Unit
) {
    val currentHighScore = viewModel.getPersonalHighScore()
    val equippedSkinId by viewModel.selectedSkinId.collectAsState()

    // Find currently selected skin in UI list for active canvas drawing preview
    val uiMockSkins = viewModel.skins

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF1B2E4A),
                        Color(0xFF0C1924)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Title Header Block
            Text(
                text = "阿威会飞天更衣室",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            // Current Personal best score helper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "你的历史最强纪录是: ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "$currentHighScore 分",
                    color = Color(0xFFFFEB3B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skins list container (Lazy Column scrollable)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("skin_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiMockSkins) { skin ->
                    val isUnlocked = currentHighScore >= skin.unlockScore
                    val isEquipped = skin.id == equippedSkinId

                    // Calculate card parameters
                    val cardBacking = if (isEquipped) {
                        Color(0xFF263238).copy(alpha = 0.9f)
                    } else if (isUnlocked) {
                        Color(0xFF1F2E3D).copy(alpha = 0.65f)
                    } else {
                        Color(0xFF1A222B).copy(alpha = 0.5f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) {
                                viewModel.selectSkin(skin.id)
                            }
                            .shadow(2.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBacking),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isEquipped) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Skin avatar vector preview card
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(46.dp)) {
                                    val r = 16f
                                    val ax = size.width / 2f
                                    val ay = size.height / 2f

                                    // Bird body
                                    drawCircle(
                                        color = Color(skin.hexColor),
                                        radius = r,
                                        center = Offset(ax, ay)
                                    )

                                    // Special cape for superhero
                                    if (skin.id == "hero") {
                                        val capePath = Path().apply {
                                            moveTo(ax - r * 0.8f, ay + r * 0.1f)
                                            quadraticTo(
                                                ax - r * 1.8f, ay + r * 0.8f,
                                                ax - r * 1.9f, ay + r * 1.3f
                                            )
                                            lineTo(ax - r * 1.2f, ay + r * 1.4f)
                                            close()
                                        }
                                        drawPath(path = capePath, color = Color(0xFFD50000))
                                    }

                                    // Wing
                                    drawOval(
                                        color = Color(skin.secondaryColor),
                                        topLeft = Offset(ax - r * 0.8f, ay - r * 0.4f),
                                        size = Size(r * 1.1f, r * 1.1f)
                                    )

                                    // Nerd Glasses specs
                                    val glassRadius = r * 0.38f
                                    val leftGlassCenter = Offset(ax + r * 0.25f, ay - r * 0.12f)
                                    val rightGlassCenter = Offset(ax + r * 0.8f, ay - r * 0.1f)

                                    val rimColor = if (skin.id == "neon") Color(0xFF00E5FF) else Color.Black
                                    val lensCol = if (skin.id == "neon") Color(0xFF00E5FF).copy(alpha = 0.6f) else Color.White

                                    drawCircle(color = rimColor, radius = glassRadius, center = leftGlassCenter)
                                    drawCircle(color = lensCol, radius = glassRadius * 0.72f, center = leftGlassCenter)

                                    if (skin.id != "neon") {
                                        drawLine(color = rimColor, start = leftGlassCenter, end = rightGlassCenter, strokeWidth = 2f)
                                    }

                                    // Beak mouth
                                    val beakP = Path().apply {
                                        moveTo(ax + r * 0.8f, ay + r * 0.1f)
                                        lineTo(ax + r * 1.3f, ay + r * 0.25f)
                                        lineTo(ax + r * 0.8f, ay + r * 0.45f)
                                        close()
                                    }
                                    drawPath(path = beakP, color = Color(0xFFFF9100))

                                    // Special Crown on top of his head
                                    if (skin.id == "golden") {
                                        val crownP = Path().apply {
                                            moveTo(ax - r * 0.4f, ay - r * 0.82f)
                                            lineTo(ax - r * 0.5f, ay - r * 1.35f)
                                            lineTo(ax - r * 0.15f, ay - r * 1.05f)
                                            lineTo(ax, ay - r * 1.55f)
                                            lineTo(ax + r * 0.15f, ay - r * 1.05f)
                                            lineTo(ax + r * 0.5f, ay - r * 1.35f)
                                            lineTo(ax + r * 0.4f, ay - r * 0.82f)
                                            close()
                                        }
                                        drawPath(path = crownP, color = Color(0xFFFFD700))
                                    } else if (skin.id == "classic") {
                                        // Line strap
                                        drawLine(
                                            color = Color.Red,
                                            start = Offset(ax - r * 0.7f, ay - r * 0.6f),
                                            end = Offset(ax + r * 0.7f, ay - r * 0.6f),
                                            strokeWidth = 3f
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Skin text info details Column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = skin.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = skin.description,
                                    fontSize = 11.sp,
                                    color = if (isUnlocked) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f),
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Locking statuses indicator boxes
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        color = if (isEquipped) {
                                            Color(0xFF4CAF50).copy(alpha = 0.18f)
                                        } else if (isUnlocked) {
                                            Color(0xFF2196F3).copy(alpha = 0.15f)
                                        } else {
                                            Color(0xFFFF5252).copy(alpha = 0.15f)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isEquipped) {
                                            Color(0xFF4CAF50)
                                        } else if (isUnlocked) {
                                            Color(0xFF2196F3)
                                        } else {
                                            Color(0xFFFF5252).copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isEquipped) {
                                    Text(
                                        text = "已装配",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                } else if (isUnlocked) {
                                    Text(
                                        text = "可装配",
                                        color = Color(0xFF2196F3),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "🔒 锁定中",
                                            color = Color(0xFFFF5252),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "需 ${skin.unlockScore} 积分",
                                            color = Color(0xFFFF5252).copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back controllers row are bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("返回菜单", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Fly", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("立即飞天!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
