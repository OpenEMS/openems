package io.openems.edge.meter.inepro.pro380modct;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class Pro380modctImplTest {

	private ComponentTest test;

	@Before
	public void setup() throws Exception {
		this.test = new ComponentTest(new Pro380modctImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(0x5002,
								// VOLTAGE + FREQUENCY
								0x4366, 0x0000, // VOLTAGE_L1 230.0 V -> 230000 mV
								0x4367, 0x0000, // VOLTAGE_L2 231.0 V -> 231000 mV
								0x4368, 0x0000, // VOLTAGE_L3 232.0 V -> 232000 mV
								0x4248, 0x6666, // FREQUENCY 50.1 Hz -> 50100 mHz

								// CURRENT
								0x4128, 0x0000, // CURRENT 10.5 A -> 10500 mA
								0x4138, 0x0000, // CURRENT_L1 11.5 A -> 11500 mA
								0x4148, 0x0000, // CURRENT_L2 12.5 A -> 12500 mA
								0x4131, 0x999a, // CURRENT_L3 11.1 A -> 11100 mA

								// ACTIVE POWER
								0x3fc0, 0x0000, // ACTIVE_POWER 1.5 kW -> 1500 W
								0x3fcc, 0xcccd, // ACTIVE_POWER_L1 1.6 kW -> 1600 W
								0x3fd9, 0x999a, // ACTIVE_POWER_L2 1.7 kW -> 1700 W
								0x4020, 0x0000, // ACTIVE_POWER_L3 2.5 kW -> 2500 W

								// REACTIVE POWER
								0x4026, 0x6666, // REACTIVE_POWER 2.6 kvar -> 2600 var
								0x402c, 0xcccd, // REACTIVE_POWER_L1 2.7 kvar -> 2700 var
								0x3fc0, 0x0000, // REACTIVE_POWER_L2 1.5 kvar -> 1500 var
								0x3fd9, 0x999a, // REACTIVE_POWER_L3 1.7 kvar -> 1700 var

								// APPARENT POWER
								0x4143, 0x3333, // APPARENT_POWER 12.2 kVA -> 12200 VA
								0x4154, 0xcccd, // APPARENT_POWER_L1 13.3 kVA -> 13300 VA
								0x4148, 0x0000, // APPARENT_POWER_L2 12.5 kVA -> 12500 VA
								0x4138, 0x0000 // APPARENT_POWER_L3 11.5 kVA -> 11500 VA
						));
	}

	@Test
	public void testMapping() throws Exception {
		this.test.activate(MyConfig.create() //
				.setId("meter0") //
				.setModbusId("modbus0") //
				.setModbusUnitId(1) //
				.setType(MeterType.GRID) //
				.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 232000) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50100) //
						.output(ElectricityMeter.ChannelId.CURRENT, 10500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 11500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 12500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 11100) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1500) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1600) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1700) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 2500) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 2600) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 2700) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 1500) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 1700) //
						.output(Pro380modct.ChannelId.APPARENT_POWER, 12200) //
						.output(Pro380modct.ChannelId.APPARENT_POWER_L1, 13300) //
						.output(Pro380modct.ChannelId.APPARENT_POWER_L2, 12500) //
						.output(Pro380modct.ChannelId.APPARENT_POWER_L3, 11500));
	}

	@Test
	public void testMeterType() throws Exception {
		var component = new Pro380modctImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setType(MeterType.PRODUCTION) //
						.build());

		assertEquals(MeterType.PRODUCTION, component.getMeterType());
	}
}
