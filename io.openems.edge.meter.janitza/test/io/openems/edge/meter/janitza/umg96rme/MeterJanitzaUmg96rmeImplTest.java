package io.openems.edge.meter.janitza.umg96rme;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.test.InvertTest;

public class MeterJanitzaUmg96rmeImplTest {

	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new MeterJanitzaUmg96rmeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withRegisters(19000,

								// VOLTAGE_L1
								0x3F80, 0x0000,
								// VOLTAGE_L2
								0x3F80, 0x0000,
								// VOLTAGE_L3
								0x3F80, 0x0000,

								// Dummy
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, //

								// CURRENT_L1
								0x3F80, 0x0000,
								// CURRENT_L2
								0x3F80, 0x0000,
								// CURRENT_L3
								0x3F80, 0x0000,
								// CURRENT
								0x4040, 0x0001,

								// ACTIVE_POWER_L1
								0x461C, 0x4000,
								// ACTIVE_POWER_L2
								0x461C, 0x4000,
								// ACTIVE_POWER_L3
								0x461C, 0x4000,
								// ACTIVE_POWER
								0x464B, 0x2000,

								// Dummy
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, //

								// REACTIVE_POWER_L1
								0x45DA, 0xC000,
								// REACTIVE_POWER_L2
								0x45DA, 0xC000,
								// REACTIVE_POWER_L3
								0x45DA, 0xC000,
								// REACTIVE_POWER
								0x45DA, 0xC000,

								// Dummy
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, //

								// FREQUENCY
								0x40A0, 0x0000)

						.withRegisters(19068,
								// ACTIVE_PRODUCTION_ENERGY
								0x464B, 0x2000,

								// DUMMY
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// REACTIVE_PRODUCTION_ENERGY
								0, 0));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.build()) //
				.next(InvertTest.testInvert(false))//
				.next(InvertTest.testEnergyInvert(false));
	}

	@Test
	public void testInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(true).build()) //
				.next(InvertTest.testInvert(true))//
				.next(InvertTest.testEnergyInvert(true));
	}
}