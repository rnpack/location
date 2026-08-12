package com.rnpack.location.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.facebook.react.HeadlessJsTaskService
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.rnpack.location.Configs
import com.rnpack.location.Constants
import com.rnpack.location.data.BackgroundLocationConfiguration
import com.rnpack.location.dtos.LocationDTO
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

const val TAG = "BackgroundLocationWorker"

class BackgroundLocationWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  companion object {
    const val WORK_NAME = "BgLocationWorker"
    private const val TAG = "BackgroundLocationWork"
  }

  private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

  @SuppressLint("MissingPermission")
  override suspend fun doWork(): Result {

    val locationConfigsString: String? =
      inputData.getString(Constants.BACKGROUND_LOCATION_WORKER_CONFIGURATION_KEY)

    Log.d(Constants.LOG_TAG, "$TAG inputData: $locationConfigsString")

    var locationConfigs = BackgroundLocationConfiguration(
      url = null,
      priority = Configs.DEFAULT_LOCATION_CONFIGURATION.priority,
      maxUpdateAgeMillis = Configs.DEFAULT_LOCATION_CONFIGURATION.maxUpdateAgeMillis,
      granularity = Configs.DEFAULT_LOCATION_CONFIGURATION.granularity,
      durationMillis = Configs.DEFAULT_LOCATION_CONFIGURATION.durationMillis,
    )

    if (locationConfigsString != null) {
      locationConfigs = BackgroundLocationConfiguration.fromJson(locationConfigsString)
    }

    if (locationConfigsString == null) {
      val sharedPrefs = applicationContext.getSharedPreferences(
        Configs.BACKGROUND_LOCATION_CONFIG_SHARED_PREFERENCES_DATABASE, Context.MODE_PRIVATE
      )

      val data = sharedPrefs.getString(
        Constants.BACKGROUND_LOCATION_WORKER_CONFIGURATION_KEY, locationConfigs.toString()
      ) ?: locationConfigs.toString()

      locationConfigs = BackgroundLocationConfiguration.fromJson(data)
    }

    Log.d(Constants.LOG_TAG, "$TAG location config data: $locationConfigs")

    val currentLocationRequest = CurrentLocationRequest.Builder(
    ).setPriority(locationConfigs.priority)
      .setMaxUpdateAgeMillis(locationConfigs.maxUpdateAgeMillis)
      .setGranularity(locationConfigs.granularity).setDurationMillis(locationConfigs.durationMillis)
      .build()

    if (ActivityCompat.checkSelfPermission(
        applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      Log.e(Constants.LOG_TAG, "$TAG Permissions missing. Stopping worker.")
      return Result.failure()
    } else {
      Log.i(
        Constants.LOG_TAG,
        "$TAG Location Permission granted in BackgroundLocationWorker === applicationContext: $applicationContext"
      )
    }

    try {
      Log.d(
        Constants.LOG_TAG,
        "$TAG Starting location client for location updates... applicationContext: $applicationContext"
      )

      fusedLocationProviderClient.getCurrentLocation(
        currentLocationRequest, CancellationTokenSource().token
      ).addOnSuccessListener {

        Log.i(
          Constants.LOG_TAG, "$TAG on location update => url: ${locationConfigs.url}, location: $it"
        )

        triggerHeadlessJsTask(it, locationConfigs.url)
      }

      return Result.success()
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching background location", e)
      return Result.retry() // Retries if it fails due to network/transient issues
    }
  }


  private fun triggerHeadlessJsTask(location: Location, url: String?) {

    Log.d(
      Constants.LOG_TAG,
      "$TAG triggerHeadlessJsTask $location and applicationContext: $applicationContext"
    )

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
      val bundle = Bundle().apply {
        putDouble(LocationDTO::latitude.name, location.latitude)
        putDouble(LocationDTO::longitude.name, location.longitude)
        putDouble(LocationDTO::altitude.name, location.altitude)
        putDouble(LocationDTO::accuracy.name, location.accuracy.toDouble())
        putLong(LocationDTO::timestamp.name, location.time)
        putBoolean(LocationDTO::isMocked.name, location.isFromMockProvider)
        putString(LocationDTO::provider.name, location.provider)
      }

      // Direct intent execution targeting our custom LocationTaskService
      val serviceIntent =
        Intent(applicationContext, BackgroundLocationHeadlessJsTaskService::class.java).apply {
          putExtras(bundle)
        }

      HeadlessJsTaskService.acquireWakeLockNow(applicationContext)


      applicationContext.startService(serviceIntent)
    }

    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O && url?.isNotBlank() == true) {
      val apiClient = OkHttpClient()

      val jsonObject = JSONObject()

      jsonObject.put(LocationDTO::latitude.name, location.latitude)
      jsonObject.put(LocationDTO::longitude.name, location.longitude)
      jsonObject.put(LocationDTO::altitude.name, location.altitude)
      jsonObject.put(LocationDTO::accuracy.name, location.accuracy.toDouble())
      jsonObject.put(LocationDTO::timestamp.name, location.time)
      jsonObject.put(LocationDTO::isMocked.name, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider)
      jsonObject.put(LocationDTO::provider.name, location.provider)

      val body = jsonObject.toString().toRequestBody()

      val request = Request.Builder().url(url).post(body).build()

      apiClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
          Log.d(Constants.LOG_TAG, "$TAG send data to server fails call: $call")
          Log.e(Constants.LOG_TAG, "$TAG send data to server fails: {}", e)
        }

        override fun onResponse(call: Call, response: Response) {
          response.use {
            if (!response.isSuccessful) {
              Log.e(Constants.LOG_TAG, "$TAG send data to server fails: $response")
              return
            }

            Log.i(
              Constants.LOG_TAG, "$TAG send data to server success: ${response.body?.string()}"
            )
          }
        }
      })
    }
  }
}
