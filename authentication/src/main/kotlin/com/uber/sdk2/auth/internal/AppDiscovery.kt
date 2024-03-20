/*
 * Copyright (C) 2024. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uber.sdk2.auth.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.uber.sdk2.auth.api.AppDiscovering

class AppDiscovery(val context: Context) : AppDiscovering {
  override fun findAppForSso(uri: Uri): Set<String> {
    val intent = Intent(Intent.ACTION_VIEW, uri)

    // Use PackageManager to find activities that can handle the Intent
    val packageManager = context.packageManager
    val appsList = packageManager.queryIntentActivities(intent, 0)

    // Extract the package names from the ResolveInfo objects and return them
    val packageNames = appsList.map { resolveInfo -> resolveInfo.activityInfo.packageName }

    return packageNames.toSet()
  }
}
