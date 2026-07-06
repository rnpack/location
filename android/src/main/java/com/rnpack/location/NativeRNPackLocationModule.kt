package com.rnpack.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WindowFocusChangeListener
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.rnpack.location.data.BackgroundLocationConfiguration
import com.rnpack.location.data.LocationAccessPermissionResult
import com.rnpack.location.data.LocationConfiguration
import com.rnpack.location.dtos.LocationDTO
import com.rnpack.location.services.BackgroundLocationWorker
import com.rnpack.location.services.LocationForegroundService
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NativeRNPackLocationModule(reactContext: ReactApplicationContext) :
  NativeRNPackLocationSpec(reactContext), LifecycleEventListener, ActivityEventListener,
  WindowFocusChangeListener {

  private val workManager = WorkManager.getInstance(reactContext)

  var locationConfiguration: LocationConfiguration = Configs.DEFAULT_LOCATION_CONFIGURATION;

  var backgroundLocationConfiguration = BackgroundLocationConfiguration(
    url = null,
    priority = locationConfiguration.priority,
    maxUpdateAgeMillis = locationConfiguration.maxUpdateAgeMillis,
    granularity = locationConfiguration.granularity,
    durationMillis = locationConfiguration.durationMillis,
  )

  var locationPermissionsChangeSubscriptionsCount = 0
  var locationChangeSubscriptionsCount = 0

  val rnPackLocation = RNPackLocationImpl()
  val locationForegroundService = LocationForegroundService()

  private val locationProvidersChangeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
        try {
          emitOnLocationProvidersChange(isLocationEnabled)
        } catch (e: Exception) {
          Log.e(Constants.LOG_TAG, "JS listener not ready or already invalidated", e)
        }
      }
    }
  }

  private val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_BALANCED_POWER_ACCURACY, locationConfiguration.intervalMillis
  ).setMinUpdateIntervalMillis(locationConfiguration.minUpdateIntervalMillis)
    .setMinUpdateDistanceMeters(locationConfiguration.minUpdateDistanceMeters)
    .setMaxUpdateAgeMillis(locationConfiguration.maxUpdateAgeMillis)
    .setWaitForAccurateLocation(locationConfiguration.waitForAccurateLocation)
    .setMaxUpdateDelayMillis(locationConfiguration.maxUpdateDelayMillis)
    .setGranularity(locationConfiguration.granularity)
    .setDurationMillis(locationConfiguration.durationMillis).build()

  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {

      for (location in locationResult.locations) {
        emitOnLocationChange(LocationDTO.fromAndroidLocation(location).toWritableMap())
      }
    }
  }

  override fun initialize() {
    super.initialize()

    reactApplicationContext.addLifecycleEventListener(this)
    reactApplicationContext.addActivityEventListener(this)
    reactApplicationContext.addWindowFocusChangeListener(this)

    rnPackLocation.cancelLocationRequest()

    Log.d(Constants.LOG_TAG, "TurboModuleLifeCycle: initialize")
  }


  override fun invalidate() {
    super.invalidate()

    unsubscribeFromLocationProvidersChange()

    locationPermissionsChangeSubscriptionsCount = 0
    unsubscribeFromLocationPermissionsChange()

    locationChangeSubscriptionsCount = 0
    unsubscribeFromLocationChange()

    Log.d(Constants.LOG_TAG, "TurboModuleLifeCycle: invalidate")
  }

  override fun onHostDestroy() {
    Log.d(Constants.LOG_TAG, "LifeCycleEventListener: onHostDestroy")
  }

  override fun onHostPause() {
    Log.d(Constants.LOG_TAG, "LifeCycleEventListener: onHostPause")
  }

  override fun onHostResume() {
    Log.d(Constants.LOG_TAG, "LifeCycleEventListener: onHostResume")
  }

  override fun onActivityResult(
    activity: Activity, requestCode: Int, resultCode: Int, data: Intent?
  ) {
    Log.d(
      Constants.LOG_TAG,
      "ActivityEventListener: onActivityResult :: activity = ${activity}, requestCode = $requestCode, resultCode = $resultCode, data = $data"
    )
  }

  override fun onNewIntent(intent: Intent) {
    Log.d(Constants.LOG_TAG, "ActivityEventListener: onNewIntent :: $intent")
  }

  override fun onWindowFocusChange(hasFocus: Boolean) {
    Log.d(Constants.LOG_TAG, "WindowFocusChangeListener: onWindowFocusChange :: $hasFocus")

    if (hasFocus && locationPermissionsChangeSubscriptionsCount > 0) {
      emitOnLocationPermissionsChange(isLocationAuthorized)
    }
  }

  override fun configureLocation(config: ReadableMap) {
    val configuration: LocationConfiguration = LocationConfiguration.fromReadableMap(config)

    locationConfiguration = configuration
  }

  override fun isLocationEnabled(): Boolean {
    return rnPackLocation.isLocationEnabled(reactApplicationContext)
  }

  override fun isLocationAuthorized(): WritableMap {
    val result = rnPackLocation.isLocationAuthorized(reactApplicationContext)

    val locationAccessPermissionResult = LocationAccessPermissionResult(
      status = result.fine || result.coarse,
      fine = result.fine,
      coarse = result.coarse
    )

    return locationAccessPermissionResult.toWritableMap()
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getLastLocation(promise: Promise) {

    rnPackLocation.getLastLocation(
      reactApplicationContext,

      onSuccess = { lastLocation ->
        promise.resolve(LocationDTO.fromAndroidLocation(lastLocation).toWritableMap())
      }, onError = { exception ->
        promise.reject(exception)
      })
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getCurrentLocation(
    priority: Double?, promise: Promise
  ) {

    val locationPriority: Int = priority?.toInt() ?: locationConfiguration.priority

    rnPackLocation.getCurrentLocation(
      reactApplicationContext,
      locationPriority,
      locationConfiguration,
      onSuccess = { lastLocation ->
        promise.resolve(LocationDTO.fromAndroidLocation(lastLocation).toWritableMap())
      },
      onError = { exception ->
        promise.reject(exception)
      })
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getFreshCurrentLocation(priority: Double?, promise: Promise) {
    val locationPriority: Int = priority?.toInt() ?: Priority.PRIORITY_BALANCED_POWER_ACCURACY

    rnPackLocation.getFreshCurrentLocation(
      reactApplicationContext,
      locationPriority,
      onSuccess = { lastLocation ->
        promise.resolve(LocationDTO.fromAndroidLocation(lastLocation).toWritableMap())
      },
      onError = { exception ->
        promise.reject(exception)
      })
  }

  override fun openLocationProvidersSettings() {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    reactApplicationContext.startActivity(intent)
  }

  override fun openLocationPermissionsSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", reactApplicationContext.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    reactApplicationContext.startActivity(intent)
  }


  override fun subscribeToLocationPermissionsChange() {
    locationPermissionsChangeSubscriptionsCount++
  }

  override fun unsubscribeFromLocationPermissionsChange() {
    if (locationPermissionsChangeSubscriptionsCount > 0) {
      locationPermissionsChangeSubscriptionsCount--
    }
  }

  override fun subscribeToLocationProvidersChange() {
    rnPackLocation.subscribeToLocationProvidersChange(
      reactApplicationContext, locationProvidersChangeReceiver
    )
  }

  override fun unsubscribeFromLocationProvidersChange() {
    rnPackLocation.unsubscribeFromLocationProvidersChange(
      reactApplicationContext, locationProvidersChangeReceiver
    )
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun subscribeToLocationChange() {

    rnPackLocation.subscribeToLocationChange(
      reactApplicationContext, locationRequest, locationCallback
    )
  }

  override fun unsubscribeFromLocationChange() {
    rnPackLocation.unsubscribeFromLocationChange(reactApplicationContext, locationCallback)
  }

  override fun startLocationForegroundService() {
    val intent = Intent(reactApplicationContext, LocationForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Log.d(Constants.LOG_TAG, "Start location foreground service calling...")
      reactApplicationContext.startForegroundService(intent)

      val foregroundLocationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val latitude = intent.getDoubleExtra(LocationDTO::latitude.name, 0.0)
          val longitude = intent.getDoubleExtra(LocationDTO::longitude.name, 0.0)
          val altitude = intent.getDoubleExtra(LocationDTO::altitude.name, 0.0)
          val accuracy = intent.getFloatExtra(LocationDTO::accuracy.name, 0F)
          val timestamp = intent.getLongExtra(LocationDTO::timestamp.name, 0L)
          val isMocked = intent.getBooleanExtra(LocationDTO::isMocked.name, false)

          val locationDto = LocationDTO(
            latitude, longitude, altitude, accuracy.toDouble(), timestamp, isMocked
          )

          Log.d(
            Constants.LOG_TAG, "Foreground Service Location Updates: $locationDto"
          )

          locationForegroundService.triggerLocationForegroundServiceHeadlessJsTask(
            reactApplicationContext, locationDto
          )

          emitOnLocationForegroundServiceChange(
            locationDto.toWritableMap()
          )
        }
      }

      val intentFilter = IntentFilter(Constants.LOCATION_FOREGROUND_SERVICE_BROADCAST_NAME)


      ContextCompat.registerReceiver(
        reactApplicationContext,
        foregroundLocationReceiver,
        intentFilter,
        ContextCompat.RECEIVER_NOT_EXPORTED
      )
    } else {
      reactApplicationContext.startService(intent)
    }

    val lfsIntent = Intent(reactApplicationContext, LocationForegroundService::class.java)
    lfsIntent.action = LocationForegroundService.Actions.START.toString()
    reactApplicationContext.startService(lfsIntent)
  }

  override fun stopLocationForegroundService() {
    Log.d(Constants.LOG_TAG, "Stop location foreground service calling....")

    val intent = Intent(reactApplicationContext, LocationForegroundService::class.java)
    intent.action = LocationForegroundService.Actions.STOP.toString()
    reactApplicationContext.startService(intent)

    Log.d(Constants.LOG_TAG, "Stopped location foreground service....")
  }

  override fun wakeUpApp() {
    Log.d(Constants.LOG_TAG, "Wake Up app calling...")
    val focusIntent: Intent? =
      reactApplicationContext.packageManager.getLaunchIntentForPackage(reactApplicationContext.packageName)

    if (focusIntent != null) {
      Log.d(Constants.LOG_TAG, "focusIntent is not null .. trying to launch the app...")
      focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
      reactApplicationContext.startActivity(focusIntent)
    } else {
      Log.d(Constants.LOG_TAG, "focusIntent is null")
    }
  }

  override fun requestLocationPermission() {

    val permissions = arrayOf(
      Manifest.permission.ACCESS_COARSE_LOCATION,
      Manifest.permission.ACCESS_FINE_LOCATION,
    )

    val activity = reactApplicationContext.currentActivity as? PermissionAwareActivity ?: return

    rnPackLocation.requestAccessLocationPermission(
      activity, permissions, Constants.LOCATION_PERMISSION_REQ_CODE
    )
  }

  override fun requestBackgroundLocationPermission() {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      return
    }

    val permissions = arrayOf(
      Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    val activity = reactApplicationContext.currentActivity as? PermissionAwareActivity ?: return

    rnPackLocation.requestBackgroundLocationPermission(
      reactApplicationContext, activity, permissions, Constants.LOCATION_PERMISSION_REQ_CODE
    )
  }

  override fun configureBackgroundLocation(config: ReadableMap) {
    val configuration: BackgroundLocationConfiguration =
      BackgroundLocationConfiguration.fromReadableMap(config)

    backgroundLocationConfiguration = configuration
  }

  @SuppressLint("MissingPermission")
  override fun startLocationBackgroundService() {

    val access = rnPackLocation.isLocationAuthorized(reactApplicationContext)

    if (!access.fine) {
      return
    }

    var isBackground = true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      isBackground = ContextCompat.checkSelfPermission(
        reactApplicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    }

    if (!isBackground) {
      return
    }

    try {
      val backgroundLocationConfigsJson = JSONObject().apply {
        put(
          BackgroundLocationConfiguration::url.name, backgroundLocationConfiguration.url
        )
        put(
          BackgroundLocationConfiguration::priority.name, backgroundLocationConfiguration.priority
        )
        put(
          BackgroundLocationConfiguration::maxUpdateAgeMillis.name,
          backgroundLocationConfiguration.maxUpdateAgeMillis
        )
        put(
          BackgroundLocationConfiguration::durationMillis.name,
          backgroundLocationConfiguration.durationMillis
        )
        put(
          BackgroundLocationConfiguration::granularity.name,
          backgroundLocationConfiguration.granularity
        )
      }

      val sharedPrefs = reactApplicationContext.getSharedPreferences(
        Configs.BACKGROUND_LOCATION_CONFIG_SHARED_PREFERENCES_DATABASE,
        Context.MODE_PRIVATE
      )

      sharedPrefs.edit {
        putString(
          Constants.BACKGROUND_LOCATION_WORKER_CONFIGURATION_KEY,
          backgroundLocationConfigsJson.toString()
        )
      }

      val backgroundLocationInputData = Data.Builder().putString(
        Constants.BACKGROUND_LOCATION_WORKER_CONFIGURATION_KEY,
        backgroundLocationConfigsJson.toString()
      ).build();

//      // Temporarily replace PeriodicWork with a OneTimeWork request
//      val testWorkRequest = OneTimeWorkRequestBuilder<BackgroundLocationWorker>()
//        .setInputData(backgroundLocationInputData)
//        .setInitialDelay(10, TimeUnit.SECONDS) // Delays execution for 10 seconds
//        .build()
//
//      WorkManager.getInstance(reactApplicationContext).enqueue(testWorkRequest)

      val locationWorkRequest = PeriodicWorkRequestBuilder<BackgroundLocationWorker>(
        15, TimeUnit.MINUTES // 15 Min minimum enforced by Android OS
      ).setInputData(backgroundLocationInputData).build()

      workManager.enqueueUniquePeriodicWork(
        BackgroundLocationWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP, // Do not override if already tracking
        locationWorkRequest
      )

      Log.i(Constants.LOG_TAG, "Started Background Location Service")

      WorkManager.getInstance(reactApplicationContext)
        .getWorkInfosForUniqueWork(BackgroundLocationWorker.WORK_NAME).apply {
          addListener({
            Log.i(
              Constants.LOG_TAG, "Start Background Location Service: ID: ${get()}"
            );
          }, ContextCompat.getMainExecutor(reactApplicationContext))
        }
    } catch (e: Exception) {
      Log.e(Constants.LOG_TAG, "Start Background Location Service Error: {}", e)
    }
  }

  override fun stopLocationBackgroundService() {
    workManager.cancelUniqueWork(BackgroundLocationWorker.WORK_NAME)
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun isLocationMocked(promise: Promise) {
    rnPackLocation.getFreshCurrentLocation(
      reactApplicationContext,
      Priority.PRIORITY_BALANCED_POWER_ACCURACY,
      onSuccess = { location ->
        val isMocked: Boolean = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider
        promise.resolve(isMocked)
      },
      onError = { exception ->
        promise.reject(exception)
      })
  }

  companion object {
    const val NAME = NativeRNPackLocationSpec.NAME
  }
}
