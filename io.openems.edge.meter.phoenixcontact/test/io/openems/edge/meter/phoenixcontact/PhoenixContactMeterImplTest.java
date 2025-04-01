package io.openems.edge.meter.phoenixcontact;

import static io.openems.common.types.MeterType.PRODUCTION;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.InvertTest;

public class PhoenixContactMeterImplTest {

	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new PhoenixContactMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin())
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withRegisters(0x8006, //
								// VOLTAGE_L1
								0x0000, 0x3F80,
								// VOLTAGE_L2
								0x0000, 0x3F80,
								// VOLTAGE_L3
								0x0000, 0x3F80,
								// FREQUENCY
								0x0000, 0x40A0,
								// CURRENT_L1
								0x0000, 0x3F80,
								// CURRENT_L2
								0x0000, 0x3F80,
								// CURRENT_L3
								0x0000, 0x3F80,
								// CURRENT
								0x0001, 0x4040,
								// ACTIVE_POWER
								0x2000, 0x464B,

								// DUMMY
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// ACTIVE_POWER_L1
								0x4000, 0x461C,
								// ACTIVE_POWER_L2
								0x4000, 0x461C,
								// ACTIVE_POWER_L3
								0x4000, 0x461C,

								// DUMMY
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
								0x0000, 0x0000, 0x0000,
								// VOLTAGE
								0x0000, 0x3F80)//
						.withRegisters(0x8100,
								// ACTIVE_PRODUCTION_ENERGY
								0x2000, 0x464B,
								// DUMMY
								0x0000, 0x0000,
								// DUMMY
								0x0000, 0x0000,

								// REACTIVE_PRODUCTION_ENERGY
								0x0000, 0x0000)); //
	}

	@Test
	public void testInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setMeterType(PRODUCTION) //
						.build())//
				.next(new TestCase()//
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER, 7000))
				.next(InvertTest.testInvert(false))//
				.next(InvertTest.testEnergyInvert(false));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setMeterType(PRODUCTION) //
						.setInvert(true)//
						.build())//
				.next(new TestCase()//
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, -7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, -7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, -7000)
						.input(ElectricityMeter.ChannelId.REACTIVE_POWER, -7000))
				.next(InvertTest.testInvert(true))//
				.next(InvertTest.testEnergyInvert(true));
	}
}
