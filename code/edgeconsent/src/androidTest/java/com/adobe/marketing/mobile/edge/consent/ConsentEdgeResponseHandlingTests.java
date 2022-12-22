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

import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.*;
import static com.adobe.marketing.mobile.edge.consent.util.TestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.util.MonitorExtension;
import com.adobe.marketing.mobile.edge.consent.util.TestHelper;
import com.adobe.marketing.mobile.edge.consent.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class ConsentEdgeResponseHandlingTests {

	static final String SHARED_STATE = "com.adobe.eventSource.sharedState";

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	// --------------------------------------------------------------------------------------------
	// Setup
	// --------------------------------------------------------------------------------------------

	@Before
	public void setup() throws Exception {
		TestHelper.registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Consent.EXTENSION), null);
	}

	@Test
	public void test_EdgeResponse_MergesWithCurrentConsent() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default      pending     NO      null
		// Updated        YES
		// EdgeResponse    NO
		//-------------------------------------------
		// Final           NO      NO       available
		//-------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// setup
		applyDefaultConsent(CreateConsentXDMMap("p", "n"));
		Consent.update(CreateConsentXDMMap("y"));
		waitForThreads(1000);
		resetTestExpectations();

		MobileCore.dispatchEvent(buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("n"))); // edge response sets the collect consent to no
		waitForThreads(1000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
		assertEquals(3, consentResponseData.size());
		assertEquals("n", consentResponseData.get("consents.collect.val"));
		assertEquals("n", consentResponseData.get("consents.adID.val"));
		assertNotNull(consentResponseData.get("consents.metadata.time"));

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size());
		assertEquals("n", xdmSharedState.get("consents.collect.val"));
		assertEquals("n", xdmSharedState.get("consents.adID.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));

		// verify persisted data - default consents are not persisted
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			ConsentConstants.DataStoreKey.DATASTORE_NAME,
			ConsentConstants.DataStoreKey.CONSENT_PREFERENCES
		);
		Map<String, Object> persistedMap = JSONUtils.toMap(new JSONObject(persistedJson));
		Map<String, String> flattenPersistedMap = flattenMap(persistedMap);
		assertEquals(2, flattenPersistedMap.size());
		assertEquals("n", flattenPersistedMap.get("consents.collect.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));
	}

	@Test
	public void test_EdgeResponse_InvalidPayload() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default      pending
		// Updated        YES
		// EdgeResponse  invalid
		//-------------------------------------------
		// Final           YES            available
		//-------------------------------------------

		// setup
		applyDefaultConsent(CreateConsentXDMMap("p"));
		Consent.update(CreateConsentXDMMap("y"));
		waitForThreads(1000);
		resetTestExpectations();

		// test
		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEvent("{\n" + "  \"payload\" : \"not what I expect\"\n" + "}")
		);
		waitForThreads(1000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(2, xdmSharedState.size());
		assertEquals("y", xdmSharedState.get("consents.collect.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			ConsentConstants.DataStoreKey.DATASTORE_NAME,
			ConsentConstants.DataStoreKey.CONSENT_PREFERENCES
		);
		Map<String, Object> persistedMap = JSONUtils.toMap(new JSONObject(persistedJson));
		Map<String, String> flattenPersistedMap = flattenMap(persistedMap);
		assertEquals(2, flattenPersistedMap.size());
		assertEquals("y", flattenPersistedMap.get("consents.collect.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));
	}

	@Test
	public void test_EdgeResponse_NoConsentChangeAndNoTimestamp() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Updated        YES      YES      timestamp
		// EdgeResponse   YES      YES      null
		//-------------------------------------------
		// Final           YES      YES     timestamp
		//-------------------------------------------

		// setup
		Consent.update(CreateConsentXDMMap("y"));
		waitForThreads(1000);
		resetTestExpectations();

		// read timestamp from XDM shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		String timestamp = xdmSharedState.get("consents.metadata.time");

		// test
		MobileCore.dispatchEvent(buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("y")));
		waitForThreads(1000);

		// verify that shared state and consent response events are not dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(0, consentResponseEvents.size());
		List<Event> sharedStateChangeEvents = getDispatchedEventsWith(EventType.HUB, SHARED_STATE);
		assertEquals(0, sharedStateChangeEvents.size());

		// verify timestamp has not changed
		xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(timestamp, xdmSharedState.get("consents.metadata.time"));
	}

	@Test
	public void test_EdgeResponse_NoConsentChangeAndSameTimestamp() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Updated        YES      YES      timestamp
		// EdgeResponse   YES      YES      timestamp
		//-------------------------------------------
		// Final           YES      YES     timestamp
		//-------------------------------------------

		// setup
		Consent.update(CreateConsentXDMMap("y", "n"));
		waitForThreads(1000);
		resetTestExpectations();

		// read timestamp from XDM shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		String timestamp = xdmSharedState.get("consents.metadata.time");

		// test
		MobileCore.dispatchEvent(buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("y", "n", timestamp)));
		waitForThreads(1000);

		// verify that shared state and consent response events are not dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(0, consentResponseEvents.size());
		List<Event> sharedStateChangeEvents = getDispatchedEventsWith(EventType.HUB, SHARED_STATE);
		assertEquals(0, sharedStateChangeEvents.size());

		// verify timestamp has not changed
		xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(timestamp, xdmSharedState.get("consents.metadata.time"));
	}
}
