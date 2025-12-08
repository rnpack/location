import { useEffect, useRef } from 'react';
import {
  check,
  PERMISSIONS,
  // RESULTS,
  // request
} from 'react-native-permissions';

import type { RefObject } from 'react';
import type { PermissionStatus } from 'react-native-permissions';
import { Platform } from 'react-native';

interface UseLocationAuthorizationReturns {
  configureLocation: () => Promise<void>;
  getLocationAuthorizationStatus: () => Promise<PermissionStatus>;
  startLocationAuthorizationUpdateListener: () => void;
  stopLocationAuthorizationUpdateListener: () => void;
}

interface UseLocationAuthorizationProps {
  onLocationStateChange: (status: PermissionStatus) => void;
}

function useLocationAuthorization(
  props: UseLocationAuthorizationProps
): UseLocationAuthorizationReturns {
  const locationAuthorizationListenerRef: RefObject<
    NodeJS.Timeout | undefined
  > = useRef<NodeJS.Timeout>(undefined);

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

  async function configureLocation(): Promise<void> {}

  async function isLocationAuthorized(): Promise<PermissionStatus> {
    let permissionResult;

    if (Platform.OS === 'ios') {
      permissionResult = await check(PERMISSIONS.IOS.LOCATION_WHEN_IN_USE);
    } else {
      permissionResult = await check(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
    }

    return permissionResult;
  }

  async function getLocationAuthorizationStatus(): Promise<PermissionStatus> {
    const authorization = await isLocationAuthorized();

    return authorization;
  }

  function startLocationAuthorizationUpdateListener() {
    locationAuthorizationListenerRef.current = setInterval(async () => {
      const authorization = await isLocationAuthorized();
      onLocationStateChange(authorization);
    }, 3000);
  }

  function stopLocationAuthorizationUpdateListener() {
    if (locationAuthorizationListenerRef?.current) {
      clearInterval(locationAuthorizationListenerRef.current);
    }
  }

  function onLocationStateChange(status: PermissionStatus): void {
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
