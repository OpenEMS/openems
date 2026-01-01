package io.openems.edge.meter.janitza.umg806;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.test.InvertTest;

public class MeterJanitzaUmg806ImplTest {

	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new MeterJanitzaUmg806Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")
					    // ---- 19000–19010 Voltages ----
					    .withRegisters(19000,
					        0x3F80,0x0000, // U_L1
					        0x3F80,0x0000, // U_L2
					        0x3F80,0x0000, // U_L3
					        
					        0x0000,0x0000, // dummy 19006
					        0x0000,0x0000, // dummy 19008
					        0x0000,0x0000,  // dummy 19010

					    // ---- 19012–19018 Currents ----
					        0x3F80,0x0000, // I_L1
					        0x3F80,0x0000, // I_L2
					        0x3F80,0x0000, // I_L3
					        
					        0x0000,0x0000,  // dummy 19018

					    // ---- 19020–19034 Active Power ----
					        0x461C,0x4000, // P_L1
					        0x461C,0x4000, // P_L2
					        0x461C,0x4000, // P_L3
					        0x464B,0x2000, // P_sum
					        
					        // dummy 19028–19034
					        0x0000,0x0000,
					        0x0000,0x0000,
					        0x0000,0x0000,
					        0x0000,0x0000,

					    // ---- 19036–19042 Reactive Power ----
					        0x45DA,0xC000, // Q_L1
					        0x45DA,0xC000, // Q_L2
					        0x45DA,0xC000, // Q_L3
					        0x45DA,0xC000  // Q_sum
					    )

					    // ---- 19050 Frequency ----
					    .withRegisters(19050,
					        0x40A0,0x0000
					    )

					    // ---- 19062–19070 Energy ----
					    .withRegisters(19062,
					        // production / consumption depending on invert
					        0x464B,0x2000,   // 19062 P_PROD
					        0x0000,0x0000,   // 19064 dummy
					        0x0000,0x0000,   // 19066 dummy
					        0x0000,0x0000,   // 19068 dummy
					        0x2634,0x2000    // 19070 P_CONS
					    )
					);
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