package io.openems.edge.meter.kdk.puct2;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.test.InvertTest;

public class MeterKdk2puctImplTest {

	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new MeterKdk2puctImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withRegisters(0x4007, 0x0000, 0x0000)//
						.withRegisters(0x401F, 0x0000, 0x0000, 0x0, 0x0, 0x0000, 0x01)//
						.withRegisters(0x5002,
								// VOLTAGE_L1
								0x3F80, 0x0000,
								// VOLTAGE_L2
								0x3F80, 0x0000,
								// VOLTAGE_L3
								0x3F80, 0x0000,
								// FREQUENCY
								0x40A0, 0x0000,

								// DUMMY
								0x0000, 0x0000,

								// CURRENT_L1
								0x3F80, 0x0000,
								// CURRENT_L2
								0x3F80, 0x0000,
								// CURRENT_L3
								0x3F80, 0x0000,

								// ACTIVE_POWER
								0x4150, 0x0001,

								// ACTIVE_POWER_L1
								0x4120, 0x0000,
								// ACTIVE_POWER_L2
								0x4120, 0x0000,
								// ACTIVE_POWER_L3
								0x4120, 0x0000,

								// REACTIVE_POWER
								0x40E0, 0x0001,

								// REACTIVE_POWER_L1
								0x40E0, 0x0001,
								// REACTIVE_POWER_L2
								0x40E0, 0x0001,
								// REACTIVE_POWER_L3
								0x40E0, 0x0001)
						.withRegisters(0x600C,
								// ACTIVE_PRODUCTION_ENERGY
								0x4150, 0x0001,

								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// REACTIVE_PRODUCTION_ENERGY
								0x0000, 0x0000,

								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000)); //
	}

	@Test
	public void testInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMeterType(GRID) //
						.setInvert(false) //
						.build())//
				.next(new TestCase())//
				.next(InvertTest.testInvert(false))//
				.next(InvertTest.testEnergyInvert(false));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMeterType(GRID) //
						.setInvert(true) //
						.build())//
				.next(new TestCase())//
				.next(InvertTest.testInvert(true))//
				.next(InvertTest.testEnergyInvert(true));
	}
}
