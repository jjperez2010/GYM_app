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
            NavItem(Icons.Default.FitnessCenter, "Ejercicios", currentRoute == "ejercicios") { onNav { if (currentRoute != "ejercicios") navController.navigate("ejercicios") } }
            NavItem(Icons.AutoMirrored.Filled.ListAlt, "Rutinas", currentRoute == "rutinas") { onNav { if (currentRoute != "rutinas") navController.navigate("rutinas") } }
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
    Button(onClick = onClick, modifier = modifier.padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = cColor), border = BorderStroke(1.dp, bColor), shape = RoundedCornerShape(8.dp)) {
        Text(text, color = Color.White, maxLines = 1)
    }
}

@Composable
fun NumericStepper(label: String, value: Int, step: Int, bigStep: Int? = null, onValueChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bigStep != null) { IconButton(onClick = { if (value >= bigStep) onValueChange(value - bigStep) }) { Icon(Icons.Default.RemoveCircle, null, tint = Color.White.copy(0.5f)) } }
            IconButton(onClick = { if (value >= step) onValueChange(value - step) }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
            Text("$value", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onValueChange(value + step) }) { Icon(Icons.Default.Add, null, tint = Color.White) }
            if (bigStep != null) { IconButton(onClick = { onValueChange(value + bigStep) }) { Icon(Icons.Default.AddCircle, null, tint = Color.White.copy(0.5f)) } }
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
