package io.openems.edge.kostal.plenticore.pvinverter;

import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L3;
import static io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

/**
 * Tests PV Inverter with Modbus registers according to KOSTAL specification.
 *
 * Key registers: 152 (Frequency), 154-170 (Per-phase current/power/voltage),
 * 531 (Max apparent power), 1066 (Total AC active power)
 */
public class KostalPvInverterModbusTest {

	private static final String PV_INVERTER_ID = "pvInverter0";
	private static final String MODBUS_ID = "modbus0";

	private static ComponentTest newComponentTest(DummyModbusBridge modbus) throws Exception {
		return new ComponentTest(new KostalPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", modbus);
	}

	private static MyConfig createConfig() {
		return MyConfig.create() //
				.setId(PV_INVERTER_ID) //
				.setReadOnly(true) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(71) //
				.build();
	}

	@Test
	public void testReadFromModbus() throws Exception {
		var modbus = new DummyModbusBridge(MODBUS_ID) //
				.withRegisters(152, //
						0x0000, 0x4248, // Frequency 50.0 Hz
						0x0000, 0x4170, // Current L1 15.0 A
						0x8000, 0x453B, // Power L1 3000.0 W
						0x0000, 0x436B, // Voltage L1 235.0 V
						0x0000, 0x4170, // Current L2 15.0 A
						0xC000, 0x455A, // Power L2 3500.0 W
						0x0000, 0x436B, // Voltage L2 235.0 V
						0x0000, 0x4170, // Current L3 15.0 A
						0x0000, 0x452F) // Power L3 2800.0 W
				.withRegisters(170, 0x0000, 0x436B) // Voltage L3 235.0 V
				.withRegisters(531, 10000) // Max apparent power
				.withRegisters(1066, 0x5000, 0x4611); // Total power 9300.0 W

		newComponentTest(modbus) //
				.activate(createConfig()) //
				.next(new TestCase(), 3) //
				.next(new TestCase() //
						.output(FREQUENCY, 50000) //
						.output(CURRENT_L1, 15000) //
						.output(CURRENT_L2, 15000) //
						.output(CURRENT_L3, 15000) //
						.output(VOLTAGE_L1, 235000) //
						.output(VOLTAGE_L2, 235000) //
						.output(VOLTAGE_L3, 235000) //
						.output(ACTIVE_POWER, 9300) //
						.output(MAX_APPARENT_POWER, 10000))
				.deactivate();
	}

	@Test
	public void testHighProductionScenario() throws Exception {
		var modbus = new DummyModbusBridge(MODBUS_ID) //
				.withRegisters(152, //
						0x0000, 0x4248, 0x0000, 0x4170, //
						0x4000, 0x4583, // Power L1 4200W
						0x0000, 0x436B, 0x0000, 0x4170, //
						0x0000, 0x457A, // Power L2 4000W
						0x0000, 0x436B, 0x0000, 0x4170, //
						0x8000, 0x456D) // Power L3 3800W
				.withRegisters(170, 0x0000, 0x436B) //
				.withRegisters(531, 12000) //
				.withRegisters(1066, 0x8000, 0x463B); // Total 12000W

		newComponentTest(modbus) //
				.activate(createConfig()) //
				.next(new TestCase(), 3) //
				.next(new TestCase() //
						.output(ACTIVE_POWER, 12000))
				.deactivate();
	}

	@Test
	public void testLowProductionScenario() throws Exception {
		var modbus = new DummyModbusBridge(MODBUS_ID) //
				.withRegisters(152, //
						0x0000, 0x4248, 0x0000, 0x4120, //
						0x0000, 0x0000, //
						0x0000, 0x436B, 0x0000, 0x4120, //
						0x0000, 0x0000, //
						0x0000, 0x436B, 0x0000, 0x4120, //
						0x0000, 0x0000) //
				.withRegisters(170, 0x0000, 0x436B) //
				.withRegisters(531, 10000) //
				.withRegisters(1066, 0x0000, 0x4416); // Total 600W

		newComponentTest(modbus) //
				.activate(createConfig()) //
				.next(new TestCase(), 3) //
				.next(new TestCase() //
						.output(ACTIVE_POWER, 600))
				.deactivate();
	}
}
