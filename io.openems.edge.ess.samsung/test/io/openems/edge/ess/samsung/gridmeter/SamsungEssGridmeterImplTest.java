package io.openems.edge.ess.samsung.gridmeter;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;

public class SamsungEssGridmeterImplTest {

	private static final String COMPONENT_ID = "meter0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SamsungEssGridmeterImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}

}