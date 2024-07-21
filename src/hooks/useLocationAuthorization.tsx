import { useEffect, useRef } from 'react';
import type { MutableRefObject } from 'react';
import RNLocation from 'react-native-location';
import type {
  LocationPermissionStatus,
  Subscription,
} from 'react-native-location';

interface UseLocationAuthorizationReturns {
  configureLocation: () => Promise<void>;
  getLocationAuthorizationStatus: () => Promise<LocationPermissionStatus>;
  startLocationAuthorizationUpdateListener: () => void;
  stopLocationAuthorizationUpdateListener: () => void;
}

interface UseLocationAuthorizationProps {
  onLocationStateChange: (status: LocationPermissionStatus) => void;
}

function useLocationAuthorization(
  props: UseLocationAuthorizationProps
): UseLocationAuthorizationReturns {
  const locationAuthorizationListener: MutableRefObject<
    Subscription | undefined
  > = useRef<Subscription>();

  useEffect(() => {
    mount();

    return () => {
      unmount();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function unmount() {
    stopLocationAuthorizationUpdateListener();
  }

  async function mount() {
    await configureLocation();

    startLocationAuthorizationUpdateListener();
  }

  async function configureLocation(): Promise<void> {
    await RNLocation.configure({});
  }

  async function getLocationAuthorizationStatus(): Promise<LocationPermissionStatus> {
    const authorization = await RNLocation.getCurrentPermission();

    return authorization;
  }

  function startLocationAuthorizationUpdateListener() {
    locationAuthorizationListener.current =
      RNLocation?.subscribeToPermissionUpdates((authorization) => {
        onLocationStateChange(authorization);
      });
  }

  function stopLocationAuthorizationUpdateListener() {
    locationAuthorizationListener?.current?.();
  }

  function onLocationStateChange(status: LocationPermissionStatus): void {
    props?.onLocationStateChange(status);
  }

  return {
    configureLocation,
    getLocationAuthorizationStatus,
    startLocationAuthorizationUpdateListener,
    stopLocationAuthorizationUpdateListener,
  };
}

export { useLocationAuthorization };
