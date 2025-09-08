package expo.modules.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import expo.modules.core.utilities.VRUtilities
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.location.records.LocationLastKnownOptions
import expo.modules.location.records.LocationOptions
import expo.modules.location.records.LocationResponse

class LocationHelpers(context: Context) {
  private val mLocationManager: LocationManager =
    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  fun requestSingleLocation(locationRequest: LocationRequest, promise: Promise) {
    try {
      val provider = when (locationRequest.priority) {
        LocationModule.ACCURACY_BEST_FOR_NAVIGATION, LocationModule.ACCURACY_HIGHEST, LocationModule.ACCURACY_HIGH -> {
          if (VRUtilities.isQuest()) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
        }

        LocationModule.ACCURACY_BALANCED, LocationModule.ACCURACY_LOW -> LocationManager.NETWORK_PROVIDER
        LocationModule.ACCURACY_LOWEST -> LocationManager.PASSIVE_PROVIDER
        else -> {
          if (VRUtilities.isQuest()) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
        }
      }

      if (!mLocationManager.isProviderEnabled(provider)) {
        promise.reject(CurrentLocationIsUnavailableException())
        return
      }

      val location = mLocationManager.getLastKnownLocation(provider)
      if (location == null) {
        promise.reject(CurrentLocationIsUnavailableException())
        return
      }

      promise.resolve(LocationResponse(location))
    } catch (e: SecurityException) {
      promise.reject(LocationRequestRejectedException(e))
    }
  }

  fun requestContinuousUpdates(locationModule: LocationModule, locationRequest: LocationRequest, watchId: Int, promise: Promise) {
    locationModule.requestLocationUpdates(
      locationRequest,
      watchId,
      object : LocationRequestCallbacks {
        override fun onLocationChanged(location: Location) {
          locationModule.sendLocationResponse(watchId, LocationResponse(location))
        }

        override fun onRequestSuccess() {
          promise.resolve(null)
        }

        override fun onRequestFailed(cause: CodedException) {
          promise.reject(cause)
        }
      }
    )
  }

  companion object {

    internal fun prepareLocationRequest(options: LocationOptions): LocationRequest {
      val locationParams = mapOptionsToLocationParams(options)

      return LocationRequest(
        interval = locationParams.interval,
        minUpdateIntervalMillis = locationParams.interval,
        maxUpdateDelayMillis = locationParams.interval,
        minUpdateDistanceMeters = locationParams.distance,
        priority = mapAccuracyToPriority(options.accuracy)
      )
    }

    internal fun prepareCurrentLocationRequest(options: LocationOptions): LocationRequest {
      val locationParams = mapOptionsToLocationParams(options)

      return LocationRequest(
        interval = locationParams.interval,
        minUpdateIntervalMillis = locationParams.interval,
        maxUpdateDelayMillis = locationParams.interval,
        minUpdateDistanceMeters = locationParams.distance,
        priority = mapAccuracyToPriority(options.accuracy)
      )
    }

    private fun mapOptionsToLocationParams(options: LocationOptions): LocationParams {
      val accuracy = options.accuracy
      val locationParams = buildLocationParamsForAccuracy(accuracy)

      options.timeInterval?.let {
        locationParams.interval = it
      }
      options.distanceInterval?.let {
        locationParams.distance = it.toFloat()
      }

      return locationParams
    }

    private fun mapAccuracyToPriority(accuracy: Int): Int {
      return when (accuracy) {
        LocationModule.ACCURACY_BEST_FOR_NAVIGATION, LocationModule.ACCURACY_HIGHEST, LocationModule.ACCURACY_HIGH -> LocationModule.ACCURACY_HIGHEST
        LocationModule.ACCURACY_BALANCED, LocationModule.ACCURACY_LOW -> LocationModule.ACCURACY_BALANCED
        LocationModule.ACCURACY_LOWEST -> LocationModule.ACCURACY_LOWEST
        else -> LocationModule.ACCURACY_BALANCED
      }
    }

    private fun buildLocationParamsForAccuracy(accuracy: Int): LocationParams {
      return when (accuracy) {
        LocationModule.ACCURACY_LOWEST -> LocationParams(accuracy = LocationAccuracy.LOWEST, distance = 3000f, interval = 10000)
        LocationModule.ACCURACY_LOW -> LocationParams(accuracy = LocationAccuracy.LOW, distance = 1000f, interval = 5000)
        LocationModule.ACCURACY_BALANCED -> LocationParams(accuracy = LocationAccuracy.MEDIUM, distance = 100f, interval = 3000)
        LocationModule.ACCURACY_HIGH -> LocationParams(accuracy = LocationAccuracy.HIGH, distance = 50f, interval = 2000)
        LocationModule.ACCURACY_HIGHEST -> LocationParams(accuracy = LocationAccuracy.HIGH, distance = 25f, interval = 1000)
        LocationModule.ACCURACY_BEST_FOR_NAVIGATION -> LocationParams(accuracy = LocationAccuracy.HIGH, distance = 0f, interval = 500)
        else -> LocationParams(accuracy = LocationAccuracy.MEDIUM, distance = 100f, interval = 3000)
      }
    }

    fun isAnyProviderAvailable(context: Context?): Boolean {
      val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
      return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
  }
}

/**
 * A singleton that keeps information about whether the app is in the foreground or not.
 * This is a simple solution for passing current foreground information from the LocationModule to LocationTaskConsumer.
 */
object AppForegroundedSingleton {
  var isForegrounded = false
}
