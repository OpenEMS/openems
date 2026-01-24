package io.openems.common.bridge.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;

public class BridgeHttpImplTest {

	@Test
	public void setMaximumPoolSize() {
		final var executor = new DummyBridgeHttpExecutor();
		final var bridge = new BridgeHttpImpl(new DummyEndpointFetcher(), executor);

		assertNotEquals(100, executor.getMaximumPoolSize());
		bridge.setMaximumPoolSize(100);
		assertEquals(100, executor.getMaximumPoolSize());
	}

}