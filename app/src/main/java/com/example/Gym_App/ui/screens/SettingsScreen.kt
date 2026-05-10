package com.example.Gym_App.ui.screens

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.Gym_App.ui.components.BottomNavBar
import com.example.Gym_App.ui.components.MenuButton
import com.example.Gym_App.ui.components.NumericStepper
import com.example.Gym_App.viewmodel.GymViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (expanded) Color(0xFF00FF00).copy(0.5f) else Color.Gray.copy(0.2f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (expanded) Color(0xFF00FF00) else Color.Gray, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    
    var name by remember { mutableStateOf(prefs.getString("userName", "") ?: "") }
    var pronoun by remember { mutableStateOf(prefs.getString("userPronoun", "Él") ?: "Él") }
    var gender by remember { mutableStateOf(prefs.getString("userGender", "Masculino") ?: "Masculino") }
    
    val birthDateMillisState = remember { mutableLongStateOf(prefs.getLong("userBirthDate", 0L)) }
    var birthDateMillis by birthDateMillisState
    
    var level by remember { mutableStateOf(prefs.getString("userLevel", "Novato") ?: "Novato") }
    var weight by remember { mutableIntStateOf(prefs.getInt("userWeight", 70)) }
    var height by remember { mutableIntStateOf(prefs.getInt("userHeight", 170)) }
    var globalWait by remember { mutableIntStateOf(prefs.getInt("globalWait", 20)) }

    var selectedTopColor by remember { mutableIntStateOf(prefs.getInt("trainingGradientTopColor", Color.Black.toArgb())) }
    var selectedBottomColor by remember { mutableIntStateOf(prefs.getInt("trainingGradientBottomColor", Color(0xFF424242).toArgb())) }
    var fontColor1 by remember { mutableIntStateOf(prefs.getInt("mainFontColor1", Color.White.toArgb())) }
    var fontColor2 by remember { mutableIntStateOf(prefs.getInt("mainFontColor2", Color(0xFFC6FF00).toArgb())) }

    val showResetConfirm = remember { mutableStateOf(false) }
    val showRestoreConfirm = remember { mutableStateOf(false) }
    val showDatePicker = remember { mutableStateOf(false) }
    val showColorPicker = remember { mutableStateOf<String?>(null) } 

    var personalExpanded by remember { mutableStateOf(true) }
    var visualExpanded by remember { mutableStateOf(false) }
    var notificationsExpanded by remember { mutableStateOf(false) }
    var supportExpanded by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", false)) }
    var dailyReminderHour by remember { mutableIntStateOf(prefs.getInt("daily_reminder_hour", 8)) }
    var dailyReminderMinute by remember { mutableIntStateOf(prefs.getInt("daily_reminder_minute", 0)) }
    var maxDaysWithoutWorkout by remember { mutableIntStateOf(prefs.getInt("max_days_without_workout", 3)) }
    var prNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("pr_notifications_enabled", true)) }
    var consistencyNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("consistency_notifications_enabled", true)) }
    var weeklyGoalNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("weekly_goal_notifications_enabled", true)) }
    var weeklyWorkoutGoal by remember { mutableIntStateOf(prefs.getInt("weekly_workout_goal", 3)) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (birthDateMillis != 0L) birthDateMillis else null,
        yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR)
    )

    val age = remember(birthDateMillis) {
        if (birthDateMillis == 0L) 0
        else {
            val dob = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
            val today = Calendar.getInstance()
            var ageVal = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) ageVal--
            ageVal
        }
    }

    val displayDate = remember(birthDateMillis) {
        if (birthDateMillis == 0L) "Seleccionar fecha" 
        else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDateMillis))
    }

    val palette64 = remember {
        val colors = mutableListOf<Color>()
        val steps = 4 
        for (r in 0 until steps) for (g in 0 until steps) for (b in 0 until steps) {
            colors.add(Color(red = (r * 85) / 255f, green = (g * 85) / 255f, blue = (b * 85) / 255f))
        }
        colors
    }

    Scaffold(bottomBar = { BottomNavBar(navController, "settings") }) { innerPadding ->
        val currentGradient = Brush.verticalGradient(listOf(Color(selectedTopColor), Color(selectedBottomColor)))
        Box(Modifier.fillMaxSize().background(brush = currentGradient).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Configuración", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(24.dp))

                SettingsSection("Datos Personales", Icons.Default.Person, personalExpanded, { personalExpanded = !personalExpanded }) {
                    OutlinedTextField(value = name, onValueChange = { name = it; prefs.edit { putString("userName", it) } }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF00FF00), unfocusedLabelColor = Color.Gray))
                    Spacer(Modifier.height(16.dp))
                    Text("¿Cómo prefieres que te llamemos?", color = Color.Gray, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Él", "Ella", "Elle").forEach { p -> FilterChip(selected = pronoun == p, onClick = { pronoun = p; prefs.edit { putString("userPronoun", p) } }, label = { Text(p) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00))) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Sexo", color = Color.Gray, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Masculino", "Femenino", "Otro").forEach { g -> FilterChip(selected = gender == g, onClick = { gender = g; prefs.edit { putString("userGender", g) } }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00).copy(0.7f))) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Fecha de Nacimiento", color = Color.Gray, fontSize = 14.sp)
                    Card(Modifier.fillMaxWidth().clickable { showDatePicker.value = true }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF00FF00)); Spacer(Modifier.width(12.dp))
                            Column { Text(displayDate, color = Color.White, fontSize = 16.sp); if (age > 0) Text("Edad calculada: $age años", color = Color.Gray, fontSize = 12.sp) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Nivel y Medidas", color = Color.Gray, fontSize = 14.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Novato", "Intermedio", "Avanzado").forEach { l -> FilterChip(selected = level == l, onClick = { level = l; prefs.edit { putString("userLevel", l) } }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFFBB86FC))) }
                    }
                    Spacer(Modifier.height(16.dp))
                    NumericStepper("Peso (kg)", weight, 1, 5) { weight = it; prefs.edit { putInt("userWeight", it) } }
                    NumericStepper("Estatura (cm)", height, 1, 5) { height = it; prefs.edit { putInt("userHeight", it) } }
                    Spacer(Modifier.height(16.dp))
                    Text("Configuración de Rutina", color = Color.Gray, fontSize = 14.sp)
                    NumericStepper("Tiempo de preparación previa (s)", globalWait, 5, 10) { globalWait = it; prefs.edit { putInt("globalWait", it) } }
                }

                SettingsSection("Personalización Visual", Icons.Default.Palette, visualExpanded, { visualExpanded = !visualExpanded }) {
                    Card(Modifier.fillMaxWidth().clickable { showColorPicker.value = "top" }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(selectedTopColor)).border(1.dp, Color.White, CircleShape)); Spacer(Modifier.width(16.dp))
                            Column { Text("Color Superior", color = Color.White, fontSize = 16.sp); Text("Inicio del gradiente", color = Color.Gray, fontSize = 12.sp) }
                            Spacer(Modifier.weight(1f)); Icon(Icons.Default.Palette, null, tint = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth().clickable { showColorPicker.value = "bottom" }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(selectedBottomColor)).border(1.dp, Color.White, CircleShape)); Spacer(Modifier.width(16.dp))
                            Column { Text("Color Inferior", color = Color.White, fontSize = 16.sp); Text("Final del gradiente", color = Color.Gray, fontSize = 12.sp) }
                            Spacer(Modifier.weight(1f)); Icon(Icons.Default.Palette, null, tint = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth().clickable { showColorPicker.value = "font1" }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(fontColor1)).border(1.dp, Color.White, CircleShape)); Spacer(Modifier.width(16.dp))
                            Column { Text("Texto Principal", color = Color.White, fontSize = 16.sp); Text("Color para nombres y títulos", color = Color.Gray, fontSize = 12.sp) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth().clickable { showColorPicker.value = "font2" }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(fontColor2)).border(1.dp, Color.White, CircleShape)); Spacer(Modifier.width(16.dp))
                            Column { Text("Color Acento", color = Color.White, fontSize = 16.sp); Text("Valores y resaltados", color = Color.Gray, fontSize = 12.sp) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Presets de Estilo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    data class Preset(val name: String, val t: Int, val b: Int, val f1: Int, val f2: Int)
                    val presets = listOf(Preset("Classic", Color.Black.toArgb(), Color(0xFF424242).toArgb(), Color.White.toArgb(), Color(0xFFC6FF00).toArgb()), Preset("Cyber", Color.Black.toArgb(), Color(0xFF1A1A2E).toArgb(), Color.White.toArgb(), Color(0xFF00F5FF).toArgb()), Preset("Volcano", Color(0xFF2D0B0B).toArgb(), Color.Black.toArgb(), Color.White.toArgb(), Color(0xFFFF4D00).toArgb()), Preset("Forest", Color(0xFF002B15).toArgb(), Color.Black.toArgb(), Color(0xFFF5F5F5).toArgb(), Color(0xFF00FF88).toArgb()), Preset("Royal", Color.Black.toArgb(), Color(0xFF1A1A1A).toArgb(), Color(0xFFFFD700).toArgb(), Color(0xFFF5F5F5).toArgb()))
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(presets) { p ->
                            Column(Modifier.width(80.dp).clickable { selectedTopColor = p.t; selectedBottomColor = p.b; fontColor1 = p.f1; fontColor2 = p.f2; prefs.edit { putInt("trainingGradientTopColor", p.t); putInt("trainingGradientBottomColor", p.b); putInt("mainFontColor1", p.f1); putInt("mainFontColor2", p.f2) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(Color(p.t), Color(p.b)))).border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(10.dp).background(Color(p.f1), CircleShape)); Spacer(Modifier.height(4.dp)); Box(Modifier.size(10.dp).background(Color(p.f2), CircleShape)) } }
                                Spacer(Modifier.height(4.dp)); Text(p.name, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                SettingsSection("Notificaciones", Icons.Default.Notifications, notificationsExpanded, { notificationsExpanded = !notificationsExpanded }) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Habilitar notificaciones", color = Color.White, fontSize = 16.sp); Spacer(Modifier.weight(1f)); Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it; prefs.edit { putBoolean("notifications_enabled", it) }; viewModel.updateNotificationSettings(it, dailyReminderHour, dailyReminderMinute, maxDaysWithoutWorkout, prNotificationsEnabled, consistencyNotificationsEnabled, weeklyGoalNotificationsEnabled, weeklyWorkoutGoal) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF00), checkedTrackColor = Color(0xFF00FF00).copy(0.5f))) }
                    if (notificationsEnabled) {
                        Spacer(Modifier.height(16.dp))
                        Card(Modifier.fillMaxWidth().clickable { showTimePicker = true }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Schedule, null, tint = Color(0xFF00FF00)); Spacer(Modifier.width(12.dp)); Column { Text("Recordatorio diario", color = Color.White, fontSize = 16.sp); Text("${String.format(Locale.getDefault(), "%02d", dailyReminderHour)}:${String.format(Locale.getDefault(), "%02d", dailyReminderMinute)}", color = Color.Gray, fontSize = 14.sp) } } }
                        Spacer(Modifier.height(16.dp)); NumericStepper("Días sin entrenar", maxDaysWithoutWorkout, 1, 1) { maxDaysWithoutWorkout = it; prefs.edit { putInt("max_days_without_workout", it) }; viewModel.updateNotificationSettings(notificationsEnabled, dailyHour = dailyReminderHour, dailyMinute = dailyReminderMinute, maxDaysWithoutWorkout = it, prEnabled = prNotificationsEnabled, consistencyEnabled = consistencyNotificationsEnabled, weeklyGoalEnabled = weeklyGoalNotificationsEnabled, weeklyGoal = weeklyWorkoutGoal) }
                        NumericStepper("Objetivo semanal", weeklyWorkoutGoal, 1, 1) { weeklyWorkoutGoal = it; prefs.edit { putInt("weekly_workout_goal", it) }; viewModel.updateNotificationSettings(notificationsEnabled, dailyHour = dailyReminderHour, dailyMinute = dailyReminderMinute, maxDaysWithoutWorkout = maxDaysWithoutWorkout, prEnabled = prNotificationsEnabled, consistencyEnabled = consistencyNotificationsEnabled, weeklyGoalEnabled = weeklyGoalNotificationsEnabled, weeklyGoal = it) }
                        Spacer(Modifier.height(16.dp)); Text("Tipos:", color = Color.Gray, fontSize = 14.sp)
                        listOf(Triple("Nuevos PRs", "Avisar récords personales", prNotificationsEnabled), Triple("Consistencia", "Aviso por inactividad", consistencyNotificationsEnabled), Triple("Objetivo semanal", "Meta de entrenamientos", weeklyGoalNotificationsEnabled)).forEach { (label, desc, state) -> Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = Color.White, fontSize = 14.sp); Text(desc, color = Color.Gray, fontSize = 11.sp) }; Switch(checked = state, onCheckedChange = { when(label) { "Nuevos PRs" -> { prNotificationsEnabled = it; prefs.edit { putBoolean("pr_notifications_enabled", it) } }; "Consistencia" -> { consistencyNotificationsEnabled = it; prefs.edit { putBoolean("consistency_notifications_enabled", it) } }; "Objetivo semanal" -> { weeklyGoalNotificationsEnabled = it; prefs.edit { putBoolean("weekly_goal_notifications_enabled", it) } } }; viewModel.updateNotificationSettings(notificationsEnabled, dailyReminderHour, dailyReminderMinute, maxDaysWithoutWorkout, prNotificationsEnabled, consistencyNotificationsEnabled, weeklyGoalEnabled = weeklyGoalNotificationsEnabled, weeklyGoal = weeklyWorkoutGoal) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFBB86FC))) } }
                    }
                }

                SettingsSection("Soporte Técnico", Icons.Default.BugReport, supportExpanded, { supportExpanded = !supportExpanded }) {
                    val lastError = remember { prefs.getString("last_error", null) }
                    val clipboardManager = LocalClipboard.current; val scope = rememberCoroutineScope()
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)), border = BorderStroke(1.dp, Color.Gray.copy(0.3f))) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Último Error", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); if (lastError != null) IconButton(onClick = { scope.launch { clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("Error Log", lastError))) }; Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF00FF00), modifier = Modifier.size(20.dp)) } }; Box(Modifier.fillMaxWidth().heightIn(max = 100.dp).verticalScroll(rememberScrollState())) { Text(text = lastError ?: "Sin errores recientes.", color = if (lastError != null) Color(0xFFFFBABA) else Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }; if (lastError != null) TextButton(onClick = { prefs.edit { remove("last_error") }; navController.navigate("settings") }, modifier = Modifier.align(Alignment.End)) { Text("LIMPIAR LOG", color = Color.Gray, fontSize = 12.sp) } } }
                    Spacer(Modifier.height(16.dp)); MenuButton("RESTAURAR EJERCICIOS", Modifier.fillMaxWidth(), bColor = Color.Yellow) { showRestoreConfirm.value = true }
                    Spacer(Modifier.height(8.dp)); MenuButton("RESET TOTAL APP", Modifier.fillMaxWidth(), bColor = Color.Red) { showResetConfirm.value = true }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showColorPicker.value != null) {
        val type = showColorPicker.value!!
        AlertDialog(onDismissRequest = { showColorPicker.value = null }, confirmButton = { TextButton(onClick = { showColorPicker.value = null }) { Text("LISTO", color = Color(0xFF00FF00)) } }, containerColor = Color(0xFF1A1C20), title = { Text(when(type) { "top" -> "Color Superior"; "bottom" -> "Color Inferior"; "font1" -> "Color Texto Principal"; else -> "Color Acento" }, color = Color.White) }, text = { Column { Text("Selecciona un color:", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp)); Box(modifier = Modifier.height(350.dp)) { LazyColumn { item { FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { palette64.forEach { color -> val currentColorInt = when(type) { "top" -> selectedTopColor; "bottom" -> selectedBottomColor; "font1" -> fontColor1; else -> fontColor2 }; Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(width = if (currentColorInt == color.toArgb()) 2.dp else 0.dp, color = if (currentColorInt == color.toArgb()) Color.White else Color.Transparent, shape = CircleShape).clickable { when(type) { "top" -> { selectedTopColor = color.toArgb(); prefs.edit { putInt("trainingGradientTopColor", selectedTopColor) } }; "bottom" -> { selectedBottomColor = color.toArgb(); prefs.edit { putInt("trainingGradientBottomColor", selectedBottomColor) } }; "font1" -> { fontColor1 = color.toArgb(); prefs.edit { putInt("mainFontColor1", fontColor1) } }; "font2" -> { fontColor2 = color.toArgb(); prefs.edit { putInt("mainFontColor2", fontColor2) } } } }) } } } } } } })
    }

    if (showRestoreConfirm.value) {
        AlertDialog(onDismissRequest = { showRestoreConfirm.value = false }, confirmButton = { Column { TextButton(onClick = { viewModel.restoreDefaultExercises(keepCustom = true); showRestoreConfirm.value = false; Toast.makeText(context, "Restaurado (con personalizados)", Toast.LENGTH_SHORT).show() }) { Text("MANTENER MIS EJERCICIOS", color = Color(0xFF00FF00)) }; TextButton(onClick = { viewModel.restoreDefaultExercises(keepCustom = false); showRestoreConfirm.value = false; Toast.makeText(context, "Restaurado de fábrica", Toast.LENGTH_SHORT).show() }) { Text("BORRAR Y RESTAURAR", color = Color.Yellow) } } }, dismissButton = { TextButton(onClick = { showRestoreConfirm.value = false }) { Text("CANCELAR", color = Color.Gray) } }, containerColor = Color(0xFF1A1C20), title = { Text("¿Restaurar ejercicios?", color = Color.White) }, text = { Text("Puedes elegir mantener tus ejercicios personalizados o borrarlos.", color = Color.Gray) })
    }

    if (showDatePicker.value) {
        DatePickerDialog(onDismissRequest = { showDatePicker.value = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { birthDateMillisState.longValue = it; prefs.edit { putLong("userBirthDate", it) } }; showDatePicker.value = false }) { Text("ACEPTAR", color = Color(0xFF00FF00)) } }, dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text("CANCELAR", color = Color.Gray) } }, colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1C20))) { DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(titleContentColor = Color.White, headlineContentColor = Color.White, selectedDayContainerColor = Color(0xFF00FF00), selectedDayContentColor = Color.Black, todayContentColor = Color(0xFF00FF00), todayDateBorderColor = Color(0xFF00FF00))) }
    }

    if (showResetConfirm.value) {
        AlertDialog(onDismissRequest = { showResetConfirm.value = false }, confirmButton = { TextButton(onClick = { prefs.edit { clear() }; viewModel.resetEverything(); viewModel.restoreDefaultExercises(keepCustom = false); showResetConfirm.value = false; Toast.makeText(context, "App reseteada", Toast.LENGTH_LONG).show() }) { Text("SÍ, BORRAR TODO", color = Color.Red) } }, dismissButton = { TextButton(onClick = { showResetConfirm.value = false }) { Text("CANCELAR", color = Color.Gray) } }, containerColor = Color(0xFF1A1C20), title = { Text("¿Resetear TODO?", color = Color.White) }, text = { Text("Se borrarán todos tus datos. ¿Estás seguro?", color = Color.Gray) })
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = dailyReminderHour, initialMinute = dailyReminderMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { dailyReminderHour = timePickerState.hour; dailyReminderMinute = timePickerState.minute; prefs.edit { putInt("daily_reminder_hour", dailyReminderHour); putInt("daily_reminder_minute", dailyReminderMinute) }; viewModel.updateNotificationSettings(notificationsEnabled, dailyHour = dailyReminderHour, dailyMinute = dailyReminderMinute, maxDaysWithoutWorkout = maxDaysWithoutWorkout, prEnabled = prNotificationsEnabled, consistencyEnabled = consistencyNotificationsEnabled, weeklyGoalEnabled = weeklyGoalNotificationsEnabled, weeklyGoal = weeklyWorkoutGoal); showTimePicker = false }) { Text("ACEPTAR", color = Color(0xFF00FF00)) } }, dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("CANCELAR", color = Color.Gray) } }, containerColor = Color(0xFF1A1C20), title = { Text("Hora recordatorio", color = Color.White) }, text = { TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(clockDialColor = Color.White.copy(0.1f), clockDialSelectedContentColor = Color(0xFF00FF00), clockDialUnselectedContentColor = Color.White, selectorColor = Color(0xFF00FF00), timeSelectorSelectedContainerColor = Color(0xFF00FF00), timeSelectorUnselectedContainerColor = Color.White.copy(0.1f), timeSelectorSelectedContentColor = Color.Black, timeSelectorUnselectedContentColor = Color.White)) })
    }
}
