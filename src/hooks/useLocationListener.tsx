import { useEffect, useRef } from 'react';
import Geolocation from '@react-native-community/geolocation';
import type {
  GeolocationError,
  GeolocationOptions,
  GeolocationResponse,
} from '@react-native-community/geolocation';

interface StartLocationListenerArgs {
  options?: GeolocationOptions;
}

interface UseLocationListenerReturnType {
  startLocationChangeListener: (args?: StartLocationListenerArgs) => void;
  stopLocationChangeListener: () => void;
}

interface UseLocationListenerProps {
  onLocationChange?: (position: GeolocationResponse) => void;
  onLocationWatchError?: (error: GeolocationError) => void;
}

function useLocationListener(
  props?: UseLocationListenerProps
): UseLocationListenerReturnType {
  const locationWatchId = useRef<number>();

  useEffect(() => {
    return () => {
      unmount();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function unmount(): void {
    stopLocationChangeListener();
  }

  function onLocationChange(position: GeolocationResponse) {
    props?.onLocationChange?.(position);
  }

  function onLocationWatchError(error: GeolocationError) {
    console.error('Location watch Error: ', error?.message);

    props?.onLocationWatchError?.(error);
  }

  function startLocationChangeListener(args?: StartLocationListenerArgs) {
    locationWatchId.current = Geolocation?.watchPosition(
      onLocationChange,
      onLocationWatchError,
      args?.options
    );
  }

  function stopLocationChangeListener() {
    if (locationWatchId?.current) {
      Geolocation?.clearWatch(locationWatchId?.current);
    }
  }

  return { startLocationChangeListener, stopLocationChangeListener };
}

export { useLocationListener };
