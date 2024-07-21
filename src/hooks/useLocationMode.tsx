import { useEffect, useRef } from 'react';
import SystemSetting from 'react-native-system-setting';

interface EmitterSubscription {
  remove: () => void;
}

interface UseLocationModeReturns {
  startLocationModeListener: () => Promise<void>;
  stopLocationModeListener: () => Promise<void>;
  startLocationPowerStateListener: () => Promise<void>;
  stopLocationPowerStateListener: () => Promise<void>;
  getLocationMode: () => Promise<boolean>;
}

interface UseLocationModeProps {
  onLocationModeChange?: (isEnabled: boolean) => void;
  onLocationPowerStateChange?: (isEnabled: boolean) => void;
}

function useLocationMode(props: UseLocationModeProps): UseLocationModeReturns {
  const locationModeListenerRef = useRef<EmitterSubscription | null>();
  const locationPowerStateListenerRef = useRef<EmitterSubscription | null>();

  useEffect(() => {
    mount();

    return () => {
      unmount();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function mount() {
    await startLocationModeListener();
    await startLocationPowerStateListener();
  }

  async function unmount() {
    await stopLocationModeListener();
    await stopLocationPowerStateListener();
  }

  async function startLocationModeListener(): Promise<void> {
    locationModeListenerRef.current =
      await SystemSetting?.addLocationModeListener((locationMode: number) => {
        props?.onLocationModeChange?.(Boolean(locationMode));
      });
  }

  async function stopLocationModeListener(): Promise<void> {
    if (locationModeListenerRef?.current) {
      SystemSetting?.removeListener(locationModeListenerRef?.current);
    }
  }

  async function startLocationPowerStateListener(): Promise<void> {
    locationPowerStateListenerRef.current =
      await SystemSetting?.addLocationListener((isEnabled: boolean) => {
        props?.onLocationPowerStateChange?.(isEnabled);
      });
  }

  async function stopLocationPowerStateListener(): Promise<void> {
    if (locationPowerStateListenerRef?.current) {
      SystemSetting?.removeListener(locationPowerStateListenerRef?.current);
    }
  }

  async function getLocationMode(): Promise<boolean> {
    const isEnabled: number = await SystemSetting?.getLocationMode();

    if (isEnabled) {
      return true;
    }

    return false;
  }

  return {
    startLocationModeListener,
    stopLocationModeListener,
    startLocationPowerStateListener,
    stopLocationPowerStateListener,
    getLocationMode,
  };
}

export type { UseLocationModeProps, UseLocationModeReturns };
export { useLocationMode };
