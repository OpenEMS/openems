package io.openems.edge.io.opendtu.inverter;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttp;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

public class OpendtuImplTest {

	public static final String COMPONENT_ID = "opendtu0";
	public final String inverterSerial = "873249724798";

	@Test
	public void test() throws Exception {
		new ComponentTest(new OpendtuImpl()) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.addReference("httpBridge", new DummyBridgeHttp()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setUsername("admin") //
						.setPassword("admin") //
						.setPhase(SinglePhase.L1) //
						.setSerialNumber(this.inverterSerial) //
						.setInitialPowerLimit(100) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}

}
