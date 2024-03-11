package io.openems.edge.io.opendtu.inverter;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

public class OpendtuImplTest {

	private static final String COMPONENT_ID = "opendtu0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new OpendtuImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setUsername("admin") //
						.setPassword("admin") //
						.setPhase(SinglePhase.L1) //
						.setSerialNumber("123456") //
						.setInitialPowerLimit(100) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}

}
