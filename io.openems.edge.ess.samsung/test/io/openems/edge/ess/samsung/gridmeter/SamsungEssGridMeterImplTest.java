package io.openems.edge.ess.samsung.gridmeter;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SamsungEssGridMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SamsungEssGridMeterImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpBundle().factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(new DummyCycleSubscriber())) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setIp("127.0.0.1") //
						.setType(MeterType.GRID) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}