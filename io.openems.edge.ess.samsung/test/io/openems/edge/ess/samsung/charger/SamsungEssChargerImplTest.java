package io.openems.edge.ess.samsung.charger;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SamsungEssChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SamsungEssChargerImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}