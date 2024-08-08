import { Fragment, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Platform } from 'react-native';
import { Dialog } from 'react-native-design';
import type { DialogProps } from 'react-native-design';
import type {
  GeolocationConfiguration,
  GeolocationError,
  GeolocationResponse,
} from '@react-native-community/geolocation';
import type { LocationPermissionStatus } from 'react-native-location';
import { openAppCustomSettings, openLocationSettings } from '@rnpack/utils';

import {
  useLocation,
  useLocationListener,
  useLocationMode,
  useLocationAuthorization,
} from '../hooks';
import type { onRequestLocationAuthorizationErrorArgs } from './../hooks';

const LISTENER_TIMER_INTERVAL: number = 3000;

interface LocationHelperProps {
  locationConfig: GeolocationConfiguration;
  onLocationOff?: () => void;
  onLocationChange?: (location: GeolocationResponse) => void;
  onPressAcceptLocationAuthorization?: () => void;
  onPressRejectLocationAuthorization?: () => void;
  locationAuthorizationAcceptText?: string;
  locationAuthorizationRejectText?: string;
  locationAuthorizationTitle?: string;
  locationAuthorizationContent?: ReactNode;
  locationAuthorizationDialogProps?: DialogProps;
  onLocationAuthorizationChange?: (isAuthorized: boolean) => void;
  onPressAcceptEnableLocation?: () => void;
  onPressRejectEnableLocation?: () => void;
  enableLocationAcceptText?: string;
  enableLocationRejectText?: string;
  enableLocationTitle?: string;
  enableLocationContent?: ReactNode;
  enableLocationDialogProps?: DialogProps;
  onLocationAdapterStateChange?: (isEnabled: boolean) => void;
  timeInterval?: number;
}

