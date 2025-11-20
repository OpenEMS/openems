package io.openems.edge.ess.samsung.charger;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SamsungEssChargerImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SamsungEssChargerImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpBundle().factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}