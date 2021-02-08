package io.openems.edge.bridge.modbus;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.common.test.ComponentTest;

public class BridgeModbusTcpImplTest {

	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeModbusTcpImpl()) //
				.activate(MyConfigTcp.create() //
						.setId(MODBUS_ID) //
						.setIp("10.0.0.1") //
						.setPort(502) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
		;
	}

}
