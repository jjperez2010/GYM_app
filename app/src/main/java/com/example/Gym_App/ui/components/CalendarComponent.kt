package com.example.Gym_App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.Gym_App.model.WorkoutHistoryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun ConsistencyCalendar(workoutDates: List<LocalDate>, workoutHistory: List<WorkoutHistoryEntity> = emptyList()) {
    var viewMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = viewMonth.lengthOfMonth()
    val firstDayOfMonth = viewMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
    
    val totalSlots = daysInMonth + firstDayOfMonth
    
    var selectedDateForHistory by remember { mutableStateOf<LocalDate?>(null) }
    
    // Calculando racha actual
    val streak = remember(workoutDates) {
        var count = 0
        var checkDate = LocalDate.now()
        while (workoutDates.contains(checkDate)) {
            count++
            checkDate = checkDate.minusDays(1)
        }
        if (count == 0) {
            val yesterday = LocalDate.now().minusDays(1)
            checkDate = yesterday
            while (workoutDates.contains(checkDate)) {
                count++
                checkDate = checkDate.minusDays(1)
            }
        }
        count
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewMonth = viewMonth.minusMonths(1) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                        }
                        Text(
                            text = viewMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es")).uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { viewMonth = viewMonth.plusMonths(1) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
                        }
                    }
                    Text(
                        text = "Consistencia mensual ${viewMonth.year}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                // Streak Counter
                Surface(
                    color = Color(0xFFFFA500).copy(0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA500).copy(0.5f))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFA500), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$streak días", color = Color(0xFFFFA500), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Días de la semana
            Row(Modifier.fillMaxWidth()) {
                listOf("D", "L", "M", "M", "J", "V", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cuadrícula del calendario
            Column {
                var currentSlot = 0
                for (row in 0..5) {
                    if (currentSlot >= totalSlots) break
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val dayNumber = currentSlot - firstDayOfMonth + 1
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (dayNumber in 1..daysInMonth) {
                                    val date = viewMonth.atDay(dayNumber)
                                    val hasWorkout = workoutDates.contains(date)
                                    val isToday = date == LocalDate.now()

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasWorkout) Color(0xFF00FF00).copy(0.2f) 
                                                else if (isToday) Color.White.copy(0.1f) 
                                                else Color.Transparent
                                            )
                                            .then(if (isToday) Modifier.border(1.dp, Color.White.copy(0.5f), CircleShape) else Modifier)
                                            .clickable {
                                                if (hasWorkout) {
                                                    selectedDateForHistory = date
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = if (hasWorkout) Color(0xFF00FF00) else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (hasWorkout || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            currentSlot++
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE HISTORIAL POR DÍA
    if (selectedDateForHistory != null) {
        val dateToCompare = selectedDateForHistory!!
        val dayHistory = workoutHistory.filter { 
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == dateToCompare
        }

        Dialog(onDismissRequest = { selectedDateForHistory = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00).copy(0.5f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateToCompare.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"))).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { selectedDateForHistory = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (dayHistory.isEmpty()) {
                        Text("No hay registros detallados para este día", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                            items(dayHistory) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val isRoutineHeader = entry.exerciseName.startsWith("Rutina:")
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = if (isRoutineHeader) entry.exerciseName.substringAfter("Rutina: ").uppercase() else entry.exerciseName,
                                                color = if (isRoutineHeader) Color(0xFFBB86FC) else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (!isRoutineHeader && entry.sets > 0) {
                                                Text(
                                                    text = "${entry.sets} series x ${entry.reps} reps — ${entry.weight}kg",
                                                    color = Color(0xFF00FF00),
                                                    fontSize = 12.sp
                                                )
                                            } else if (isRoutineHeader) {
                                                Text("Sesión completada", color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                        if (!isRoutineHeader) {
                                            val timeStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(
                                                Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault()).toLocalTime()
                                            )
                                            Text(timeStr, color = Color.Gray.copy(0.6f), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { selectedDateForHistory = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black)
                    ) {
                        Text("CERRAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
