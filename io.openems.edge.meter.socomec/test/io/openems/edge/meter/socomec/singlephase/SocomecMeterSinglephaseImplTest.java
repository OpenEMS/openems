package io.openems.edge.meter.socomec.singlephase;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

public class SocomecMeterSinglephaseImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		SocomecMeterSinglephaseImpl meter = new SocomecMeterSinglephaseImpl();
		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setType(MeterType.GRID) //
						.setInvert(false) //
						.setPhase(SinglePhase.L1) //
						.build()); //

		meter.identifiedCountisE14();
		meter.identifiedCountisE23_E24();
		meter.identifiedDirisA10();
		meter.identifiedDirisA14();
		meter.identifiedDirisB30();
	}

}
