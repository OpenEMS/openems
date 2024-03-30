package io.openems.edge.meter.tibber.pulse;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;

public class MeterTibberPulseImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String password = "CHX-ED4R";

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterTibberPulseImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setPassword(password) //
						.setType(MeterType.GRID) //
						.build()) //
		;
	}

}