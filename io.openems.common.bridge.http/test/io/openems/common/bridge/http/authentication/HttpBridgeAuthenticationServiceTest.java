package io.openems.common.bridge.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

class HttpBridgeAuthenticationServiceTest {

	@Test
	void testCreateServiceOncePerDefinition() {
		try (var authService = new HttpBridgeAuthenticationService(new BridgeHttpImpl(mock(), mock()), mock(),
				mock())) {
			final HttpBridgeServiceDefinition<HttpBridgeService> dummyServiceDefinition = mock();
			when(dummyServiceDefinition.create(any(), any(), any())).thenReturn(mock());

			final var createdService = authService.createService(dummyServiceDefinition);

			assertEquals(createdService, authService.createService(dummyServiceDefinition));
			verify(dummyServiceDefinition, times(1)).create(any(), any(), any());
		}
	}

}