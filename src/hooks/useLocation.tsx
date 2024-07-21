import { useEffect } from 'react';
import Geolocation from '@react-native-community/geolocation';
import type {
  GeolocationConfiguration,
  GeolocationError,
  GeolocationOptions,
  GeolocationResponse,
} from '@react-native-community/geolocation';
import { hasLocationAuthorized } from '@rnpack/utils';

interface GetCurrentLocationArgs {
  options?: GeolocationOptions;
}

interface onRequestLocationAuthorizationErrorArgs {
  success: false;
  error: GeolocationError;
}

interface UseLocationReturns {
  getCurrentLocation: (args?: GetCurrentLocationArgs) => void;
  requestLocationAuthorization: () => void;
  checkLocationAuthorization: () => Promise<boolean>;
}

interface UseLocationProps {
  config: GeolocationConfiguration;
  onGetCurrentLocationSuccess?: (position: GeolocationResponse) => void;
  onGetCurrentLocationError?: (error: GeolocationError) => void;
  onRequestLocationAuthorizationSucces?: (success: boolean) => void;
  onRequestLocationAuthorizationError?: (
    args: onRequestLocationAuthorizationErrorArgs
  ) => void;
}

function useLocation(props: UseLocationProps): UseLocationReturns {
  useEffect(() => {
    Geolocation?.setRNConfiguration(props?.config);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onGetCurrentLocationSuccess(position: GeolocationResponse): void {
    props?.onGetCurrentLocationSuccess?.(position);
  }

  function onGetCurrentLocationError(error: GeolocationError): void {
    console.error('Get current location Error: ', error?.message);

    props?.onGetCurrentLocationError?.(error);
  }

  function getCurrentLocation(args?: GetCurrentLocationArgs) {
    Geolocation?.getCurrentPosition(
      onGetCurrentLocationSuccess,
      onGetCurrentLocationError,
      args?.options
    );
  }

  function onLocationAuthorizationSuccess() {
    props?.onRequestLocationAuthorizationSucces?.(true);
  }

  function onLocationAuthorizationError(error: GeolocationError) {
    console.error('Request location authorization Error: ', error?.message);

    props?.onRequestLocationAuthorizationError?.({ success: false, error });
  }

  function requestLocationAuthorization() {
    Geolocation?.requestAuthorization(
      onLocationAuthorizationSuccess,
      onLocationAuthorizationError
    );
  }

  async function checkLocationAuthorization(): Promise<boolean> {
    const isGranted: boolean = await hasLocationAuthorized();

    return isGranted;
  }

  return {
    getCurrentLocation,
    requestLocationAuthorization,
    checkLocationAuthorization,
  };
}

export type {
  GetCurrentLocationArgs,
  UseLocationProps,
  UseLocationReturns,
  onRequestLocationAuthorizationErrorArgs,
};
export { useLocation };
