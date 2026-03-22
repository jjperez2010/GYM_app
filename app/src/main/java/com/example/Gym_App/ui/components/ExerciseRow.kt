package com.example.Gym_App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Gym_App.model.Exercise

@Composable
fun ExerciseRow(
    exercise: Exercise,
    isEdit: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (isEdit) Color(0xFF00FF00).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            // Parámetros justo debajo del nombre
            Text("${exercise.sets} series x ${exercise.reps} reps — ${exercise.weight}kg", color = Color(0xFF00FF00), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(exercise.muscleGroup, color = Color.Gray, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Descanso", color = Color.Gray, fontSize = 10.sp)
            Text("${exercise.rest}s", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}