package io.openems.edge.io.shelly.shellypro3em;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class IoShelly3EmImplTest {

	private static final String METER_ID = "io0";
	private static final String MODBUS_ID = "modbus0";

	private static IoShellyPro3EmImpl meter;

	@Test
	public void setup() throws Exception {
		meter = new IoShellyPro3EmImpl();
		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setType(MeterType.GRID) //
						.build()); //
	}

}
