package com.example.shakeflashlight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "com.example.shakeflashlight.action.START"
        const val ACTION_STOP = "com.example.shakeflashlight.action.STOP"
        const val ACTION_TORCH_STATE_CHANGED = "com.example.shakeflashlight.action.TORCH_STATE_CHANGED"
        const val EXTRA_TORCH_ON = "torch_on"

        private const val NOTIFICATION_CHANNEL_ID = "shake_flashlight_channel"
        private const val NOTIFICATION_ID = 1001

        // Minimum time between two toggles, so one violent shake can't
        // register as multiple on/off flips.
        private const val DEBOUNCE_MS = 700L
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    private var torchOn = false
    private var threshold = Prefs.MIN_THRESHOLD

    // Shake state machine: we only allow a new trigger once acceleration
    // has fallen back under the threshold after the last spike. This turns
    // one continuous shake gesture into a single toggle instead of many.
    private var armed = true
    private var lastToggleTime = 0L

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.KEY_SENSITIVITY_PUBLIC) {
            threshold = Prefs.getThreshold(this)
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraIdCallback: String, enabled: Boolean) {
            if (cameraIdCallback == cameraId) {
                torchOn = enabled
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = findFlashCameraId()
        cameraManager.registerTorchCallback(torchCallback, null)

        threshold = Prefs.getThreshold(this)
        Prefs.registerListener(this, prefsListener)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        startForeground(NOTIFICATION_ID, buildNotification())
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        Prefs.setServiceEnabled(this, true)
    }

    private fun findFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val hasFlash = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val facing = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING)
                hasFlash == true && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Magnitude of acceleration including gravity; subtracting standard
        // gravity gives roughly how hard the phone is being shaken.
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = magnitude - SensorManager.GRAVITY_EARTH

        if (delta > threshold) {
            if (armed) {
                val now = System.currentTimeMillis()
                if (now - lastToggleTime > DEBOUNCE_MS) {
                    toggleTorch()
                    lastToggleTime = now
                }
                armed = false
            }
        } else if (delta < threshold * 0.5f) {
            // Acceleration has settled back down; allow the next shake to trigger.
            armed = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun toggleTorch() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, !torchOn)
            // torchOn is updated via the torch callback above, but we set it
            // here too so the notification updates immediately.
            torchOn = !torchOn
            updateNotification()
        } catch (e: Exception) {
            // Camera might be busy (e.g. another app is using it); ignore.
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (torchOn) getString(R.string.notification_text_on)
        else getString(R.string.notification_text_off)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_flash)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop), stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Prefs.unregisterListener(this, prefsListener)
        cameraManager.unregisterTorchCallback(torchCallback)
        // Make sure we never leave the flashlight stuck on if the service dies.
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, false)
            } catch (e: Exception) { /* ignore */ }
        }
        Prefs.setServiceEnabled(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
