package com.example.appdeprueba.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.appdeprueba.ui.components.BottomNavBar
import com.example.appdeprueba.ui.components.MenuButton
import com.example.appdeprueba.ui.components.NumericStepper
import com.example.appdeprueba.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: GymViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    
    var name by remember { mutableStateOf(prefs.getString("userName", "") ?: "") }
    var pronoun by remember { mutableStateOf(prefs.getString("userPronoun", "Él") ?: "Él") }
    var gender by remember { mutableStateOf(prefs.getString("userGender", "Masculino") ?: "Masculino") }
    var birthDateMillis by remember { mutableLongStateOf(prefs.getLong("userBirthDate", 0L)) }
    var level by remember { mutableStateOf(prefs.getString("userLevel", "Novato") ?: "Novato") }
    var weight by remember { mutableIntStateOf(prefs.getInt("userWeight", 70)) }
    var height by remember { mutableIntStateOf(prefs.getInt("userHeight", 170)) }
    var globalWait by remember { mutableIntStateOf(prefs.getInt("globalWait", 20)) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (birthDateMillis != 0L) birthDateMillis else null,
        yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR)
    )

    fun calculateAge(birthMillis: Long): Int {
        if (birthMillis == 0L) return 0
        val dob = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    val displayDate = if (birthDateMillis == 0L) "Seleccionar fecha" 
                     else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDateMillis))
    val age = calculateAge(birthDateMillis)

    Scaffold(bottomBar = { BottomNavBar(navController, "settings") }) { innerPadding ->
        val gradientSettings = Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF424242)))
        Box(Modifier.fillMaxSize().background(brush = gradientSettings).padding(innerPadding)) {
            Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Perfil y Configuración", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it; prefs.edit { putString("userName", it) } }, 
                    label = { Text("Nombre") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color(0xFF00FF00), unfocusedLabelColor = Color.Gray)
                )
                
                Spacer(Modifier.height(16.dp))
                Text("¿Cómo prefieres que te llamemos?", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Él", "Ella", "Elle").forEach { p -> 
                        FilterChip(selected = pronoun == p, onClick = { pronoun = p; prefs.edit { putString("userPronoun", p) } }, label = { Text(p) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00))) 
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Sexo", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Masculino", "Femenino", "Otro").forEach { g -> 
                        FilterChip(selected = gender == g, onClick = { gender = g; prefs.edit { putString("userGender", g) } }, label = { Text(g) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFF00FF00).copy(0.7f))) 
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text("Fecha de Nacimiento", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Card(
                    Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(0.3f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF00FF00))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(displayDate, color = Color.White, fontSize = 16.sp)
                            if (age > 0) {
                                Text("Edad calculada: $age años", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Nivel de Entrenamiento", color = Color.Gray, modifier = Modifier.align(Alignment.Start), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("Novato", "Intermedio", "Avanzado").forEach { l -> 
                        FilterChip(selected = level == l, onClick = { level = l; prefs.edit { putString("userLevel", l) } }, label = { Text(l) }, colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = Color(0xFFBB86FC))) 
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                NumericStepper("Peso (kg)", weight, 1, 5) { weight = it; prefs.edit { putInt("userWeight", it) } }
                NumericStepper("Estatura (cm)", height, 1, 5) { height = it; prefs.edit { putInt("userHeight", it) } }
                
                HorizontalDivider(Modifier.padding(vertical = 24.dp), color = Color.Gray.copy(0.3f))
                Text("Configuración de Rutina", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(16.dp))
                NumericStepper("Tiempo de preparación previa (s)", globalWait, 5, 10) { globalWait = it; prefs.edit { putInt("globalWait", it) } }
                
                Spacer(Modifier.height(24.dp))
                
                MenuButton("RESTAURAR EJERCICIOS POR DEFECTO", Modifier.fillMaxWidth(), bColor = Color.Yellow) {
                    showRestoreConfirm = true
                }
                
                Spacer(Modifier.height(16.dp))
                
                MenuButton("RESET TOTAL DE LA APP", Modifier.fillMaxWidth(), bColor = Color.Red) {
                    showResetConfirm = true
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("¿Restaurar ejercicios?", color = Color.White) },
            text = { Text("Puedes elegir mantener tus ejercicios personalizados o borrarlos para dejar solo los de fábrica.", color = Color.Gray) },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        viewModel.restoreDefaultExercises(keepCustom = true)
                        showRestoreConfirm = false
                        Toast.makeText(context, "Ejercicios restaurados (manteniendo personalizados)", Toast.LENGTH_SHORT).show()
                    }) { Text("MANTENER MIS EJERCICIOS", color = Color(0xFF00FF00)) }
                    
                    TextButton(onClick = {
                        viewModel.restoreDefaultExercises(keepCustom = false)
                        showRestoreConfirm = false
                        Toast.makeText(context, "Ejercicios restaurados de fábrica", Toast.LENGTH_SHORT).show()
                    }) { Text("BORRAR Y RESTAURAR", color = Color.Yellow) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        birthDateMillis = it
                        prefs.edit { putLong("userBirthDate", it) }
                    }
                    showDatePicker = false
                }) { Text("ACEPTAR", color = Color(0xFF00FF00)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR", color = Color.Gray) }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1C20))
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    selectedDayContainerColor = Color(0xFF00FF00),
                    selectedDayContentColor = Color.Black,
                    todayContentColor = Color(0xFF00FF00),
                    todayDateBorderColor = Color(0xFF00FF00)
                )
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = Color(0xFF1A1C20),
            title = { Text("¿Resetear TODO?", color = Color.White) },
            text = { Text("Esta acción borrará:\n- Todos tus datos de perfil\n- Todo el historial de peso\n- Todas las rutinas creadas\n- Todos los ejercicios personalizados\n\nSolo quedarán los ejercicios por defecto. ¿Estás seguro?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    // Limpiar SharedPreferences
                    prefs.edit { clear() }
                    
                    // Resetear estados locales para reflejar UI vacía
                    name = ""
                    pronoun = "Él"
                    gender = "Masculino"
                    birthDateMillis = 0L
                    level = "Novato"
                    weight = 70
                    height = 170
                    globalWait = 20
                    
                    // Resetear base de datos y cargar solo defaults
                    viewModel.resetEverything()
                    viewModel.restoreDefaultExercises(keepCustom = false)
                    
                    showResetConfirm = false
                    Toast.makeText(context, "App reseteada por completo", Toast.LENGTH_LONG).show()
                }) { Text("SÍ, BORRAR TODO", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("CANCELAR", color = Color.Gray) }
            }
        )
    }
}
