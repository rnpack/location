import type { LocationResponse } from '@rnpack/location';
import { LocationLogApi } from './constants';

let updatesCount = 0;

export default async (taskData: LocationResponse) => {
  console.log('Foreground Location Service Task Data: ', {
    taskData,
    updatesCount,
  });

  const response = await fetch(LocationLogApi, {
    method: 'POST',
    body: JSON.stringify({
      ...taskData,
      type: 'Foreground Location Service',
    }),
  });

  console.log('API response: ', response.status);

  updatesCount++;
};
