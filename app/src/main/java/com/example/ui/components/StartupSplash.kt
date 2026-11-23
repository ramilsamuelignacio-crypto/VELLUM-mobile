package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.VellumColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StartupSplash(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strokeProgress = remember { Animatable(0f) }
    val accentProgress = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val ruleScale = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            strokeProgress.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            accentProgress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(260)
            wordmarkAlpha.animateTo(1f, tween(420))
        }
        launch {
            delay(330)
            ruleScale.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        launch {
            delay(400)
            subtitleAlpha.animateTo(1f, tween(420))
        }
        delay(1200)
        exitAlpha.animateTo(0f, tween(340))
        onComplete()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(VellumColors.StudioDesk)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onComplete
            )
            .testTag("startup_splash")
    ) {
        // Subtle 24dp dot grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            var y = step / 2f
            while (y < size.height) {
                var x = step / 2f
                while (x < size.width) {
                    drawCircle(
                        color = VellumColors.GridDot,
                        radius = 1.dp.toPx(),
                        center = Offset(x, y)
                    )
                    x += step
                }
                y += step
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 144.dp, height = 92.dp)) {
                val sx = size.width / 144f
                val sy = size.height / 92f

                // Cobalt drafting arc
                val arcPath = Path().apply {
                    moveTo(22f * sx, 68f * sy)
                    cubicTo(46f * sx, 22f * sy, 98f * sx, 22f * sy, 122f * sx, 68f * sy)
                }
                val arcMeasure = PathMeasure().apply { setPath(arcPath, false) }
                val arcSegment = Path()
                arcMeasure.getSegment(0f, arcMeasure.length * accentProgress.value, arcSegment, true)
                drawPath(
                    path = arcSegment,
                    color = VellumColors.CobaltBlue,
                    style = Stroke(width = 2.5f * sx, cap = StrokeCap.Round)
                )

                // Expressive V ink flourish
                val vPath = Path().apply {
                    moveTo(30f * sx, 22f * sy)
                    cubicTo(42f * sx, 22f * sy, 56f * sx, 54f * sy, 69f * sx, 74f * sy)
                    cubicTo(81f * sx, 52f * sy, 96f * sx, 26f * sy, 114f * sx, 20f * sy)
                }
                val vMeasure = PathMeasure().apply { setPath(vPath, false) }
                val vSegment = Path()
                vMeasure.getSegment(0f, vMeasure.length * strokeProgress.value, vSegment, true)
                drawPath(
                    path = vSegment,
                    color = VellumColors.InkCharcoal,
                    style = Stroke(width = 5.5f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                if (wordmarkAlpha.value > 0.05f) {
                    drawCircle(
                        color = VellumColors.CobaltBlue.copy(alpha = wordmarkAlpha.value),
                        radius = 2.8f * sx,
                        center = Offset(30f * sx, 22f * sy)
                    )
                }
                if (subtitleAlpha.value > 0.05f) {
                    drawCircle(
                        color = VellumColors.InkCharcoal.copy(alpha = subtitleAlpha.value),
                        radius = 2.8f * sx,
                        center = Offset(114f * sx, 20f * sy)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = VellumColors.InkCharcoal,
                modifier = Modifier.alpha(wordmarkAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width((112f * ruleScale.value).dp)
                    .height(1.dp)
                    .background(VellumColors.GridDot)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_subtitle),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = VellumColors.SlateSecondary,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onComplete,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .testTag("splash_enter_button")
            ) {
                Text(
                    text = stringResource(R.string.enter_studio),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = VellumColors.SlateSecondary
                )
            }
        }
    }
}
