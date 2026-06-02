import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';

import ForegroundLocationServiceTask from './src/ForegroundLocationServiceTask';
import BackgroundLocationServiceTask from './src/BackgroundLocationServiceTask';

AppRegistry.registerComponent(appName, () => App);

AppRegistry.registerHeadlessTask(
  'RN_PACK_FOREGROUND_LOCATION_HEADLESS_JS_TASK_SERVICE',
  () => ForegroundLocationServiceTask
);
AppRegistry.registerHeadlessTask(
  'RN_PACK_BACKGROUND_LOCATION_HEADLESS_JS_TASK_SERVICE',
  () => BackgroundLocationServiceTask
);

if (typeof document !== 'undefined') {
  AppRegistry.runApplication(appName, {
    rootTag: document.getElementById('root'),
  });
}
