/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.consentTestApp;

import android.app.Application;
import android.util.Log;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.Consent;

import java.util.Arrays;

public class ConsentTestApplication extends Application {

	private static final String LOG_TAG = "ConsentTestApplication";

	// TODO: Set up the Environment File ID from your Launch property for the preferred environment
	private final String ENVIRONMENT_FILE_ID = "";

	@Override
	public void onCreate() {
		super.onCreate();
		MobileCore.setApplication(this);

		MobileCore.setLogLevel(LoggingMode.DEBUG);
		MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

		// register Adobe extensions
		MobileCore.registerExtensions(
				Arrays.asList(Consent.EXTENSION, Edge.EXTENSION, Assurance.EXTENSION),
				o -> Log.d(LOG_TAG, "Mobile SDK was initialized")
		);
	}
}
