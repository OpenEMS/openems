package io.openems.edge.io.opendtu.inverter;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;

public class IoOpenDtuImplTest {

	private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new OpendtuImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.PRODUCTION) //
						.setUsername("admin") //
						.setPassword("admin") //
						.setSerialNumberL1("1234567890L1") //
						.setSerialNumberL2("1234567890L2") //
						.setSerialNumberL3("1234567890L3") //
						.setRelativeLimit(100) //
						.setAbsoluteLimit(800) //
						.setThreshold(50) //
						.setDelay(30) //
						.setDebugMode(true) //
						.build()); //
		;
	}
}