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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ConsentTest {

	private static Map<String, Object> SAMPLE_CONSENTS_MAP = ConsentTestUtil.CreateConsentXDMMap("y");

	@Before
	public void setup() {}

	// ========================================================================================
	// extensionVersion
	// ========================================================================================

	@Test
	public void test_extensionVersionAPI() {
		// test
		String extensionVersion = Consent.extensionVersion();
		assertEquals(
			"The Extension version API returns the correct value",
			ConsentConstants.EXTENSION_VERSION,
			extensionVersion
		);
	}

	// ========================================================================================
	// registerExtension
	// ========================================================================================
	@Test
	public void testRegistration() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			// mock MobileCore.registerExtension()
			ArgumentCaptor<Class> extensionClassCaptor = ArgumentCaptor.forClass(Class.class);
			ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
				ExtensionErrorCallback.class
			);
			mobileCoreMockedStatic
				.when(() -> MobileCore.registerExtension(extensionClassCaptor.capture(), callbackCaptor.capture()))
				.thenReturn(true);
			// call registerExtension() API
			Consent.registerExtension();
			// verify: happy
			assertNotNull(callbackCaptor.getValue());
			assertEquals(ConsentExtension.class, extensionClassCaptor.getValue());
			// verify: not exception when error callback was called
			callbackCaptor.getValue().error(ExtensionError.UNEXPECTED_ERROR);
		}
	}

	// ========================================================================================
	// publicExtensionConstants
	// ========================================================================================
	@Test
	public void test_publicExtensionConstants() {
		assertEquals(ConsentExtension.class, Consent.EXTENSION);
		List<Class<? extends Extension>> extensions = new ArrayList<>();
		extensions.add(Consent.EXTENSION);
		// should not throw exceptions
		MobileCore.registerExtensions(extensions, null);
	}

	// ========================================================================================
	// Registration without Error
	// ========================================================================================
	@Test
	public void test_registerExtension_withoutError() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			// mock MobileCore.registerExtension()
			ArgumentCaptor<Class> extensionClassCaptor = ArgumentCaptor.forClass(Class.class);
			ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
				ExtensionErrorCallback.class
			);
			mobileCoreMockedStatic
				.when(() -> MobileCore.registerExtension(extensionClassCaptor.capture(), callbackCaptor.capture()))
				.thenReturn(true);
			// call registerExtension() API
			Consent.registerExtension();
			// verify: happy
			assertNotNull(callbackCaptor.getValue());
			assertEquals(ConsentExtension.class, extensionClassCaptor.getValue());
			// verify: not exception when error callback was called
			callbackCaptor.getValue().error(null);
		}
	}

	// ========================================================================================
	// update Public API
	//========================================================================================
	@Test
	public void testUpdate() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			mobileCoreMockedStatic.reset();
			ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

			// test
			Consent.update(SAMPLE_CONSENTS_MAP);

			// verify
			mobileCoreMockedStatic.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
			Event dispatchedEvent = eventCaptor.getValue();

			assertNotNull(dispatchedEvent);
			assertEquals(ConsentConstants.EventNames.CONSENT_UPDATE_REQUEST, dispatchedEvent.getName());
			assertEquals(EventType.CONSENT, dispatchedEvent.getType());
			assertEquals(EventSource.UPDATE_CONSENT, dispatchedEvent.getSource());
			assertEquals(SAMPLE_CONSENTS_MAP, dispatchedEvent.getEventData());
		}
	}

	@Test
	public void testUpdate_withNull() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			mobileCoreMockedStatic.reset();

			// test
			Consent.update(null);

			// verify
			mobileCoreMockedStatic.verifyNoInteractions();
		}
	}

	@Test
	public void testGetConsents() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			// setup
			final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
			final ArgumentCaptor<AdobeCallbackWithError<Event>> callbackCaptor = ArgumentCaptor.forClass(
				AdobeCallbackWithError.class
			);

			final List<Map<String, Object>> callbackReturnValues = new ArrayList<>();

			// test
			Consent.getConsents(
				new AdobeCallback<Map<String, Object>>() {
					@Override
					public void call(Map<String, Object> stringObjectMap) {
						callbackReturnValues.add(stringObjectMap);
					}
				}
			);

			// verify
			mobileCoreMockedStatic.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					ArgumentMatchers.anyLong(),
					ArgumentMatchers.any(AdobeCallbackWithError.class)
				)
			);

			final Event dispatchedEvent = eventCaptor.getValue();
			final AdobeCallbackWithError<Event> callbackWithError = callbackCaptor.getValue();

			// verify the dispatched event details
			assertEquals(ConsentConstants.EventNames.GET_CONSENTS_REQUEST, dispatchedEvent.getName());
			assertEquals(EventType.CONSENT, dispatchedEvent.getType());
			assertEquals(EventSource.REQUEST_CONTENT, dispatchedEvent.getSource());
			final Map<String, Object> eventData = dispatchedEvent.getEventData();
			assertEquals(null, eventData); //TODO check is null or empty
			//verify callback responses

			//callbackWithError.call(buildConsentResponseEvent(SAMPLE_CONSENTS_MAP));
			verify(callbackWithError, times(1)).call(buildConsentResponseEvent(SAMPLE_CONSENTS_MAP));
			assertEquals(SAMPLE_CONSENTS_MAP, callbackReturnValues.get(0));
			// TODO - enable when ExtensionError creation is available
			// should not crash on calling the callback
			//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
		}
	}

	@Test
	public void testGetConsents_NullCallback() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			mobileCoreMockedStatic.reset();
			ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

			// test
			Consent.getConsents(null);

			// verify
			mobileCoreMockedStatic.verifyNoInteractions();
		}
	}

	@Test
	public void testGetConsents_NullResponseEvent() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			// setup
			final String KEY_IS_ERRORCALLBACK_CALLED = "errorCallBackCalled";
			final String KEY_CAPTUREDERRORCALLBACK = "capturedErrorCallback";
			final Map<String, Object> errorCapture = new HashMap<>();
			final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
			final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
			final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
				@Override
				public void fail(AdobeError adobeError) {
					errorCapture.put(KEY_IS_ERRORCALLBACK_CALLED, true);
					errorCapture.put(KEY_CAPTUREDERRORCALLBACK, adobeError);
				}

				@Override
				public void call(Object o) {}
			};

			// test
			Consent.getConsents(callbackWithError);

			// verify if the event is dispatched
			mobileCoreMockedStatic.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					ArgumentMatchers.anyLong(),
					ArgumentMatchers.any(AdobeCallbackWithError.class)
				)
			);
			// set response event to null
			adobeCallbackCaptor.getValue().call(null);

			// verify
			assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
			assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
		}
	}

	// ========================================================================================
	// Private method
	// ========================================================================================
	private Event buildConsentResponseEvent(final Map<String, Object> eventData) {
		return new Event.Builder(
			ConsentConstants.EventNames.GET_CONSENTS_RESPONSE,
			EventType.CONSENT,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(eventData)
			.build();
	}
}
