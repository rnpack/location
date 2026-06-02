package com.rnpack.location.services

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.rnpack.location.Constants

class ForegroundLocationHeadlessJsTaskService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    return intent?.extras?.let {
      HeadlessJsTaskConfig(
        intent.extras?.getString("taskKey")
          ?: Constants.FOREGROUND_LOCATION_HEADLESS_JS_TASK_SERVICE_NAME,
        Arguments.fromBundle(it),
        0,
        true
      )
    }
  }
}
