package io.openems.edge.meter.socomec.threephase;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.test.InvertTest;

public class MeterSocomecThreephaseImplTest {

	private MeterSocomecThreephaseImpl meter;
	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.meter = new MeterSocomecThreephaseImpl();
		this.test = new ComponentTest(this.meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withRegisters(0xc558,
								// VOLTAGE_L1
								0x0000, 0x0064,
								// VOLTAGE_L2
								0x0000, 0x0064,
								// VOLTAGE_L3
								0x0000, 0x0064,

								// FREQUENCY
								0x0000, 0x1388,

								// CURRENT_L1
								0x0000, 0x03E8,
								// CURRENT_L2
								0x0000, 0x03E8,
								// CURRENT_L3
								0x0000, 0x03E8,

								// DUMMY
								0x0000, 0x0000,

								// ACTIVE_POWER
								0x0000, 0x0514,

								// REACTIVE_POWER
								0x0000, 0x02BC,

								// DUMMY
								0x0000, 0x0000, //
								0x0000, 0x0000, //

								// ACTIVE_POWER_L1
								0x0000, 0x03E8,
								// ACTIVE_POWER_L2
								0x0000, 0x03E8,
								// ACTIVE_POWER_L3
								0x0000, 0x03E8,

								// REACTIVE_POWER_L1
								0x0000, 0x02BC,
								// REACTIVE_POWER_L2
								0x0000, 0x02BC,
								// REACTIVE_POWER_L3
								0x0000, 0x02BC,

								// DUMMY
								0x0000, 0x0000, //
								0x0000, 0x0000, //
								0x0000, 0x0000, //
								0x0000, 0x0000, //
								0x0000, 0x0000, //
								0x0000, 0x0000, //

								// CURRENT
								0x0000, 0x0BB8,

								// DUMMY
								0x0000, 0x0000, //
								// VOLTAGE
								0x0000, 0x0064//
						)//
						.withRegisters(0xC702,
								// ACTIVE_PRODUCTION_ENERGY
								0x0000, 0x0514,

								// DUMMY
								0x0000, 0x0000, //
								0x0000, 0x0000, //

								// REACTIVE_PRODUCTION_ENERGY
								0x0000, 0x0000)); //

	}

	private void activateNonInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(false) //
						.build()); //
	}

	@Test
	public void testNonInvert() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedCountisE23_E24_E27_E28();
		this.test.next(InvertTest.testEnergyInvert(false));
		this.test.next(InvertTest.testInvert(false));
	}

	@Test
	public void testInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(true) //
						.build()); //
		this.meter.identifiedCountisE23_E24_E27_E28();
		this.test.next(InvertTest.testEnergyInvert(true));
		this.test.next(InvertTest.testInvert(true));
	}

	@Test
	public void testCountisE14() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedCountisE14();
	}

	@Test
	public void testCountisE23_E24_E27_E28() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedCountisE23_E24_E27_E28();
	}

	@Test
	public void testCountisE34_E44() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedCountisE34_E44();
	}

	@Test
	public void testDirisA10() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedDirisA10();
	}

	@Test
	public void testDirisA14() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedDirisA14();
	}

	@Test
	public void testDirisB30() throws Exception {
		this.activateNonInvert();
		this.meter.identifiedDirisB30();
	}

}
