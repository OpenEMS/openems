package io.openems.edge.meter.socomec.threephase;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class SocomecMeterThreephaseImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		SocomecMeterThreephaseImpl meter = new SocomecMeterThreephaseImpl();
		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setType(MeterType.GRID) //
						.setInvert(false) //
						.build()); //

		meter.identifiedCountisE14();
		meter.identifiedCountisE23_E24();
		meter.identifiedDirisA10();
		meter.identifiedDirisA14();
		meter.identifiedDirisB30();
	}

}
