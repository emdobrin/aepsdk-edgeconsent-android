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

package com.adobe.marketing.mobile.edge.consent.util;

import static com.adobe.marketing.mobile.edge.consent.util.ConsentTestConstants.*;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobileCoreHelper;
import com.adobe.marketing.mobile.edge.consent.util.MonitorExtension.EventSpec;
import com.adobe.marketing.mobile.services.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test helper for functional testing to read, write, reset and assert against eventhub events, shared states and persistence data.
 */
public class TestHelper {

	private static final String LOG_SOURCE = "TestHelper";
	static final int WAIT_TIMEOUT_MS = 1000;
	static final int WAIT_EVENT_TIMEOUT_MS = 2000;
	static final long WAIT_SHARED_STATE_MS = 5000;
	private static final long REGISTRATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);
	static Application defaultApplication;

	// List of threads to wait for after test execution
	private static final List<String> knownThreads = new ArrayList<>();

	static {
		knownThreads.add("pool"); // used for threads that execute the listeners code
		knownThreads.add("ADB"); // module internal threads
	}

	/**
	 * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
	 * tearsdown the MobileCore after test execution.
	 *
	 * To use, add the following to your test class:
	 * <pre>
	 * 	@Rule
	 * 	public TestHelper.SetupCoreRule coreRule = new TestHelper.SetupCoreRule();
	 * </pre>
	 */
	public static class SetupCoreRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (defaultApplication == null) {
						Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
						defaultApplication = Instrumentation.newApplication(CustomApplication.class, context);
					}

					MobileCore.setLogLevel(LoggingMode.VERBOSE);
					MobileCore.setApplication(defaultApplication);

					try {
						base.evaluate();
					} catch (Throwable e) {
						Log.debug(LOG_TAG, "SetupCoreRule", "Wait after test failure.");
						throw e; // rethrow test failure
					} finally {
						// After test execution
						Log.debug(LOG_TAG, "SetupCoreRule", "Finished '" + description.getMethodName() + "'");
						waitForThreads(5000); // wait to allow thread to run after test execution
						MobileCoreHelper.resetSDK();
						TestPersistenceHelper.resetKnownPersistence();
						resetTestExpectations();
					}
				}
			};
		}
	}

	/**
	 * Applies the configuration provided, registers the extensions and then starts
	 * core.
	 * @param extensions the extensions that need to be registered
	 * @param configuration the initial configuration update that needs to be applied
	 * @throws InterruptedException if the wait time for extension registration has elapsed
	 */
	public static void registerExtensions(
		final List<Class<? extends Extension>> extensions,
		@Nullable final Map<String, Object> configuration
	) throws InterruptedException {
		if (configuration != null) {
			MobileCore.updateConfiguration(configuration);
		}

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		MobileCore.registerExtensions(extensions, o -> latch.countDown());
		latch.await(REGISTRATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Waits for all the {@code #knownThreads} to finish or fails the test after timeoutMillis if some of them are still running
	 * when the timer expires. If timeoutMillis is 0, a default timeout will be set = 1000ms
	 *
	 * @param timeoutMillis max waiting time
	 */
	public static void waitForThreads(final int timeoutMillis) {
		int TEST_DEFAULT_TIMEOUT_MS = 1000;
		int TEST_DEFAULT_SLEEP_MS = 50;
		int TEST_INITIAL_SLEEP_MS = 100;

		long startTime = System.currentTimeMillis();
		int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
		int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

		sleep(TEST_INITIAL_SLEEP_MS);
		Set<Thread> threadSet = getEligibleThreads();

		while (threadSet.size() > 0 && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
			Log.debug(LOG_TAG, LOG_SOURCE, "waitForThreads - Still waiting for " + threadSet.size() + " thread(s)");
			for (Thread t : threadSet) {
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					"waitForThreads - Waiting for thread " + t.getName() + " (" + t.getId() + ")"
				);

				boolean done = false;
				boolean timedOut = false;

				while (!done && !timedOut) {
					if (
						t.getState().equals(Thread.State.TERMINATED) ||
						t.getState().equals(Thread.State.TIMED_WAITING) ||
						t.getState().equals(Thread.State.WAITING)
					) {
						//Cannot use the join() API since we use a cached thread pool, which
						//means that we keep idle threads around for 60secs (default timeout).
						done = true;
					} else {
						//blocking
						sleep(sleepTime);
						timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
					}
				}

				if (timedOut) {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"waitForThreads - Timeout out waiting for thread " + t.getName() + " (" + t.getId() + ")"
					);
				} else {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"waitForThreads - Done waiting for thread " + t.getName() + " (" + t.getId() + ")"
					);
				}
			}

			threadSet = getEligibleThreads();
		}

		Log.debug(LOG_TAG, LOG_SOURCE, "waitForThreads - All known threads are terminated.");
	}

	/**
	 * Retrieves all the known threads that are still running
	 * @return set of running tests
	 */
	private static Set<Thread> getEligibleThreads() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Set<Thread> eligibleThreads = new HashSet<>();

		for (Thread t : threadSet) {
			if (
				isAppThread(t) &&
				!t.getState().equals(Thread.State.WAITING) &&
				!t.getState().equals(Thread.State.TERMINATED) &&
				!t.getState().equals(Thread.State.TIMED_WAITING)
			) {
				eligibleThreads.add(t);
			}
		}

		return eligibleThreads;
	}

	/**
	 * Checks if current thread is not a daemon and its name starts with one of the known thread names specified here
	 * {@link #knownThreads}
	 *
	 * @param t current thread to verify
	 * @return true if it is a known thread, false otherwise
	 */
	private static boolean isAppThread(final Thread t) {
		if (t.isDaemon()) {
			return false;
		}

		for (String prefix : knownThreads) {
			if (t.getName().startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Resets the network and event test expectations.
	 */
	public static void resetTestExpectations() {
		Log.debug(LOG_TAG, LOG_SOURCE, "Resetting functional test expectations for events");
		MonitorExtension.reset();
	}

	// ---------------------------------------------------------------------------------------------
	// Event Test Helpers
	// ---------------------------------------------------------------------------------------------

	/**
	 * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
	 * @param type the event type as in the expectation
	 * @param source the event source as in the expectation
	 * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
	 * @throws InterruptedException
	 * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
	 */
	public static List<Event> getDispatchedEventsWith(final String type, final String source)
		throws InterruptedException {
		return getDispatchedEventsWith(type, source, WAIT_EVENT_TIMEOUT_MS);
	}

	/**
	 * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
	 * @param type the event type as in the expectation
	 * @param source the event source as in the expectation
	 * @param timeout how long should this method wait for the expected event, in milliseconds.
	 * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
	 * @throws InterruptedException
	 * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
	 */
	public static List<Event> getDispatchedEventsWith(final String type, final String source, int timeout)
		throws InterruptedException {
		EventSpec eventSpec = new EventSpec(source, type);
		Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();

		sleep(timeout);

		return receivedEvents.containsKey(eventSpec) ? receivedEvents.get(eventSpec) : Collections.emptyList();
	}

	/**
	 * Synchronous call to get the shared state for the specified {@code stateOwner}.
	 * This API throws an assertion failure in case of timeout.
	 * @param stateOwner the owner extension of the shared state (typically the name of the extension)
	 * @param timeout how long should this method wait for the requested shared state, in milliseconds
	 * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
	 * @throws InterruptedException
	 */
	public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout)
		throws InterruptedException {
		Event event = new Event.Builder(
			"Get Shared State Request",
			ConsentTestConstants.EventType.MONITOR,
			ConsentTestConstants.EventSource.SHARED_STATE_REQUEST
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(ConsentTestConstants.EventDataKey.STATE_OWNER, stateOwner);
					}
				}
			)
			.build();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		final Map<String, Object> sharedState = new HashMap<>();
		MobileCore.dispatchEventWithResponseCallback(
			event,
			WAIT_EVENT_TIMEOUT_MS,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(AdobeError adobeError) {
					Log.error(
						LOG_TAG,
						LOG_SOURCE,
						"Failed to get shared state for " + stateOwner + ": " + adobeError.getErrorName()
					);
				}

				@Override
				public void call(Event event) {
					if (event.getEventData() != null) {
						sharedState.putAll(event.getEventData());
					}

					latch.countDown();
				}
			}
		);
		assertTrue("Timeout waiting for shared state " + stateOwner, latch.await(timeout, TimeUnit.MILLISECONDS));
		return sharedState.isEmpty() ? null : sharedState;
	}

	/**
	 * Synchronous call to get the XDM shared state for the specified {@code stateOwner}.
	 * This API throws an assertion failure in case of timeout.
	 * @param stateOwner the owner extension of the shared state (typically the name of the extension)
	 * @param timeout how long should this method wait for the requested shared state, in milliseconds
	 * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
	 * @throws InterruptedException
	 */
	public static Map<String, Object> getXDMSharedStateFor(final String stateOwner, int timeout)
		throws InterruptedException {
		Event event = new Event.Builder(
			"Get Shared State Request",
			ConsentTestConstants.EventType.MONITOR,
			ConsentTestConstants.EventSource.XDM_SHARED_STATE_REQUEST
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(ConsentTestConstants.EventDataKey.STATE_OWNER, stateOwner);
					}
				}
			)
			.build();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		final Map<String, Object> sharedState = new HashMap<>();
		MobileCore.dispatchEventWithResponseCallback(
			event,
			WAIT_SHARED_STATE_MS,
			new AdobeCallbackWithError<Event>() {
				@Override
				public void fail(AdobeError adobeError) {
					Log.error(
						LOG_TAG,
						LOG_SOURCE,
						"Failed to get shared state for " + stateOwner + ": " + adobeError.getErrorName()
					);
				}

				@Override
				public void call(Event event) {
					if (event.getEventData() != null) {
						sharedState.putAll(event.getEventData());
					}

					latch.countDown();
				}
			}
		);

		assertTrue("Timeout waiting for shared state " + stateOwner, latch.await(timeout, TimeUnit.MILLISECONDS));

		return sharedState.isEmpty() ? null : sharedState;
	}

	/**
	 * Pause test execution for the given {@code milliseconds}
	 * @param milliseconds the time to sleep the current thread.
	 */
	public static void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Dummy Application for the test instrumentation
	 */
	public static class CustomApplication extends Application {

		public CustomApplication() {}
	}
}
