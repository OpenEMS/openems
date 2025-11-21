package io.openems.edge.kostal.plenticore.gridmeter;

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

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

/**
 * Tests GridMeter with Modbus registers according to KOSTAL specification.
 *
 * Key registers (viaInverter=true): 220-251 (Per-phase frequency/current/power/voltage),
 * 252 (Total active power), 254 (Total reactive power)
 */
public class KostalGridMeterModbusTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void testReadFromModbusViaInverter() throws Exception {
		var modbus = new DummyModbusBridge(MODBUS_ID) //
				.withRegisters(220, //
						0x0000, 0x4248, // Frequency 50.0 Hz
						0x0000, 0x4120, // Current L1 10.0 A
						0x4000, 0x451C, // Power L1 2500.0 W
						0x0000, 0x0000, // Reactive power L1
						0x0000, 0x0000, // Dummy
						0x0000, 0x4366, // Voltage L1 230.0 V
						0x0000, 0x4120, // Current L2 10.0 A
						0x4000, 0x451C, // Power L2 2500.0 W
						0x0000, 0x0000, // Reactive power L2
						0x0000, 0x0000, // Dummy
						0x0000, 0x4366, // Voltage L2 230.0 V
						0x0000, 0x4120, // Current L3 10.0 A
						0x4000, 0x451C, // Power L3 2500.0 W
						0x0000, 0x0000, // Reactive power L3
						0x0000, 0x0000, // Dummy
						0x0000, 0x4366, // Voltage L3 230.0 V
						0x6000, 0x45EA, // Total power 7500.0 W
						0x0000, 0x0000); // Total reactive power

		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", modbus) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(71) //
						.setType(MeterType.GRID) //
						.setViaInverter(true) //
						.setWordwrap(true) //
						.build()) //
				.next(new TestCase(), 3) //
				.next(new TestCase() //
						.output(FREQUENCY, 50000) //
						.output(CURRENT_L1, 10000) //
						.output(CURRENT_L2, 10000) //
						.output(CURRENT_L3, 10000) //
						.output(VOLTAGE_L1, 230000) //
						.output(VOLTAGE_L2, 230000) //
						.output(VOLTAGE_L3, 230000) //
						.output(ACTIVE_POWER_L1, 2500) //
						.output(ACTIVE_POWER_L2, 2500) //
						.output(ACTIVE_POWER_L3, 2500) //
						.output(ACTIVE_POWER, 7500))
				.deactivate();
	}
}
