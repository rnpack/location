import { NitroModules } from 'react-native-nitro-modules';
import type { RNPackLocation } from './RNPackLocation.nitro';

const RNPackLocationHybridObject =
  NitroModules.createHybridObject<RNPackLocation>('RNPackLocation');

export function multiply(a: number, b: number): number {
  return RNPackLocationHybridObject.multiply(a, b);
}

export * from './helpers';
export * from './hooks';
