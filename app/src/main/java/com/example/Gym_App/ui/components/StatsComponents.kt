package com.example.Gym_App.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Gym_App.model.WorkoutHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatCard(label: String, value: String, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun VolumeChart(history: List<WorkoutHistoryEntity>) {
    val weeklyVolume = remember(history) {
        val last7Days = (0..6).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(cal.time)
            val vol = history.filter { h ->
                val hCal = Calendar.getInstance().apply { timeInMillis = h.date }
                hCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
            }.sumOf { it.volume }
            dateStr to vol
        }.reversed()
        last7Days
    }

    if (weeklyVolume.all { it.second.toInt() == 0 }) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("Sin datos semanales", color = Color.Gray)
        }
    } else {
        val maxVol = weeklyVolume.maxOf { it.second }.coerceAtLeast(1.0).toFloat()
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 16.dp)) {
            val barWidth = size.width / 14f
            weeklyVolume.forEachIndexed { i, item ->
                val vol = item.second.toFloat()
                val barHeight = (vol / maxVol) * size.height
                drawRect(
                    color = Color(0xFF00FF00),
                    topLeft = Offset(i * (size.width / 7f) + barWidth / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }
    }
}

@Composable
fun WeightLineChart(exerciseHistory: List<WorkoutHistoryEntity>) {
    if (exerciseHistory.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("Sin historial para este ejercicio", color = Color.Gray)
        }
    } else {
        val weights = exerciseHistory.map { it.weight.toFloat() }
        val maxW = weights.maxOrNull() ?: 1f
        val minW = weights.minOrNull() ?: 0f
        val range = (maxW - minW).coerceAtLeast(1f)

        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 8.dp)) {
            val path = Path()
            val stepX = size.width / (weights.size - 1).coerceAtLeast(1)
            weights.forEachIndexed { i, w ->
                val x = i * stepX
                val y = size.height - ((w - minW) / range) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(Color(0xFF00AAFF), radius = 4.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path, Color(0xFF00AAFF), style = Stroke(width = 2.dp.toPx()))
        }
    }
}
