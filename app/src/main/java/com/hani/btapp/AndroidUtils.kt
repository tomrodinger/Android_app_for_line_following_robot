package com.hani.btapp

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.content.PermissionChecker

/**
 * Created by hanif on 2022-07-23.
 */
object AndroidUtils {

    fun isBluetoothPermissionGranted(context: Context): Boolean {
        val scan = hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        val connect = hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        val admin = hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        val bt = hasPermission(context, Manifest.permission.BLUETOOTH)
        val location = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        Logger.log("scan:$scan, connect:$connect, admin:$admin, bt:$bt, location:$location")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return location
        }
        return scan && connect
    }

    fun getDeviceName(): String {
        return "Android (" + Build.BRAND.toString() + " " +
                Build.MODEL.toString() + ")"
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return PermissionChecker.checkSelfPermission(context, permission) ==
                PermissionChecker.PERMISSION_GRANTED
    }

}

fun Context.toast(
    message: String,
    duration: Int = Toast.LENGTH_SHORT,
) = Toast.makeText(this, message, duration).show()