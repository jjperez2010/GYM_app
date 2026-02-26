package com.example.appdeprueba.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }
    val pronoun = prefs.getString("userPronoun", "Él")
    val name = prefs.getString("userName", "") ?: ""
    val greetingBase = when(pronoun) { "Ella" -> "Bienvenida"; "Elle" -> "Bienvenide"; else -> "Bienvenido" }
    val welcomeText = if (name.isNotBlank()) "¡$greetingBase $name!\n¡Vamos a entrenar!" else "¡$greetingBase!\n¡Vamos a entrenar!"
    
    LaunchedEffect(Unit) { 
        delay(2500)
        onFinished()
    }
    
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(
            text = welcomeText, 
            color = Color(0xFF00FF00), 
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center, 
            lineHeight = 40.sp
        )
    }
}
