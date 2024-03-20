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
package com.uber.sdk2.auth.internal.sso

import androidx.appcompat.app.AppCompatActivity
import com.uber.sdk2.auth.AuthActivity
import com.uber.sdk2.auth.RobolectricTestBase
import com.uber.sdk2.auth.api.AppDiscovering
import com.uber.sdk2.auth.api.request.AuthContext
import com.uber.sdk2.auth.api.request.AuthDestination
import com.uber.sdk2.auth.api.request.AuthType
import com.uber.sdk2.auth.api.request.CrossApp
import com.uber.sdk2.auth.api.request.PrefillInfo
import com.uber.sdk2.auth.api.request.SsoConfig
import com.uber.sdk2.auth.api.sso.CustomTabsLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

class UniversalSsoLinkTest : RobolectricTestBase() {
  private val activity: AppCompatActivity =
    Robolectric.buildActivity(AuthActivity::class.java).get()
  private val ssoConfig: SsoConfig =
    SsoConfig(clientId = "clientId", redirectUri = "redirectUri", scope = "scope")
  private val authContext: AuthContext =
    AuthContext(
      authDestination = AuthDestination.CrossAppSso(listOf(CrossApp.Rider)),
      authType = AuthType.AuthCode,
      prefillInfo =
        PrefillInfo(
          email = "email",
          firstName = "firstName",
          lastName = "lastName",
          phoneNumber = "phoneNumber",
        ),
    )
  private val appDiscovering: AppDiscovering = mock()

  private val customTabsLauncher: CustomTabsLauncher = mock()

  private val universalSsoLink =
    UniversalSsoLink(
      activity = activity,
      ssoConfig = ssoConfig,
      authContext = authContext,
      appDiscovering = appDiscovering,
      customTabsLauncher = customTabsLauncher,
    )

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    whenever(appDiscovering.findAppForSso(any())).thenReturn(CrossApp.Rider.packages.toSet())
  }

  @Test
  fun `execute should return a string`() =
    runTest(testDispatcher) {
      universalSsoLink.resultDeferred.complete("SuccessResult")

      // Simulate calling execute and handle outcomes.
      val result = universalSsoLink.execute(mapOf("param1" to "value1"))

      assertNotNull(result)
      assertEquals("SuccessResult", result)
    }

  @Test
  fun `execute should fail`() =
    runTest(testDispatcher) {
      universalSsoLink.resultDeferred.completeExceptionally(Exception("Failed"))

      // Simulate calling execute and handle outcomes.
      try {
        universalSsoLink.execute(mapOf("param1" to "value1"))
      } catch (e: Exception) {
        assertEquals("Failed", e.message)
      }
    }

  @Test
  fun `execute should use inApp authentication when apps are not present`() =
    runTest(testDispatcher) {
      whenever(appDiscovering.findAppForSso(any())).thenReturn(emptySet())

      doNothing().whenever(customTabsLauncher).launch(any())
      universalSsoLink.resultDeferred.complete("SuccessResult")

      // Simulate calling execute and handle outcomes.
      val result = universalSsoLink.execute(mapOf("param1" to "value1"))

      verify(customTabsLauncher).launch(any())
    }
}
