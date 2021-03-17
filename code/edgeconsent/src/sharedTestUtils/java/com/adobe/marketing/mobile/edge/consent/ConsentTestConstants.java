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

public class ConsentTestConstants {

    public static String SAMPLE_METADATA_TIMESTAMP = "2019-09-23T18:15:45Z";

    public final class DataStoreKey {
        public static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
        public static final String CONSENT_DATASTORE = "com.adobe.edge.consent";
        public static final String CONSENT_PREFERENCES = "consent:preferences";
        private DataStoreKey() { }
    }

    public final class SharedStateName {
        public static final String CONFIG = "com.adobe.module.configuration";
        public static final String EVENT_HUB = "com.adobe.module.eventhub";
        private SharedStateName() { }
    }

    public final class GetConsentHelper {
        public static final String VALUE = "getConsentValue";
        public static final String ERROR = "getConsentError";
        private GetConsentHelper() { }
    }

    private ConsentTestConstants() { }
}
