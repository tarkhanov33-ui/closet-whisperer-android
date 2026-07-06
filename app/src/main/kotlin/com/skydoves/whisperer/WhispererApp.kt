/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.whisperer

import android.app.Application
import com.example.remoteuisdk.RemoteUiSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WhispererApp : Application() {

  override fun onCreate() {
    super.onCreate()
    RemoteUiSdk.init(
      context = this,
      apiKey = "\$2a\$10\$LF0pSka3erQsQ1hy.oqh4./30.Y/INUoNWDVujolN0lqUbBksvnFu",
      baseUrl = "https://api.jsonbin.io/v3/b/6a4abc0af5f4af5e296397c9"
    )
  }
}
