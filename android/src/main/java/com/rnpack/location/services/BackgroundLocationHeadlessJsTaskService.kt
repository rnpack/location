package com.rnpack.location.services

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.rnpack.location.Constants

class BackgroundLocationHeadlessJsTaskService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    return intent?.extras?.let {
      HeadlessJsTaskConfig(
        Constants.BACKGROUND_LOCATION_HEADLESS_JS_TASK_SERVICE_NAME,
        Arguments.fromBundle(it),
        0,
        true
      )
    }
  }
}
