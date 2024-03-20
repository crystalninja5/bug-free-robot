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
package com.uber.sdk2.auth.api.sso

/**
 * Represents the Single Sign-On (SSO) link for authentication. This class is used to start the SSO
 * flow
 */
interface SsoLink {
  /** Executes the SSO link with the given optional query parameters. */
  suspend fun execute(optionalQueryParams: Map<String, String>): String

  fun handleAuthCode(authCode: String)
}