function LocationHelper(props: LocationHelperProps) {
  const {
    getCurrentLocation,
    requestLocationAuthorization,
    checkLocationAuthorization,
  } = useLocation({
    config: props?.locationConfig,
    onGetCurrentLocationSuccess,
    onGetCurrentLocationError,
    onRequestLocationAuthorizationSucces,
    onRequestLocationAuthorizationError,
  });
  const { startLocationChangeListener } = useLocationListener({
    onLocationChange,
    onLocationWatchError,
  });
  const { getLocationAuthorizationStatus } = useLocationAuthorization({
    onLocationStateChange,
  });
  const { getLocationMode } = useLocationMode({
    onLocationModeChange,
    onLocationPowerStateChange,
  });

  const locationStateChangeTimerInterval = useRef<NodeJS.Timeout>();
  const locationModeChangeTimerInterval = useRef<NodeJS.Timeout>();
  const locationUpdateTimerInterval = useRef<NodeJS.Timeout>();

  const [isAuthorized, setIsAuthorized] = useState<boolean>(true);
  const [isEnabled, setIsEnabled] = useState<boolean>(true);

  useEffect(() => {
    mount();

    return () => {
      unmount();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!isEnabled || !isAuthorized) {
      props?.onLocationOff?.();
    }

    if (!isAuthorized) {
      // startLocationStateChangeListener();
    }

    if (isAuthorized) {
      // stopLocationStateChangeListener();
    }

    if (!isEnabled) {
      // startLocationModeChangeListener();
    }

    if (isEnabled) {
      // stopLocationModeChangeListener();
    }

    props?.onLocationAuthorizationChange?.(isAuthorized);
    props?.onLocationAdapterStateChange?.(isEnabled);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthorized, isEnabled]);

  async function mount() {
    startLocationStateChangeListener();
    startLocationModeChangeListener();

    if (props?.timeInterval) {
      startLocationUpdateListener();
    }

    const isGranted: boolean = await checkLocationAuthorization();

    if (!isGranted) {
      requestLocationAuthorization();
    }

    getCurrentLocation();

    startLocationChangeListener();
  }

  async function unmount() {
    stopLocationStateChangeListener();
    stopLocationModeChangeListener();
    stopLocationUpdateListener();
  }

  function onLocationModeChange(_isEnabled: boolean): void {
    setIsEnabled(_isEnabled);
  }

  function onLocationPowerStateChange(_isEnabled: boolean): void {
    setIsEnabled(_isEnabled);
  }

  async function processLocationAuthorizationState() {
    const status: LocationPermissionStatus =
      await getLocationAuthorizationStatus();

    onLocationStateChange(status);
  }

  async function startLocationStateChangeListener() {
    locationStateChangeTimerInterval.current = setInterval(() => {
      processLocationAuthorizationState();
    }, LISTENER_TIMER_INTERVAL);
  }

  function stopLocationStateChangeListener() {
    clearInterval(locationStateChangeTimerInterval?.current);
  }

  async function processLocationMode() {
    const _isEnabled: boolean = await getLocationMode();

    onLocationPowerStateChange(_isEnabled);
  }

  async function startLocationModeChangeListener() {
    locationModeChangeTimerInterval.current = setInterval(() => {
      processLocationMode();
    }, LISTENER_TIMER_INTERVAL);
  }

  function stopLocationModeChangeListener() {
    if (locationModeChangeTimerInterval?.current) {
      clearInterval(locationModeChangeTimerInterval?.current);
    }
  }

  async function onPressAcceptAuthorization(): Promise<void> {
    if (props?.onPressAcceptLocationAuthorization) {
      props?.onPressAcceptLocationAuthorization();

      return;
    }

    if (Platform?.OS === 'ios') {
      const authorization: LocationPermissionStatus =
        await getLocationAuthorizationStatus();

      if (authorization === 'notDetermined') {
        requestLocationAuthorization();
        return;
      }

      await openAppCustomSettings();
    }

    if (Platform?.OS === 'android') {
      await openAppCustomSettings();
    }
  }

  async function onPressRejectAuthorization(): Promise<void> {
    if (props?.onPressRejectLocationAuthorization) {
      props?.onPressRejectLocationAuthorization();

      return;
    }
  }

  async function onPressAcceptEnable(): Promise<void> {
    if (props?.onPressAcceptEnableLocation) {
      props?.onPressAcceptEnableLocation();

      return;
    }

    if (Platform?.OS === 'ios') {
      await openLocationSettings();
    }

    if (Platform?.OS === 'android') {
      await openLocationSettings();
    }
  }

  async function onPressRejectEnable(): Promise<void> {
    if (props?.onPressRejectEnableLocation) {
      props?.onPressRejectEnableLocation();

      return;
    }
  }

  function onGetCurrentLocationSuccess(position: GeolocationResponse) {
    props?.onLocationChange?.(position);
  }

  function onGetCurrentLocationError(error: GeolocationError) {
    console.error('On get current location Error: ', error?.message);
  }

  function onRequestLocationAuthorizationSucces(success: boolean) {
    console.log('onRequestLocationAuthorizationSucces: ', success);
  }

  function onRequestLocationAuthorizationError(
    args: onRequestLocationAuthorizationErrorArgs
  ) {
    console.error('On request location authorization Error: ', args);
  }

  function onLocationChange(position: GeolocationResponse) {
    props?.onLocationChange?.(position);
  }

  function onLocationWatchError(error: GeolocationError) {
    console.error('On location watch Error: ', error?.message);
  }

  function onLocationStateChange(status: LocationPermissionStatus) {
    switch (status) {
      case 'authorizedAlways':
        setIsAuthorized(true);
        break;

      case 'authorizedWhenInUse':
        setIsAuthorized(true);
        break;

      case 'authorizedFine':
        setIsAuthorized(true);
        break;

      case 'authorizedCoarse':
        setIsAuthorized(true);
        break;

      case 'denied':
        setIsAuthorized(false);
        break;

      case 'restricted':
        setIsAuthorized(false);
        break;

      case 'notDetermined':
        setIsAuthorized(false);
        break;

      default:
        console.warn('Location authorization out of scope: ', { status });
        break;
    }
  }

  function startLocationUpdateListener() {
    if (props?.timeInterval) {
      locationUpdateTimerInterval.current = setInterval(() => {
        getCurrentLocation();
      }, props?.timeInterval);
    }
  }

  function stopLocationUpdateListener() {
    if (locationUpdateTimerInterval?.current) {
      clearInterval(locationUpdateTimerInterval?.current);
    }
  }

  return (
    <Fragment>
      <Dialog
        isVisible={!isAuthorized}
        title={props?.locationAuthorizationTitle}
        onPressAccept={onPressAcceptAuthorization}
        onPressReject={onPressRejectAuthorization}
        acceptText={props?.locationAuthorizationAcceptText}
        rejectText={props?.locationAuthorizationRejectText}
        transparent
        hideReject
        hideClose
        {...props?.locationAuthorizationDialogProps}
      >
        {props?.locationAuthorizationContent}
      </Dialog>
      <Dialog
        isVisible={!isEnabled}
        title={props?.enableLocationTitle}
        onPressAccept={onPressAcceptEnable}
        onPressReject={onPressRejectEnable}
        acceptText={props?.enableLocationAcceptText}
        rejectText={props?.enableLocationRejectText}
        transparent
        hideReject
        hideClose
        {...props?.enableLocationDialogProps}
      >
        {props?.enableLocationContent}
      </Dialog>
    </Fragment>
  );
}

export type { LocationHelperProps };
export { LocationHelper };
