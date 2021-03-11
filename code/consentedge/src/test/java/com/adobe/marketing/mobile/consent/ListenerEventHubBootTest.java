package com.adobe.marketing.mobile.consent;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ListenerEventHubBootTest {

    @Mock
    private ConsentExtension mockConsentExtension;

    private ListenerEventHubBoot listener;

    @Before
    public void setup() {
        mockConsentExtension = Mockito.mock(ConsentExtension.class);
        MobileCore.start(null);
        listener = spy(new ListenerEventHubBoot(null, ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.CONSENT_PREFERENCE));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Event Hub Boot", ConsentConstants.EventType.HUB, ConsentConstants.EventSource.BOOTED).build();
        doReturn(mockConsentExtension).when(listener).getConsentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(1)).handleEventHubBoot(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Event Hub Boot", ConsentConstants.EventType.HUB, ConsentConstants.EventSource.BOOTED).build();
        doReturn(null).when(listener).getConsentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(0)).handleConsentUpdate(any(Event.class));
    }

}
