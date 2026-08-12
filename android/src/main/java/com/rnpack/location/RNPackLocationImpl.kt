package com.rnpack.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.rnpack.location.data.LocationAccessPermissionResult
import com.rnpack.location.data.LocationConfiguration
import com.rnpack.location.data.LocationProvider
import java.util.concurrent.Executor
import java.util.function.Consumer
import androidx.core.util.Consumer as ConsumerUtil
import com.google.android.gms.location.LocationRequest as LocationRequestGMS


class RNPackLocationImpl : RNPackLocation {

  var cancellationTokenSource = CancellationTokenSource()
  var locationProvidersChangeSubscriptionsCount = 0
  var locationChangeSubscriptionsCount = 0
  var appPermissionRequestCode = Constants.LOCATION_PERMISSION_REQ_CODE

  val onRequestPermissionsResultListener = object : PermissionListener {
    override fun onRequestPermissionsResult(
      requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ): Boolean {

      Log.d(
        Constants.LOG_TAG, "onRequestPermissionsResultListener calling...requestCode: $requestCode"
      )

      if (requestCode == appPermissionRequestCode) {

        for (i in permissions.indices) {
          val permission = permissions[i]
          val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED

          when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> Log.d(
              Constants.LOG_TAG, "fine " + if (isGranted) "granted" else "denied"
            )

            Manifest.permission.ACCESS_COARSE_LOCATION -> Log.d(
              Constants.LOG_TAG, "coarse " + if (isGranted) "granted" else "denied"
            )

            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> Log.d(
              Constants.LOG_TAG, "background " + if (isGranted) "granted" else "denied"
            )
          }
        }
        return true
      }
      return false
    }
  }

  override fun requestAccessLocationPermission(
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ) {
    appPermissionRequestCode = permissionRequestCode

    permissionAwareActivity.requestPermissions(
      permissions, permissionRequestCode, onRequestPermissionsResultListener
    )
  }

  override fun requestBackgroundLocationPermission(
    reactApplicationContext: ReactApplicationContext,
    permissionAwareActivity: PermissionAwareActivity,
    permissions: Array<String>,
    permissionRequestCode: Int
  ) {

    val result = isLocationAuthorized(reactApplicationContext)

    if (!result.fine) {
      return
    }

    appPermissionRequestCode = permissionRequestCode

    permissionAwareActivity.requestPermissions(
      permissions, permissionRequestCode, onRequestPermissionsResultListener
    )
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun isLocationEnabledApi28(reactApplicationContext: ReactApplicationContext): Boolean {
    val locationManager: LocationManager =
      reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isOn: Boolean = locationManager.isLocationEnabled

    return isOn
  }

  override fun isLocationEnabledApi27(reactApplicationContext: ReactApplicationContext): Boolean {
    val locationManager: LocationManager =
      reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isOn: Boolean =
      locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
      )

    return isOn
  }

  override fun isLocationEnabled(reactApplicationContext: ReactApplicationContext): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      isLocationEnabledApi28(reactApplicationContext)
    } else {
      isLocationEnabledApi27(reactApplicationContext)
    }
  }

  override fun isLocationAuthorizedCoarse(reactApplicationContext: ReactApplicationContext): Boolean {
    return ContextCompat.checkSelfPermission(
      reactApplicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun isLocationAuthorizedFine(reactApplicationContext: ReactApplicationContext): Boolean {
    return ContextCompat.checkSelfPermission(
      reactApplicationContext, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun isLocationAuthorized(reactApplicationContext: ReactApplicationContext): LocationAccessPermissionResult {
    val isFine: Boolean = isLocationAuthorizedFine(reactApplicationContext)
    val isCoarse: Boolean = isLocationAuthorizedCoarse(reactApplicationContext)

    return LocationAccessPermissionResult(
      status = isFine || isCoarse, fine = isFine, coarse = isCoarse
    )
  }

  override fun subscribeToLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  ) {

    Log.d(
      Constants.LOG_TAG,
      "subscribeToLocationProvidersChange calling...$locationProvidersChangeSubscriptionsCount"
    )

    locationProvidersChangeSubscriptionsCount++

    if (locationProvidersChangeSubscriptionsCount <= 1) {
      val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)

      reactApplicationContext.registerReceiver(locationProvidersChangeReceiver, intentFilter)
    }
  }

  override fun unsubscribeFromLocationProvidersChange(
    reactApplicationContext: ReactApplicationContext,
    locationProvidersChangeReceiver: BroadcastReceiver
  ) {
    Log.d(Constants.LOG_TAG, "unsubscribeFromLocationProvidersChange calling...")

    if (locationProvidersChangeSubscriptionsCount > 1) {
      locationProvidersChangeSubscriptionsCount--
    }

    if (locationProvidersChangeSubscriptionsCount <= 0) {
      try {
        reactApplicationContext.unregisterReceiver(locationProvidersChangeReceiver)
      } catch (e: Exception) {
        Log.d(Constants.LOG_TAG, "unsubscribeFromLocationProvidersChange: " + e.message)
      }
    }
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getLastLocation(
    reactApplicationContext: ReactApplicationContext,
    locationConfig: LocationConfiguration,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {
    val isAuthorized = isLocationAuthorized(reactApplicationContext)

    if (!isAuthorized.coarse && !isAuthorized.fine) {
      onError(SecurityException("Location permission not granted"))
    }

    if (locationConfig.provider == LocationProvider.FUSED.value) {

      val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(reactApplicationContext)

      fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
          onSuccess(location)
        } else {
          onError(NullPointerException("Last Location was null"))
        }
      }.addOnFailureListener { exception ->
        onError(exception)
      }
    } else {

      val locationManager: LocationManager = reactApplicationContext.getSystemService(
        Context.LOCATION_SERVICE
      ) as LocationManager

      var lastLocation: Location? = null

      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      }

      if (lastLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      }

      if (lastLocation == null) {
        onError(NullPointerException("Last Location was null"))
      } else {
        onSuccess(lastLocation)
      }
    }
  }

  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun getFreshCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    locationConfig: LocationConfiguration,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {

    cancelLocationRequest()

    if (locationConfig.provider == LocationProvider.FUSED.value) {
      val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(reactApplicationContext)

      fusedLocationProviderClient.getCurrentLocation(
        priority, cancellationTokenSource.token
      ).addOnSuccessListener { location ->
        if (location != null) {
          onSuccess(location)
        } else {
          onError(NullPointerException("Location was null"))
        }
      }.addOnFailureListener { exception ->
        onError(exception)
      }
    } else {
      val locationManager: LocationManager =
        reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

      val locationCancellationSignal = CancellationSignal()
      val locationExecutor: Executor = ContextCompat.getMainExecutor(reactApplicationContext)


      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
        val locationConsumer: Consumer<Location?> = Consumer { location: Location? ->
          if (location == null) {
            onError(NullPointerException("Location was null"))
          } else {
            onSuccess(location)
          }
        }

        val locationRequest: LocationRequest =
          LocationRequest.Builder(locationConfig.intervalMillis).apply {
            setDurationMillis(locationConfig.durationMillis)
            setIntervalMillis(locationConfig.intervalMillis)
            setMinUpdateIntervalMillis(locationConfig.minUpdateIntervalMillis)
            setMaxUpdateDelayMillis(locationConfig.maxUpdateDelayMillis)
            setMinUpdateDistanceMeters(locationConfig.minUpdateDistanceMeters)
            setQuality(priority)
          }.build()

        if (!locationManager.isProviderEnabled(
            locationConfig.provider ?: LocationProvider.GPS.value
          )
        ) {
          onError(Exception("Provider not enabled: ${locationConfig.provider ?: LocationProvider.GPS.value}"))
        }

        locationManager.getCurrentLocation(
          locationConfig.provider ?: LocationProvider.GPS.value,
          locationRequest,
          locationCancellationSignal,
          locationExecutor,
          locationConsumer
        )
      } else {

        val locationConsumer: ConsumerUtil<Location> = ConsumerUtil { location: Location? ->
          if (location == null) {
            onError(NullPointerException("Location was null"))
          } else {
            onSuccess(location)
          }
        }

        LocationManagerCompat.getCurrentLocation(
          locationManager,
          locationConfig.provider ?: LocationProvider.GPS.value,
          locationCancellationSignal,
          locationExecutor,
          locationConsumer
        )
      }
    }
  }

  @RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
  )
  override fun getCurrentLocation(
    reactApplicationContext: ReactApplicationContext,
    priority: Int,
    locationConfig: LocationConfiguration,
    onSuccess: (Location) -> Unit,
    onError: (Exception) -> Unit
  ) {
    getLastLocation(reactApplicationContext, locationConfig, onSuccess = { lastLocation ->
      val isFresh =
        System.currentTimeMillis() - lastLocation.time < locationConfig.maxUpdateAgeMillis

      if (isFresh) {
        onSuccess(lastLocation)
      } else {
        getFreshCurrentLocation(
          reactApplicationContext, priority, locationConfig, onSuccess, onError
        )
      }
    }, onError = {
      getFreshCurrentLocation(
        reactApplicationContext, priority, locationConfig, onSuccess, onError
      )
    })
  }

  override fun cancelLocationRequest() {
    cancellationTokenSource.cancel()
    cancellationTokenSource = CancellationTokenSource()
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  override fun subscribeToLocationChange(
    reactApplicationContext: ReactApplicationContext,
    locationConfig: LocationConfiguration,
    onResult: (Location) -> Unit,
    locationRequest: LocationRequestGMS,
    locationCallback: LocationCallback
  ) {
    locationChangeSubscriptionsCount++

    if (locationChangeSubscriptionsCount <= 1) {
      if (locationConfig.provider == LocationProvider.FUSED.value) {

        val fusedLocationProviderClient: FusedLocationProviderClient =
          LocationServices.getFusedLocationProviderClient(reactApplicationContext)

        fusedLocationProviderClient.requestLocationUpdates(
          locationRequest, locationCallback, Looper.getMainLooper()
        )
      } else {
        val locationManager: LocationManager =
          reactApplicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationExecutor: Executor = ContextCompat.getMainExecutor(reactApplicationContext)

        val locationListener = LocationListener { location ->
          onResult(location)
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
          val locationRequest: LocationRequest =
            LocationRequest.Builder(locationConfig.intervalMillis).apply {
              setDurationMillis(locationConfig.durationMillis)
              setIntervalMillis(locationConfig.intervalMillis)
              setMinUpdateIntervalMillis(locationConfig.minUpdateIntervalMillis)
              setMaxUpdateDelayMillis(locationConfig.maxUpdateDelayMillis)
              setMinUpdateDistanceMeters(locationConfig.minUpdateDistanceMeters)
              setQuality(locationConfig.priority)
            }.build()

          locationManager.requestLocationUpdates(
            locationConfig.provider ?: LocationProvider.GPS.value,
            locationRequest,
            locationExecutor,
            locationListener
          )
        } else {
          locationManager.requestLocationUpdates(
            locationConfig.provider ?: LocationProvider.GPS.value,
            locationConfig.minUpdateIntervalMillis,
            locationConfig.minUpdateDistanceMeters,
            locationListener
          )
        }
      }
    }
  }

  override fun unsubscribeFromLocationChange(
    reactApplicationContext: ReactApplicationContext, locationCallback: LocationCallback
  ) {
    if (locationChangeSubscriptionsCount > 1) {
      locationChangeSubscriptionsCount--
    }

    if (locationChangeSubscriptionsCount <= 0) {

      val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient((reactApplicationContext))

      fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
  }

  override fun invalidateAllSubscriptions(reactApplicationContext: ReactApplicationContext) {
    locationChangeSubscriptionsCount = 0
  }
}
