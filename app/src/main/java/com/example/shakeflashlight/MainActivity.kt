package com.example.shakeflashlight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.shakeflashlight.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            startShakeService()
        } else {
            binding.masterSwitch.isChecked = false
            Toast.makeText(
                this,
                getString(R.string.permission_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSensitivitySlider()
        setupMasterSwitch()
    }

    override fun onResume() {
        super.onResume()
        // Reflect real service state without re-triggering the listener logic.
        binding.masterSwitch.setOnCheckedChangeListener(null)
        binding.masterSwitch.isChecked = Prefs.isServiceEnabled(this)
        updateStatusText(Prefs.isServiceEnabled(this))
        setupMasterSwitch()
    }

    private fun setupSensitivitySlider() {
        val currentSeek = Prefs.getSensitivitySeek(this)
        binding.sensitivitySeekBar.progress = currentSeek

        binding.sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    Prefs.setSensitivitySeek(this@MainActivity, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupMasterSwitch() {
        binding.masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestPermissionsThenStart()
            } else {
                stopShakeService()
            }
            updateStatusText(isChecked)
        }
    }

    private fun requestPermissionsThenStart() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isEmpty()) {
            startShakeService()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startShakeService() {
        val intent = Intent(this, ShakeDetectorService::class.java).apply {
            action = ShakeDetectorService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopShakeService() {
        val intent = Intent(this, ShakeDetectorService::class.java).apply {
            action = ShakeDetectorService.ACTION_STOP
        }
        startService(intent)
    }

    private fun updateStatusText(enabled: Boolean) {
        binding.statusText.text = if (enabled) {
            getString(R.string.subtitle_status_on)
        } else {
            getString(R.string.subtitle_status_off)
        }
    }
}
