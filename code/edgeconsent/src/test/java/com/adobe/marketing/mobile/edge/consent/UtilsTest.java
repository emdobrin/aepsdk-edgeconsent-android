/*
  Copyright 2023 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

	@Test
	public void testDeepCopy_whenNull() {
		assertNull(Utils.deepCopy((Map) null));
	}

	@Test
	public void testDeepCopy_whenEmpty() {
		Map<String, Object> emptyMap = new HashMap<>();
		assertEquals(0, Utils.deepCopy(emptyMap).size());
	}

	@Test
	public void testDeepCopy_whenValidSimple_thenSetOriginalNull() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "value1");
		Map<String, Object> deepCopy = Utils.deepCopy(map);

		map = null;
		assertNotNull(deepCopy);
	}

	@Test
	public void testDeepCopy_whenValidSimple_thenMutateOriginal() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "value1");

		Map<String, Object> deepCopy = Utils.deepCopy(map);
		deepCopy.remove("key1");

		assertTrue(map.containsKey("key1"));
	}

	@Test
	public void testDeepCopy_whenValidNested() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "value1");

		Map<String, Object> nested = new HashMap<>();
		nested.put("nestedKey", "nestedValue");
		map.put("nestedMap", nested);

		Map<String, Object> deepCopy = Utils.deepCopy(map);
		Map<String, Object> nestedDeepCopy = (Map<String, Object>) deepCopy.get("nestedMap");
		nestedDeepCopy.put("newKey", "newValue");

		assertFalse(nested.size() == nestedDeepCopy.size());
	}

	@Test
	public void testDeepCopy_whenNullKey_ignoresKey() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "value1");
		map.put(null, "null");

		Map<String, Object> result = Utils.deepCopy(map);
		assertEquals(1, result.size());
		assertEquals("value1", result.get("key1"));
	}

	@Test
	public void testDeepCopy_whenInvalidMapWithCustomObjects_returnsNullAndNoThrow() {
		class CustomObj {

			private final int value;

			CustomObj(int value) {
				this.value = value;
			}
		}
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", new CustomObj(1000));

		Map<String, Object> deepCopy = Utils.deepCopy(map);
		assertNull(deepCopy);
	}

	@Test
	public void testOptDeepCopy_whenNull_fallbackNull() {
		assertNull(Utils.optDeepCopy(null, null));
	}

	@Test
	public void testOptDeepCopy_whenNull_fallbackEmptyMap() {
		Map<String, Object> emptyMap = new HashMap<>();
		Map<String, Object> copyMap = Utils.optDeepCopy(null, emptyMap);
		assertNotNull(copyMap);
		assertEquals(emptyMap, copyMap);
	}
}
