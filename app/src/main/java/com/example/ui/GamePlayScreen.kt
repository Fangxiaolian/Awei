package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.SoundFX
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.UiScreen
import kotlinx.coroutines.launch
import kotlin.random.Random

// Game Constants (DP units relative to 360x640 design grid)
private const val DESIGN_WIDTH = 360f
private const val DESIGN_HEIGHT = 640f

private const val GRAVITY = 0.35f
private const val JUMP_VELOCITY = -6.8f
private const val HORIZONTAL_SPEED = 2.4f
private const val PIPE_GAP = 145f
private const val PIPE_WIDTH = 58f
private const val PIPE_DISTANCE = 220f
private const val BIRD_RADIUS = 16f
private const val GROUND_HEIGHT = 80f

enum class PlayState {
    READY, PLAYING, GAMEOVER
}

data class Pipe(
    var x: Float,
    val topHeight: Float,
    val bottomHeight: Float,
    var passed: Boolean = false
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    val lifeTime: Float = 1.0f, // in seconds
    var age: Float = 0.0f
)

data class ParallaxCloud(
    val id: Int,
    var x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val speed: Float
)

data class Building(
    var x: Float,
    val width: Float,
    val height: Float,
    val color: Color
)

@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isMutedState = remember { mutableStateOf(SoundFX.getMuteStatus()) }

    // Selected skin colors
    val activeSkinId by viewModel.selectedSkinId.collectAsState()
    val activeSkin = viewModel.skins.find { it.id == activeSkinId } ?: viewModel.skins.first()
    
    val birdBodyColor = Color(activeSkin.hexColor)
    val birdAccentColor = Color(activeSkin.secondaryColor)

    // Game Variables
    var playState by remember { mutableStateOf(PlayState.READY) }
    var score by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    // Physical Position of Bird (relative to 360x640 design grid)
    var birdY by remember { mutableStateOf(DESIGN_HEIGHT / 2f) }
    var birdVY by remember { mutableStateOf(0f) }
    var birdAngle by remember { mutableStateOf(0f) }
    var wingFlapFrame by remember { mutableStateOf(0f) }

    // Screen shake trigger animation
    var shakePixels by remember { mutableStateOf(0f) }

    // Lists
    val pipes = remember { mutableStateListOf<Pipe>() }
    val particles = remember { mutableStateListOf<Particle>() }
    val clouds = remember { mutableStateListOf<ParallaxCloud>() }
    val buildings = remember { mutableStateListOf<Building>() }
    var groundOffset by remember { mutableStateOf(0f) }

    // Initialize parallax backgrounds
    LaunchedEffect(Unit) {
        // Initial Clouds
        clouds.clear()
        for (i in 0..4) {
            clouds.add(
                ParallaxCloud(
                    id = i,
                    x = Random.nextFloat() * DESIGN_WIDTH,
                    y = 40f + Random.nextFloat() * 140f,
                    width = 50f + Random.nextFloat() * 50f,
                    height = 20f + Random.nextFloat() * 15f,
                    speed = 0.1f + Random.nextFloat() * 0.15f
                )
            )
        }
        // Initial Background buildings
        buildings.clear()
        var bx = 0f
        while (bx < DESIGN_WIDTH + 100f) {
            val w = 40f + Random.nextFloat() * 45f
            val h = 60f + Random.nextFloat() * 120f
            buildings.add(Building(bx, w, h, Color(0xFF1E2F3F)))
            bx += w + 8f
        }
    }

    // Function to trigger a Jump Flap
    val performFlap = {
        if (!isPaused && playState != PlayState.GAMEOVER) {
            if (playState == PlayState.READY) {
                // Initialize game pipes on first flap!
                playState = PlayState.PLAYING
                pipes.clear()
                pipes.add(generatePipe(DESIGN_WIDTH + 60f))
                pipes.add(generatePipe(DESIGN_WIDTH + 60f + PIPE_DISTANCE))
                score = 0
            }
            birdVY = JUMP_VELOCITY
            SoundFX.playJump()

            // Jump smoke puff particle effects
            repeat(6) {
                particles.add(
                    Particle(
                        x = 100f, // Bird is locked at X=100
                        y = birdY + Random.nextFloat() * 10f - 5f,
                        vx = -1f - Random.nextFloat() * 1.5f,
                        vy = Random.nextFloat() * 1f - 0.5f,
                        size = 5f + Random.nextFloat() * 5f,
                        color = Color.White.copy(alpha = 0.7f),
                        lifeTime = 0.5f
                    )
                )
            }
        }
    }

    // Restart logic
    val restartGame = {
        birdY = DESIGN_HEIGHT / 2f
        birdVY = 0f
        birdAngle = 0f
        score = 0
        pipes.clear()
        particles.clear()
        isPaused = false
        playState = PlayState.READY
    }

    // Game Core Tick Loop
    LaunchedEffect(playState, isPaused) {
        if (playState == PlayState.PLAYING && !isPaused) {
            var lastTime = System.currentTimeMillis()
            while (playState == PlayState.PLAYING && !isPaused) {
                withFrameMillis { frameTime ->
                    val currentTime = System.currentTimeMillis()
                    val deltaSec = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    // Standard frame physics update (equivalent of ~60 ticks per sec)
                    // 1. Move Background clouds & buildings
                    for (cloud in clouds) {
                        cloud.x -= cloud.speed
                        if (cloud.x + cloud.width < 0) {
                            cloud.x = DESIGN_WIDTH + 10f
                        }
                    }
                    for (b in buildings) {
                        b.x -= 0.3f
                        if (b.x + b.width < 0) {
                            // Find the max X right now to loop back nicely
                            val maxX = buildings.maxOf { it.x + it.width }
                            b.x = maxX + 6f
                        }
                    }

                    // Move ground
                    groundOffset = (groundOffset - HORIZONTAL_SPEED) % 40f

                    // 2. Physics on Bird
                    birdVY += GRAVITY
                    birdY += birdVY

                    // Smooth rotation angle based on velocity
                    if (birdVY < 0f) {
                        birdAngle = (birdAngle + (birdVY * 4f - birdAngle) * 0.15f).coerceAtLeast(-25f)
                    } else {
                        birdAngle = (birdAngle + (birdVY * 5.5f - birdAngle) * 0.12f).coerceAtMost(70f)
                    }

                    // Flapping wing oscillation
                    wingFlapFrame = (wingFlapFrame + 0.18f) % 3f

                    // Jet trail effects for skin
                    when (activeSkinId) {
                        "neon" -> {
                            // Cyan pixels spark trail
                            if (Random.nextInt(4) == 0) {
                                particles.add(
                                    Particle(
                                        x = 100f - 14f,
                                        y = birdY + Random.nextFloat() * 10f - 5f,
                                        vx = -3f - Random.nextFloat() * 2f,
                                        vy = Random.nextFloat() * 1.5f - 0.75f,
                                        size = 3f + Random.nextFloat() * 4f,
                                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                        lifeTime = 0.4f
                                    )
                                )
                            }
                        }
                        "golden" -> {
                            // Sparkling stars trail
                            if (Random.nextInt(6) == 0) {
                                particles.add(
                                    Particle(
                                        x = 100f - 14f,
                                        y = birdY + Random.nextFloat() * 12f - 6f,
                                        vx = -2f - Random.nextFloat() * 1.5f,
                                        vy = Random.nextFloat() * 1f - 0.5f,
                                        size = 4f + Random.nextFloat() * 4f,
                                        color = Color(0xFFFFD700).copy(alpha = 0.9f),
                                        lifeTime = 0.6f
                                    )
                                )
                            }
                        }
                        "hero" -> {
                            // Red flowing wind line particles
                            if (Random.nextInt(5) == 0) {
                                particles.add(
                                    Particle(
                                        x = 100f - 16f,
                                        y = birdY + 4f,
                                        vx = -4f - Random.nextFloat() * 1f,
                                        vy = Random.nextFloat() * 0.5f - 0.25f,
                                        size = 2f + Random.nextFloat() * 3f,
                                        color = Color(0xFFE53935).copy(alpha = 0.7f),
                                        lifeTime = 0.30f
                                    )
                                )
                            }
                        }
                    }

                    // Update particle engines
                    val iterator = particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.age += 0.016f
                        p.alpha = (1.0f - p.age / p.lifeTime).coerceIn(0f, 1f)
                        // Apply velocities
                        var px = p.x + p.vx
                        var py = p.y + p.vy
                        // Draw scale update
                        if (p.age >= p.lifeTime) {
                            iterator.remove()
                        }
                    }

                    // 3. Pipes physics and score checking
                    for (i in pipes.indices) {
                        val pipe = pipes[i]
                        pipe.x -= HORIZONTAL_SPEED

                        // Scoring boundary detection
                        if (!pipe.passed && pipe.x < 100f) {
                            pipe.passed = true
                            score++
                            SoundFX.playPoint()

                            // Ring point celebration sparkles
                            repeat(10) {
                                val speedAngle = Random.nextDouble() * 2.0 * Math.PI
                                val speedMag = 2f + Random.nextFloat() * 3f
                                particles.add(
                                    Particle(
                                        x = 100f,
                                        y = birdY,
                                        vx = (Math.cos(speedAngle) * speedMag).toFloat(),
                                        vy = (Math.sin(speedAngle) * speedMag).toFloat(),
                                        size = 5f + Random.nextFloat() * 6f,
                                        color = Color(0xFFFFEB3B),
                                        lifeTime = 0.6f
                                    )
                                )
                            }
                        }
                    }

                    // Remove off-screen pipes & add fresh ones
                    if (pipes.isNotEmpty() && pipes[0].x < -PIPE_WIDTH) {
                        pipes.removeAt(0)
                        // Add new pipe based on the last pipe's position
                        val lastPipeX = if (pipes.isNotEmpty()) pipes[pipes.size - 1].x else DESIGN_WIDTH
                        pipes.add(generatePipe(lastPipeX + PIPE_DISTANCE))
                    }

                    // 4. Collision Detections (AABB boundaries relative to 360x640)
                    val groundThreshold = DESIGN_HEIGHT - GROUND_HEIGHT
                    // Hard hit check on ground or sky ceiling
                    if (birdY - BIRD_RADIUS <= 0f || birdY + BIRD_RADIUS >= groundThreshold) {
                        // Dead crash!
                        gameOverOccurred(viewModel, score, pipes, particles, birdY) {
                            playState = PlayState.GAMEOVER
                        }
                    } else {
                        // Check collisions with pipes
                        var collided = false
                        for (pipe in pipes) {
                            val birdLeft = 100f - BIRD_RADIUS
                            val birdRight = 100f + BIRD_RADIUS
                            val birdTop = birdY - BIRD_RADIUS
                            val birdBottom = birdY + BIRD_RADIUS

                            val pipeLeft = pipe.x
                            val pipeRight = pipe.x + PIPE_WIDTH

                            // Check bounding overlapping
                            if (birdRight > pipeLeft && birdLeft < pipeRight) {
                                // Within vertical columns X-range
                                if (birdTop < pipe.topHeight || birdBottom > (DESIGN_HEIGHT - GROUND_HEIGHT - pipe.bottomHeight)) {
                                    collided = true
                                    break
                                }
                            }
                        }

                        if (collided) {
                            gameOverOccurred(viewModel, score, pipes, particles, birdY) {
                                playState = PlayState.GAMEOVER
                            }
                        }
                    }

                    // Decay camera screen shake
                    if (shakePixels > 0f) {
                        shakePixels *= 0.85f
                        if (shakePixels < 1f) shakePixels = 0f
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_viewport")
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Core responsive scaling calculations
        val scaleX = widthPx / DESIGN_WIDTH
        val scaleY = heightPx / DESIGN_HEIGHT

        val touchModifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { performFlap() }
            )

        Box(modifier = touchModifier) {
            // Draw all game elements onto Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topOverlayColor = Color(0xFF142230)
                val bottomOverlayColor = Color(0xFF263D52)

                // Background gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topOverlayColor, bottomOverlayColor)
                    ),
                    size = size
                )

                // 1. Draw Buildings (Silhouette Parallax)
                for (b in buildings) {
                    val bxScaled = b.x * scaleX
                    val bwScaled = b.width * scaleX
                    val bhScaled = b.height * scaleY
                    val byScaled = (DESIGN_HEIGHT - GROUND_HEIGHT) * scaleY - bhScaled

                    drawRect(
                        color = b.color,
                        topLeft = Offset(bxScaled, byScaled),
                        size = Size(bwScaled, bhScaled)
                    )
                }

                // 2. Draw Parallax Clouds
                for (cloud in clouds) {
                    val cxScaled = cloud.x * scaleX
                    val cyScaled = cloud.y * scaleY
                    val cwScaled = cloud.width * scaleX
                    val chScaled = cloud.height * scaleY

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset(cxScaled, cyScaled),
                        size = Size(cwScaled, chScaled),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(chScaled / 2f, chScaled / 2f)
                    )
                }

                // 3. Draw Pipes (Steel gradient columns)
                for (pipe in pipes) {
                    val pipeXScaled = pipe.x * scaleX
                    val pipeWidthScaled = PIPE_WIDTH * scaleX
                    val topPipeYScaled = pipe.topHeight * scaleY
                    val bottomPipeYScaled = (DESIGN_HEIGHT - GROUND_HEIGHT - pipe.bottomHeight) * scaleY
                    val groundScaled = (DESIGN_HEIGHT - GROUND_HEIGHT) * scaleY

                    // Top pipe linear gradient
                    val pipeBrush = Brush.linearGradient(
                        colors = listOf(Color(0xFF81C784), Color(0xFF388E3C), Color(0xFF1B5E20)),
                        start = Offset(pipeXScaled, 0f),
                        end = Offset(pipeXScaled + pipeWidthScaled, 0f)
                    )

                    // Draw Top tubular body
                    drawRect(
                        brush = pipeBrush,
                        topLeft = Offset(pipeXScaled, 0f),
                        size = Size(pipeWidthScaled, topPipeYScaled)
                    )
                    // Top pipe outer lip
                    val lipHeight = 22f * scaleY
                    val lipWidthOffset = 4f * scaleX
                    drawRoundRect(
                        brush = pipeBrush,
                        topLeft = Offset(pipeXScaled - lipWidthOffset, topPipeYScaled - lipHeight),
                        size = Size(pipeWidthScaled + (lipWidthOffset * 2), lipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    // Draw highlight details
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(pipeXScaled, 0f),
                        size = Size(1.5f * scaleX, topPipeYScaled)
                    )

                    // Draw Bottom tubular body
                    drawRect(
                        brush = pipeBrush,
                        topLeft = Offset(pipeXScaled, bottomPipeYScaled),
                        size = Size(pipeWidthScaled, groundScaled - bottomPipeYScaled)
                    )
                    // Bottom pipe outer lip
                    drawRoundRect(
                        brush = pipeBrush,
                        topLeft = Offset(pipeXScaled - lipWidthOffset, bottomPipeYScaled),
                        size = Size(pipeWidthScaled + (lipWidthOffset * 2), lipHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                }

                // 4. Draw Ground (Scrolling floor)
                val groundYScaled = (DESIGN_HEIGHT - GROUND_HEIGHT) * scaleY
                val groundHeightScaled = GROUND_HEIGHT * scaleY
                // Mud soil base
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E))
                    ),
                    topLeft = Offset(0f, groundYScaled),
                    size = Size(size.width, groundHeightScaled)
                )
                // Grass surface line
                val grassHeight = 10f * scaleY
                drawRect(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(0f, groundYScaled),
                    size = Size(size.width, grassHeight)
                )
                // Diagonal neon green ground patterns
                var go = groundOffset * scaleX
                while (go < size.width + 40f) {
                    drawRect(
                        color = Color(0xFF81C784).copy(alpha = 0.5f),
                        topLeft = Offset(go, groundYScaled + grassHeight),
                        size = Size(6f * scaleX, groundHeightScaled / 2.5f)
                    )
                    go += 35f * scaleX
                }

                // 5. Draw Particle effects
                for (p in particles) {
                    val pxScaled = p.x * scaleX
                    val pyScaled = p.y * scaleY
                    val psScaled = p.size * scaleX

                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = psScaled / 2f,
                        center = Offset(pxScaled, pyScaled)
                    )
                }

                // 6. Draw "阿威" Ah Wei (Fitted inside 100(X) locked screen position)
                val axScaled = 100f * scaleX
                val ayScaled = birdY * scaleY
                val birdRadScaled = BIRD_RADIUS * scaleX

                rotate(degrees = birdAngle, pivot = Offset(axScaled, ayScaled)) {
                    // Body circle (Primary skin color)
                    drawCircle(
                        color = birdBodyColor,
                        radius = birdRadScaled,
                        center = Offset(axScaled, ayScaled)
                    )

                    // Cape styling for "hero" cape
                    if (activeSkinId == "hero") {
                        val capePath = Path().apply {
                            moveTo(axScaled - birdRadScaled * 0.8f, ayScaled + birdRadScaled * 0.1f)
                            quadraticTo(
                                axScaled - birdRadScaled * 1.8f, ayScaled + birdRadScaled * 0.8f,
                                axScaled - birdRadScaled * 1.9f, ayScaled + birdRadScaled * 1.3f
                            )
                            lineTo(axScaled - birdRadScaled * 1.2f, ayScaled + birdRadScaled * 1.4f)
                            close()
                        }
                        drawPath(
                            path = capePath,
                            color = Color(0xFFD50000)
                        )
                    }

                    // Flapping Wing
                    val wingOffsetState = when (wingFlapFrame.toInt()) {
                        1 -> -4f
                        2 -> 3f
                        else -> 0f
                    }
                    val wingRadX = birdRadScaled * 0.65f
                    val wingRadY = birdRadScaled * 0.45f
                    val wingCenterY = ayScaled + wingOffsetState * scaleY

                    // Wing color
                    drawOval(
                        color = birdAccentColor,
                        topLeft = Offset(axScaled - birdRadScaled * 0.8f, wingCenterY - wingRadY / 2f),
                        size = Size(wingRadX * 1.7f, wingRadY * 1.7f)
                    )

                    // Big Spectacles/Glasses (NERD retro style)
                    val glassRadius = birdRadScaled * 0.38f
                    val leftGlassCenter = Offset(axScaled + birdRadScaled * 0.3f, ayScaled - birdRadScaled * 0.15f)
                    val rightGlassCenter = Offset(axScaled + birdRadScaled * 0.85f, ayScaled - birdRadScaled * 0.12f)
                    
                    // Glass rim styles
                    val rimColor = if (activeSkinId == "neon") Color(0xFF00E5FF) else Color.Black
                    val glassFillColor = if (activeSkinId == "neon") Color(0xFF00E5FF).copy(alpha = 0.6f) else Color.White
                    
                    // Left lens
                    drawCircle(
                        color = rimColor,
                        radius = glassRadius,
                        center = leftGlassCenter
                    )
                    drawCircle(
                        color = glassFillColor,
                        radius = glassRadius * 0.72f,
                        center = leftGlassCenter
                    )
                    
                    // Connection bridge
                    if (activeSkinId != "neon") {
                        drawLine(
                            color = rimColor,
                            start = leftGlassCenter,
                            end = rightGlassCenter,
                            strokeWidth = 2.5f * scaleX
                        )
                    }

                    // Beak (Orange mouth for bird style)
                    val beakPath = Path().apply {
                        moveTo(axScaled + birdRadScaled * 0.85f, ayScaled + birdRadScaled * 0.1f)
                        lineTo(axScaled + birdRadScaled * 1.42f, ayScaled + birdRadScaled * 0.3f)
                        lineTo(axScaled + birdRadScaled * 0.82f, ayScaled + birdRadScaled * 0.55f)
                        close()
                    }
                    drawPath(
                        path = beakPath,
                        color = Color(0xFFFF9100)
                    )

                    // Head accessory decoration
                    when (activeSkinId) {
                        "classic" -> {
                            // Red retro headband wrapping around his forehead
                            val headbandLineY = ayScaled - birdRadScaled * 0.6f
                            drawLine(
                                color = Color.Red,
                                start = Offset(axScaled - birdRadScaled * 0.7f, headbandLineY),
                                end = Offset(axScaled + birdRadScaled * 0.7f, headbandLineY),
                                strokeWidth = 4f * scaleY
                            )
                        }
                        "golden" -> {
                            // Crown on head
                            val crownPath = Path().apply {
                                moveTo(axScaled - birdRadScaled * 0.4f, ayScaled - birdRadScaled * 0.82f)
                                lineTo(axScaled - birdRadScaled * 0.5f, ayScaled - birdRadScaled * 1.35f)
                                lineTo(axScaled - birdRadScaled * 0.15f, ayScaled - birdRadScaled * 1.05f)
                                lineTo(axScaled, ayScaled - birdRadScaled * 1.55f)
                                lineTo(axScaled + birdRadScaled * 0.15f, ayScaled - birdRadScaled * 1.05f)
                                lineTo(axScaled + birdRadScaled * 0.5f, ayScaled - birdRadScaled * 1.35f)
                                lineTo(axScaled + birdRadScaled * 0.4f, ayScaled - birdRadScaled * 0.82f)
                                close()
                            }
                            drawPath(
                                path = crownPath,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }

            // UI Overlays Screen HUD
            // 1. Live Score Counter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 55.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayLarge
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Peak score",
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "最高记录: ${viewModel.getPersonalHighScore()}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Control overlay icons (Mute & Pause)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 48.dp)
                    .align(Alignment.TopCenter)
            ) {
                IconButton(
                    onClick = {
                        isMutedState.value = SoundFX.toggleMute()
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .testTag("mute_button")
                ) {
                    Text(
                        text = if (isMutedState.value) "🔇" else "🔊",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (playState == PlayState.PLAYING) {
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Home, // Draw simple pause/play indicator
                            tint = Color.White,
                            contentDescription = if (isPaused) "Resume" else "Pause"
                        )
                        Text(
                            text = if (isPaused) "▶" else "‖",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 3. READY instructions overlay
            if (playState == PlayState.READY) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom finger tap vector indicator
                    Text(
                        text = "👆",
                        fontSize = 62.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "轻触屏幕，起飞！",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "穿着皮服: ${activeSkin.name}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 4. PAUSE Screen overlay
            if (isPaused && playState == PlayState.PLAYING) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.wrapContentSize(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "游戏暂停",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { isPaused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text("继续游戏", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                playState = PlayState.READY
                                isPaused = false
                                restartGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text("重新开始", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isPaused = false
                                playState = PlayState.READY
                                onBackToMenu()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text("返回菜单", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                }
            }

            // 5. GAME OVER Bottom Popup Overlay
            AnimatedVisibility(
                visible = (playState == PlayState.GAMEOVER),
                enter = fadeIn(animationSpec = tween(350)) + scaleIn(animationSpec = tween(350)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gameover_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202C39)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💥 坠毁了！",
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            color = Color(0xFFFF5252)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "本次积分",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$score",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFEB3B)
                        )

                        val currentHighScore = viewModel.getPersonalHighScore()
                        if (score >= currentHighScore && score > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🌟 新高记录创造者！ 🌟",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Display player profile detail summary
                        Text(
                            text = "玩家：${viewModel.playerName.value}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("restart_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("再飞一次", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onViewLeaderboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = "Leaderboard", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("积分榜", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onBackToMenu() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78909C)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("主菜单", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to generate randomized top and bottom values relative to 360x640 coordinate grid
private fun generatePipe(startX: Float): Pipe {
    val clearanceGap = PIPE_GAP
    val minPipeHeight = 50f
    val maxPipeHeight = DESIGN_HEIGHT - GROUND_HEIGHT - clearanceGap - minPipeHeight
    
    // Generate randomized top height
    val topH = minPipeHeight + Random.nextFloat() * (maxPipeHeight - minPipeHeight)
    // Bottom height makes sure total height is filled minus gap clearance
    val bottomH = DESIGN_HEIGHT - GROUND_HEIGHT - topH - clearanceGap

    return Pipe(
        x = startX,
        topHeight = topH,
        bottomHeight = bottomH
    )
}

// Function triggered when game finishes crash over, submitting details
private fun gameOverOccurred(
    viewModel: GameViewModel,
    endScore: Int,
    pipes: List<Pipe>,
    particles: MutableList<Particle>,
    crashY: Float,
    triggerStateUpdate: () -> Unit
) {
    SoundFX.playHit()
    triggerStateUpdate()

    // Create explosion fireworks!
    repeat(25) {
        val speedAngle = Random.nextDouble() * 2.0 * Math.PI
        val speedMag = 3f + Random.nextFloat() * 6f
        particles.add(
            Particle(
                x = 100f,
                y = crashY,
                vx = (Math.cos(speedAngle) * speedMag).toFloat(),
                vy = (Math.sin(speedAngle) * speedMag).toFloat(),
                size = 4f + Random.nextFloat() * 7f,
                color = when (Random.nextInt(3)) {
                    0 -> Color(0xFFFF5252) // Orange red
                    1 -> Color(0xFFFFCA28) // Bright gold
                    else -> Color(0xFFFF8A65) // Light red coral
                },
                lifeTime = 0.8f
            )
        )
    }

    // Submit score immediately to local Room database ViewModel if >0
    if (endScore >= 0) {
        viewModel.submitScore(endScore)
    }
}
