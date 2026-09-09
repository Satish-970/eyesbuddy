package com.example.eyesbuddy.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Reports whether approximate location may be used; the Activity owns the request launcher. */
class LocationPermissionGate(private val context: Context) {
    val isGranted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
