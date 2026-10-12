package io.openems.edge.bridge.eebus;

import io.openems.edge.bridge.eebus.test.DummyBridgeEebus;
import org.junit.Test;

public class BridgeEebusTest {
	@Test
	public void testDummyBridge() throws Exception {
		var bridge = new DummyBridgeEebus("eebus0");
	}
}
