package com.rnpack.location.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.facebook.react.ReactApplication
import com.facebook.react.bridge.ReactApplicationContext
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.rnpack.location.Constants
import com.rnpack.location.RNPackLocationImpl
import com.rnpack.location.dtos.LocationDTO

class LocationForegroundService : Service() {

  val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {

      for (location in locationResult.locations) {

        val intent = Intent(Constants.LOCATION_FOREGROUND_SERVICE_BROADCAST_NAME)


        intent.putExtra(LocationDTO::latitude.name, location.latitude)
        intent.putExtra(LocationDTO::longitude.name, location.longitude)
        intent.putExtra(LocationDTO::altitude.name, location.altitude)
        intent.putExtra(LocationDTO::accuracy.name, location.accuracy)
        intent.putExtra(LocationDTO::timestamp.name, location.time)

        intent.setPackage(packageName)

        sendBroadcast(intent)
      }
    }
  }


  override fun onBind(intent: Intent?): IBinder? = null

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    val reactApplicationContext: ReactApplicationContext? =
      (application as ReactApplication).reactHost?.currentReactContext as ReactApplicationContext?

    val rnPackLocation = RNPackLocationImpl()

    when (intent?.action) {
      Actions.START.toString() -> start()
      Actions.STOP.toString() -> {
        if (reactApplicationContext != null) {
          rnPackLocation.unsubscribeFromLocationChange(reactApplicationContext, locationCallback)
        }

        stopSelf()
      }
    }

    return super.onStartCommand(intent, flags, startId)
  }

  @SuppressLint("MissingPermission")
  fun start() {

    val rnPackLocation = RNPackLocationImpl()

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
      .setMinUpdateIntervalMillis(5000).build()

    val reactApplicationContext: ReactApplicationContext? =
      (application as ReactApplication).reactHost?.currentReactContext as ReactApplicationContext?

    // Example: Start location updates
    Log.d(Constants.LOG_TAG, "LocationService::Location tracking started...$START_STICKY")

    if (reactApplicationContext != null) {

      val notification = createNotification(reactApplicationContext)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          Constants.FOREGROUND_LOCATION_SERVICE_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
      } else {
        startForeground(
          Constants.FOREGROUND_LOCATION_SERVICE_NOTIFICATION_ID, notification
        )
      }

      val isFine = ContextCompat.checkSelfPermission(
        reactApplicationContext,
        Manifest.permission.ACCESS_FINE_LOCATION,
      ) == PackageManager.PERMISSION_GRANTED

      val isCoarse = ContextCompat.checkSelfPermission(
        reactApplicationContext,
        Manifest.permission.ACCESS_COARSE_LOCATION,
      ) == PackageManager.PERMISSION_GRANTED

      if (!isCoarse && !isFine) {
        Log.e(Constants.LOG_TAG, "Location permission not granted")
        return
      }

      rnPackLocation.subscribeToLocationChange(
        reactApplicationContext, locationRequest, locationCallback
      )
    }
  }


  private fun createNotification(reactApplicationContext: ReactApplicationContext): Notification {
    val channelId = Constants.LOCATION_FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId, "Location Tracking", NotificationManager.IMPORTANCE_HIGH
      )

      val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

      manager.createNotificationChannel(channel)
    }

    val appInfo = reactApplicationContext.packageManager.getApplicationInfo(
      reactApplicationContext.packageName, 0
    )
    val appIcon = appInfo.icon

    return NotificationCompat.Builder(this, channelId)
      .setContentTitle(reactApplicationContext.packageManager.getApplicationLabel(appInfo))
      .setContentText("Tracking your location…").setSmallIcon(appIcon).build()
  }

  fun triggerLocationForegroundServiceHeadlessJsTask(
    reactApplicationContext: ReactApplicationContext, location: LocationDTO
  ) {

    val serviceIntent =
      Intent(reactApplicationContext, ForegroundLocationHeadlessJsTaskService::class.java)

    val bundle = Bundle().apply {
      putDouble(LocationDTO::latitude.name, location.latitude)
      putDouble(LocationDTO::longitude.name, location.longitude)
      putDouble(LocationDTO::altitude.name, location.altitude)
      putDouble(LocationDTO::accuracy.name, location.accuracy)
      putLong(LocationDTO::timestamp.name, location.timestamp)
    }

    serviceIntent.putExtras(bundle)

    reactApplicationContext.startService(serviceIntent)
  }

  enum class Actions {
    START, STOP
  }
}
