package io.openems.edge.pvinverter.sma;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.pvinverter.sunspec.Phase;

public class SmaPvInverterTest {

	private static final String PV_INVERTER_ID = "pvInverter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new PvInverterSmaSunnyTripowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setReadOnly(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setPhase(Phase.ALL) //
						.build()) //
		;
	}
}