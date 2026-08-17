package io.openems.common.bridge.http.logging;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.EndpointFetcherEvents;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpMediaType;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.ReflectionUtils;

class HttpBridgeLoggingServiceTest {

	private BridgeHttp bridge;
	private Logger logger;

	@BeforeEach
	void setup() {
		this.bridge = new BridgeHttpImpl(mock(), mock());
		this.bridge.setDebugMode(DebugMode.DETAILED);

		final var service = new HttpBridgeLoggingService(this.bridge, HttpBridgeLoggingServiceConfiguration.DEFAULT);

		this.logger = mock();
		ReflectionUtils.setAttributeViaReflection(service, "log", this.logger);
	}

	@Test
	void testRequestStartEvent() {
		this.bridge.raiseEvent(EndpointFetcherEvents.REQUEST_START,
				new EndpointFetcherEvents.RequestStartEvent(0L, mock()));
		verify(this.logger, times(1)).info(startsWith("Request[{}] started"), eq(0L), any());
	}

	@Test
	void testRequestSuccessEvent() {
		this.bridge.raiseEvent(EndpointFetcherEvents.REQUEST_SUCCESS,
				new EndpointFetcherEvents.RequestSuccessEvent(0L, mock(), mock()));
		verify(this.logger, times(1)).info(startsWith("Request[{}] success"), eq(0L), any());
	}

	@Test
	void testRequestFailedEvent() {
		this.bridge.raiseEvent(EndpointFetcherEvents.REQUEST_FAILED,
				new EndpointFetcherEvents.RequestFailedEvent(0L, mock(), mock()));
		verify(this.logger, times(1)).error(startsWith("Request[{}] failed"), eq(0L), any(), any());
	}

	@Test
	void testSanitizeHeaders() {
		final var headers = Map.of(//
				HttpHeader.HEADER_AUTHORIZATION, "Bearer 12345", //
				HttpHeader.HEADER_ACCEPT, HttpMediaType.Text.PLAIN //
		);
		final var expectedHeaders = Map.of(//
				HttpHeader.HEADER_AUTHORIZATION, "****", //
				HttpHeader.HEADER_ACCEPT, HttpMediaType.Text.PLAIN //
		);

		assertEquals(expectedHeaders, HttpBridgeLoggingService
				.sanitizeHeaders(HttpBridgeLoggingServiceConfiguration.SANITIZE_AUTHORIZATION, headers));
	}

}