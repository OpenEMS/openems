package io.openems.edge.ess.samsung.ess;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.power.api.Phase;

public class SamsungEssImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SamsungEssImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setIp("127.0.0.1") //
						.setPhase(Phase.L1) //
						.setCapacity(3600) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}