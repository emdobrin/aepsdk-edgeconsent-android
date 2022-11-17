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

package com.adobe.marketing.mobile.edge.consent;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import java.util.Map;

public class Consent {

	private static final String CLASS_NAME = "Consent";

	private Consent() {}

	/**
	 * Returns the version of the {@code Consent} extension
	 *
	 * @return The version as {@code String}
	 */
	public static String extensionVersion() {
		return ConsentConstants.EXTENSION_VERSION;
	}
	public static final Class<? extends Extension> EXTENSION = ConsentExtension.class;

	/**
	 * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
	 */
	@Deprecated
	public static void registerExtension() {
		MobileCore.registerExtension(
			ConsentExtension.class,
			extensionError -> {
				if (extensionError == null) {
					return;
				}
				Log.error(
					ConsentConstants.LOG_TAG,
					CLASS_NAME,
					"There was an error registering the Consent extension:  %s",
					extensionError.getErrorName()
				);
			}
		);
	}

	/**
	 * Merges the existing consents with the given consents. Duplicate keys will take the value of those passed in the API
	 * <p>
	 * Input example: {"consents": {"collect": {"val": "y"}}}
	 *
	 * @param consents A {@link Map} of consents to be merged with the existing consents
	 */
	public static void update(final Map<String, Object> consents) {
		if (consents == null || consents.isEmpty()) {
			Log.debug(
				ConsentConstants.LOG_TAG,
				CLASS_NAME,
				"Null/Empty consents passed to update API. Ignoring the API call."
			);
			return;
		}

		// create and dispatch an consent fragments update event
		final Event event = new Event.Builder(
			ConsentConstants.EventNames.CONSENT_UPDATE_REQUEST,
			EventType.CONSENT,
			EventSource.UPDATE_CONSENT
		)
			.setEventData(consents)
			.build();
		MobileCore.dispatchEvent(event);
	}

	/**
	 * Retrieves the current consent preferences stored in the Consent extension
	 * <p>
	 * Output example: {"consents": {"collect": {"val": "y"}}}
	 *
	 * @param callback The {@link AdobeCallback} is invoked with the current consent preferences.
	 */
	public static void getConsents(final AdobeCallback<Map<String, Object>> callback) {
		if (callback == null) {
			Log.debug(
				ConsentConstants.LOG_TAG,
				CLASS_NAME,
				"Unexpected null callback, provide a callback to retrieve current consents."
			);
			return;
		}

		final Event event = new Event.Builder(
			ConsentConstants.EventNames.GET_CONSENTS_REQUEST,
			EventType.CONSENT,
			EventSource.REQUEST_CONTENT
		)
			.build();
		MobileCore.dispatchEvent(event);
	}
}