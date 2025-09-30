// StopwatchScreen.kt
package com.galaxy.timer.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/**
 * 秒表主界面组件
 * 显示时间、开始/暂停和重置功能
 */
@Composable
fun StopwatchScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var lastTime by remember { mutableStateOf(0L) }
    var lastLapTime by remember { mutableStateOf(0L) } // 记录上一次分段计时的时间点
    var lapTimes by remember { mutableStateOf(listOf(0L)) } // 存储每次分段时间间隔
    var isDetailPage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableStateOf(0f) }

    // 计时器逻辑
    val currentTime = System.currentTimeMillis()
    if (isRunning) {
        if (lastTime != 0L) {
            elapsedTime += currentTime - lastTime
        }
        lastTime = currentTime
    } else {
        lastTime = 0L
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val swipeThreshold = 100f // 灵敏度阈值，按需调整
                        if (!isDetailPage && offsetX < -swipeThreshold) {
                            // 主页面：左滑进入详情
                            scope.launch { isDetailPage = true }
                        } else if (isDetailPage && offsetX > swipeThreshold) {
                            // 详情页：右滑返回主页面
                            scope.launch { isDetailPage = false }
                        }
                        offsetX = 0f
                    },

                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isDetailPage) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 时间显示
                TimeDisplay(elapsedTime = elapsedTime)

                Spacer(modifier = Modifier.height(4.dp))
                LapTimesDisplay(lapTimes = lapTimes)

                Spacer(modifier = Modifier.height(4.dp))

                // 控制按钮区域
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(2.dp))

                    ControlButton(icon = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow) {
                        isRunning = !isRunning
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    ControlButton(icon = Icons.Filled.AddCircle) {
                        if (isRunning) {
                            val currentLapTime = elapsedTime - lastLapTime
                            lapTimes = lapTimes + currentLapTime
                            lastLapTime = elapsedTime
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    ControlButton(icon = Icons.Filled.Refresh) {
                        isRunning = false
                        elapsedTime = 0L
                        lastTime = 0L
                        lastLapTime = 0L
                        lapTimes = listOf(0L)
                    }

                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        } else {
            DetailPage(lapTimes = lapTimes, totalTime = elapsedTime)
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun LapTimesDisplay(lapTimes: List<Long>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (lapTimes.isNotEmpty()) {
            val latestLap = lapTimes.last()
            Text(
                text = String.format(
                    "Lap: %02d:%02d:%02d.%02d",
                    (latestLap / 3600000).toInt(),
                    ((latestLap % 3600000) / 60000).toInt(),
                    ((latestLap % 60000) / 1000).toInt(),
                    ((latestLap % 1000) / 10).toInt()
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DetailPage(lapTimes: List<Long>, totalTime: Long) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "分段时间记录",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // 中间：可滚动的分段列表，占用剩余空间
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f) // 占满中间空间
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lapTimes.forEachIndexed { index, time ->
                Text(
                    text = String.format(
                        "Lap %d: %02d:%02d:%02d.%02d",
                        index + 1,
                        (time / 3600000).toInt(),
                        ((time % 3600000) / 60000).toInt(),
                        ((time % 60000) / 1000).toInt(),
                        ((time % 1000) / 10).toInt()
                    ),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 底部：总时间（始终固定显示）
        Text(
            text = "${String.format(
                "%02d:%02d:%02d.%02d",
                (totalTime / 3600000).toInt(),
                ((totalTime % 3600000) / 60000).toInt(),
                ((totalTime % 60000) / 1000).toInt(),
                ((totalTime % 1000) / 10).toInt()
            )}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun TimeDisplay(elapsedTime: Long) {
    val hours = (elapsedTime / 3600000).toInt()
    val minutes = ((elapsedTime % 3600000) / 60000).toInt()
    val seconds = ((elapsedTime % 60000) / 1000).toInt()
    val milliseconds = ((elapsedTime % 1000) / 10).toInt()

    Text(
        text = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary
    )
}

@Composable
fun ControlButton(icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun StopwatchScreenPreview() {
    StopwatchScreen()
}
