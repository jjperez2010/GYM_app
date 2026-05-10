package com.example.Gym_App.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.Gym_App.model.Routine
import com.example.Gym_App.model.Exercise

@Composable
fun TodayWorkoutCard(
    routine: Routine?,
    isDone: Boolean,
    allExercises: List<Exercise>,
    onStart: (Routine?) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C20))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (routine != null) {
                // Imagen de fondo
                val resId = context.resources.getIdentifier(routine.imageId, "drawable", context.packageName)
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.9f))
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isDone) "ENTRENAMIENTO COMPLETADO" else "TE TOCA HOY",
                        color = if (isDone) Color(0xFF00FF00) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = routine?.name ?: "Día de Descanso",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (routine != null) {
                        val routineExercises = routine.exerciseNames.mapNotNull { name -> allExercises.find { it.name == name } }
                        val muscles = routineExercises.map { it.muscleGroup }.distinct().take(2).joinToString(", ")
                        Text(
                            text = muscles,
                            color = Color(0xFFC6FF00),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isDone && routine != null) {
                    Button(
                        onClick = { onStart(routine) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC6FF00)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "INICIAR ENTRENAMIENTO",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                } else if (isDone) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00FF00), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("¡Buen trabajo!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWorkoutCard(
    routineName: String,
    completedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    val progress = completedCount.toFloat() / totalCount.toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFBB86FC).copy(0.1f)),
        border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.3f))
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFBB86FC),
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(0.1f)
                )
                Text("${(progress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text("Continuar entrenamiento", color = Color(0xFFBB86FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(routineName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("Quedan ${totalCount - completedCount} ejercicios", color = Color.Gray, fontSize = 12.sp)
            }
            
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String, onNav: (() -> Unit) -> Unit = { it() }) {
    val context = LocalContext.current
    Surface(color = Color(0xFF12151C), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.navigationBarsPadding().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Default.Home, "Inicio", currentRoute == "menu") { onNav { if (currentRoute != "menu") navController.navigate("menu") } }
            NavItem(Icons.Default.MonitorWeight, "Peso", currentRoute == "weight") { onNav { if (currentRoute != "weight") navController.navigate("weight") } }
            NavItem(Icons.Default.BarChart, "Progreso", currentRoute == "progress") { onNav { if (currentRoute != "progress") navController.navigate("progress") } }
            NavItem(Icons.Default.Settings, "Perfil", currentRoute == "settings") { onNav { if (currentRoute != "settings") navController.navigate("settings") } }
            NavItem(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar", false) { (context as? Activity)?.finish() }
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF00FF00) else Color.Gray
    Column(Modifier.clickable { onClick() }.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        Text(label, color = color, fontSize = 11.sp)
    }
}

@Composable
fun MenuButton(text: String, modifier: Modifier = Modifier, bColor: Color = Color(0xFF00FF00), cColor: Color = Color.DarkGray, onClick: () -> Unit) {
    Button(
        onClick = onClick, 
        modifier = modifier.padding(2.dp), 
        colors = ButtonDefaults.buttonColors(containerColor = cColor), 
        border = BorderStroke(1.dp, bColor), 
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NumericStepper(label: String, value: Int, step: Int, bigStep: Int? = null, onValueChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bigStep != null) { 
                IconButton(onClick = { if (value >= bigStep) onValueChange(value - bigStep) }, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Default.RemoveCircle, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(18.dp)) 
                } 
            }
            IconButton(onClick = { if (value >= step) onValueChange(value - step) }, modifier = Modifier.size(32.dp)) { 
                Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(18.dp)) 
            }
            Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = { onValueChange(value + step) }, modifier = Modifier.size(32.dp)) { 
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) 
            }
            if (bigStep != null) { 
                IconButton(onClick = { onValueChange(value + bigStep) }, modifier = Modifier.size(32.dp)) { 
                    Icon(Icons.Default.AddCircle, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(18.dp)) 
                } 
            }
        }
    }
}

@Composable
fun InfoCol(l: String, v: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(l, color = Color.Gray, fontSize = 10.sp)
        Text(v, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatWeight(weight: Int): String = when {
    weight == 0 -> "Corp."
    weight < 0 -> "Asist."
    else -> "${weight}kg"
}
