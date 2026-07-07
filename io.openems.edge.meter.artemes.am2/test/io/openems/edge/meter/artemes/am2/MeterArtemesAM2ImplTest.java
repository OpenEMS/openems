package io.openems.edge.meter.artemes.am2;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.test.InvertTest;

public class MeterArtemesAM2ImplTest {

	private ComponentTest basis;

	@Before
	public void setup() throws Exception {
		this.basis = new ComponentTest(new MeterArtemesAM2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withInputRegisters(0x0000,
								// Voltages L1, L2, L3
								0x0000, 0x03E8, // VOLTAGE_L1
								0x0000, 0x03E8, // VOLTAGE_L2
								0x0000, 0x03E8, // VOLTAGE_L3

								// Dummy registers 0x0006 - 0x000B
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// Voltage (Total)
								0x0000, 0x03E8, // VOLTAGE

								// Currents L1, L2, L3 (SignedDoubleword)
								0x0000, 0x03E8, // CURRENT_L1
								0x0000, 0x03E8, // CURRENT_L2
								0x0000, 0x03E8, // CURRENT_L3

								// Dummy registers 0x0014 - 0x0015
								0x0000, 0x0000,

								// Total Current (SignedDoubleword)
								0x0000, 0x0BB8, // CURRENT

								// Active Powers L1, L2, L3, Total (SignedQuadrupleword - 4 registers each)
								// 1000, 1096, 1203, 1328 (scale -3, meaning 1.000 = 1000000 in registers)

								0x0000, 0x0000, 0x0098, 0x9680, // ACTIVE_POWER_L1
								0x0000, 0x0000, 0x0098, 0x9680, // ACTIVE_POWER_L2
								0x0000, 0x0000, 0x0098, 0x9680, // ACTIVE_POWER_L3
								0x0000, 0x0000, 0x00C6, 0x5D40, // ACTIVE_POWER

								// Dummy registers 0x0028 - 0x0037
								0x0000, 0x0000, 0x0000, 0x0000, //
								0x0000, 0x0000, 0x0000, 0x0000, //
								0x0000, 0x0000, 0x0000, 0x0000, //
								0x0000, 0x0000, 0x0000, 0x0000,

								// Reactive Powers L1, L2, L3, Total (SignedQuadrupleword - 4 registers each)
								// 700, 732, 764, 796 (scale -3, meaning 1.000 = 1000000 in registers)

								0x0000, 0x0000, 0x006A, 0xCFC0, // REACTIVE_POWER_L1
								0x0000, 0x0000, 0x006A, 0xCFC0, // REACTIVE_POWER_L2
								0x0000, 0x0000, 0x006A, 0xCFC0, // REACTIVE_POWER_L3
								0x0000, 0x0000, 0x006A, 0xCFC0, // REACTIVE_POWER

								// Dummy registers 0x0048 - 0x0071
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x000, //
								0x0000, 0x0000,

								// Frequency
								0x0000, 0x1388)//
						.withInputRegisters(0x0418, //
								0x0000, 0x0000, 0x0001, 0xFBD0, //
								0x0000, 0x0000, 0x0000, 0x0000));
	}

	@Test
	public void invertTest() throws Exception {
		this.basis.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(true).build()) //
				.next(InvertTest.testInvert(true))//
				.next(InvertTest.testEnergyInvert(true));
	}

	@Test
	public void nonInvertTest() throws Exception {
		this.basis.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setInvert(false).build()) //
				.next(InvertTest.testInvert(false))//
				.next(InvertTest.testEnergyInvert(false));
	}

}
