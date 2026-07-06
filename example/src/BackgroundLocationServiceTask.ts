import type { LocationResponse } from '@rnpack/location';
import { ForegroundLocationLogApi } from './constants';

let updatesCount = 0;

export default async (taskData: LocationResponse) => {
  console.log('Background Location Service Task Data: ', {
    taskData,
    updatesCount,
  });

  const response = await fetch(ForegroundLocationLogApi, {
    method: 'POST',
    body: JSON.stringify({
      ...taskData,
      type: 'Background Location Service',
    }),
  });

  console.log('API response: ', response.status);

  updatesCount++;
};
