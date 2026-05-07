package com.example.Gym_App.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "¡Hora de entrenar!"
        val message = intent.getStringExtra("message") ?: "Es momento de tu rutina diaria"

        NotificationHelper.sendNotification(context, title, message)
    }
}
