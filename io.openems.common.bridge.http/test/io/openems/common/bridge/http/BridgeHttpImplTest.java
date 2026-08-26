package io.openems.common.bridge.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;

public class BridgeHttpImplTest {

	@Test
	void setMaximumPoolSize() {
		final var executor = new DummyBridgeHttpExecutor();
		final var bridge = new BridgeHttpImpl(new DummyEndpointFetcher(), executor);

		assertNotEquals(100, executor.getMaximumPoolSize());
		bridge.setMaximumPoolSize(100);
		assertEquals(100, executor.getMaximumPoolSize());
	}

}