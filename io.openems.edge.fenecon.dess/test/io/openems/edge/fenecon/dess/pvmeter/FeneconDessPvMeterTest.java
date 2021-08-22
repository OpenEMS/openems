package io.openems.edge.fenecon.dess.pvmeter;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.fenecon.dess.ResetChannelSources;

public class FeneconDessPvMeterTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Before
	public void before() {
		ResetChannelSources.run();
	}

	@Test
	public void test() throws Exception {
		new ComponentTest(new FeneconDessPvMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.build()) //
		;
	}
}