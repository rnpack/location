package com.rnpack.location.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.rnpack.location.Constants
import com.rnpack.location.dtos.LocationDTO

class BackgroundLocationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {

    Log.d(Constants.LOG_TAG, "BackgroundLocationReceiver calling...")

    if (LocationResult.hasResult(intent)) {
      val locationResult = LocationResult.extractResult(intent) ?: return
      val lastLocation = locationResult.lastLocation ?: return

      val bundle = Bundle().apply {
        putDouble(LocationDTO::latitude.name, lastLocation.latitude)
        putDouble(LocationDTO::longitude.name, lastLocation.longitude)
        putDouble(LocationDTO::altitude.name, lastLocation.altitude)
        putDouble(LocationDTO::accuracy.name, lastLocation.accuracy.toDouble())
        putDouble(LocationDTO::timestamp.name, lastLocation.time.toDouble())
      }

      val serviceIntent = Intent(context, BackgroundLocationHeadlessJsTaskService::class.java)

      serviceIntent.putExtras(bundle)

      context.startService(serviceIntent)
    }
  }
}


