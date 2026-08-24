package com.example.shakeflashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ShakeDetectorService.ACTION_STOP) {
            val stopIntent = Intent(context, ShakeDetectorService::class.java).apply {
                action = ShakeDetectorService.ACTION_STOP
            }
            context.startService(stopIntent)
        }
    }
}
