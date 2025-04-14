package io.openems.edge.meter.carlo.gavazzi.em300;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.test.InvertTest;

public class MeterCarloGavazziEm300ImplTest {

	final int offset = 300000 + 1;
	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new MeterCarloGavazziEm300Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withInputRegisters(300001 - this.offset, //
								// VOLTAGE_L1
								10, 0x0000,
								// VOLTAGE_L2
								10, 0x0000,
								// VOLTAGE_L3
								10, 0x0000)
						.withInputRegisters(300013 - this.offset, //
								// CURRENT_L1
								1000, 0x0000,
								// CURRENT_L2
								1000, 0x0000,
								// CURRENT_L3
								1000, 0x0000,
								// ACTIVE_POWER_L1
								0x86A0, 0x0001,
								// ACTIVE_POWER_L2
								0x86A0, 0x0001,
								// ACTIVE_POWER_L3
								0x86A0, 0x0001,

								// apparentpower not tested here
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// REACTIVE_POWER_L1
								0x1170, 0x0001,
								// REACTIVE_POWER_L2
								0x1170, 0x0001,
								// REACTIVE_POWER_L3
								0x1170, 0x0001,

								// Dummy Registers
								0x0000, 0x0000, 0x0000, 0x0000,

								// ACTIVE_POWER
								0xFBD0, 0x0001,

								// apparentpower not tested here
								0x0000, 0x0000,

								// REACTIVE_POWER
								0x1170, 0x0001)
						.withInputRegisters(300052 - this.offset, //
								50, //
								130, 0, 130, 0, 130, 0, //
								130, 0, 130, 0, 130, 0, //
								130, 0, 130, 0, 130, 0, //
								130, 0, 130, 0, 130, 0, //
								130, 0, 0, 0)) //

		;
	}

	@Test
	public void testInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setInvert(true).setType(GRID) //
						.build()); //
		this.test.next(new TestCase());
		this.test.next(InvertTest.testInvert(true));
		this.test.next(InvertTest.testEnergyInvert(true));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setInvert(false).setType(GRID) //
						.build()); //
		this.test.next(new TestCase());
		this.test.next(InvertTest.testInvert(false));
		this.test.next(InvertTest.testEnergyInvert(false));
	}
}