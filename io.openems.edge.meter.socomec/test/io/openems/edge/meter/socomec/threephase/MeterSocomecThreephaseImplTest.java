package io.openems.edge.meter.socomec.threephase;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class MeterSocomecThreephaseImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	private static MeterSocomecThreephaseImpl meter;

	@Before
	public void setup() throws Exception {
		meter = new MeterSocomecThreephaseImpl();
		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setType(MeterType.GRID) //
						.setInvert(false) //
						.build()); //
	}

	@Test
	public void testCountisE14() throws Exception {
		meter.identifiedCountisE14();
	}

	@Test
	public void testCountisE23_E24_E27_E28() throws Exception {
		meter.identifiedCountisE23_E24_E27_E28();
	}

	@Test
	public void testCountisE34_E44() throws Exception {
		meter.identifiedCountisE34_E44();
	}

	@Test
	public void testDirisA10() throws Exception {
		meter.identifiedDirisA10();
	}

	@Test
	public void testDirisA14() throws Exception {
		meter.identifiedDirisA14();
	}

	@Test
	public void testDirisB30() throws Exception {
		meter.identifiedDirisB30();
	}

}
