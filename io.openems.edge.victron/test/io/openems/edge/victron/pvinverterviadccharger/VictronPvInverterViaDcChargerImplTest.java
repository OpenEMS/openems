package io.openems.edge.victron.pvinverterviadccharger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.victron.enums.ChargeState;

/**
 * Tests for {@link VictronPvInverterViaDcChargerImpl}.
 */
public class VictronPvInverterViaDcChargerImplTest {

	private static final String CHARGER_ID = "charger0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(CHARGER_ID, "ActivePower");
	private static final ChannelAddress VOLTAGE = new ChannelAddress(CHARGER_ID, "Voltage");
	private static final ChannelAddress ACTIVE_PRODUCTION_ENERGY = new ChannelAddress(CHARGER_ID,
			"ActiveProductionEnergy");
	private static final ChannelAddress BATTERY_VOLTAGE = new ChannelAddress(CHARGER_ID, "BatteryVoltage");
	private static final ChannelAddress BATTERY_CURRENT = new ChannelAddress(CHARGER_ID, "BatteryCurrent");
	private static final ChannelAddress BATTERY_TEMPERATURE = new ChannelAddress(CHARGER_ID, "BatteryTemperature");
	private static final ChannelAddress CHARGE_STATE = new ChannelAddress(CHARGER_ID, "ChargeState");
	private static final ChannelAddress YIELD_TODAY = new ChannelAddress(CHARGER_ID, "YieldToday");
	private static final ChannelAddress MAX_CHARGE_POWER_TODAY = new ChannelAddress(CHARGER_ID, "MaxChargePowerToday");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronPvInverterViaDcChargerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(771,
								// BATTERY_VOLTAGE (register 771) - 5200 = 52.00V
								5200,
								// BATTERY_CURRENT (register 772) - 100 = 10.0A
								100,
								// BATTERY_TEMPERATURE (register 773) - 250 = 25.0°C
								250,
								// CHARGER_ON_OFF (register 774)
								1,
								// CHARGE_STATE (register 775) - Bulk charging
								3,
								// VOLTAGE (register 776) - 380V
								380)
						.withRegisters(778,
								// EQUALIZATION_PENDING (register 778)
								0,
								// EQUALIZATION_TIME_REMAINING (register 779)
								0,
								// RELAY_ON_THE_CHARGER (register 780)
								0,
								// DUMMY (register 781)
								0,
								// LOW_BATTERY_VOLTAGE_ALARM (register 782)
								0,
								// HIGH_BATTERY_VOLTAGE_ALARM (register 783)
								0,
								// YIELD_TODAY (register 784) - 500 = 50.0kWh
								500,
								// MAX_CHARGE_POWER_TODAY (register 785) - 3000W
								3000,
								// YIELD_YESTERDAY (register 786) - 450 = 45.0kWh
								450,
								// MAX_CHARGE_POWER_YESTERDAY (register 787) - 2800W
								2800,
								// ERROR_CODE (register 788)
								0,
								// ACTIVE_POWER (register 789) - 25000 = 2500.0W
								25000,
								// ACTIVE_PRODUCTION_ENERGY (register 790) - 1000kWh
								1000,
								// MPP_OPERATION_MODE (register 791)
								2)) //
						// Note: Registers 3700+ are in a separate Priority.LOW task
						// that is not executed in the first cycles, so we skip testing them
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setAlias("Victron BlueSolar DC Charger") //
						.setEnabled(true) //
						.setType(MeterType.PRODUCTION) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(229) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(ACTIVE_POWER, 2500) // 25000 / 10 (SCALE_FACTOR_MINUS_1) = 2500W
						.output(VOLTAGE, 3800) // 380 * 10 (SCALE_FACTOR_1) = 3800mV
						.output(ACTIVE_PRODUCTION_ENERGY, 100000L) // 1000 * 100 (SCALE_FACTOR_2) = 100000Wh
						.output(BATTERY_VOLTAGE, 52) // 5200 / 100 (SCALE_FACTOR_MINUS_2) = 52V
						.output(BATTERY_CURRENT, 10) // 100 / 10 (SCALE_FACTOR_MINUS_1) = 10A
						.output(BATTERY_TEMPERATURE, 25) // 250 / 10 (SCALE_FACTOR_MINUS_1) = 25°C
						.output(CHARGE_STATE, ChargeState.BULK) // Bulk charging (value 3)
						.output(YIELD_TODAY, 50) // 500 / 10 = 50kWh (SCALE_FACTOR_MINUS_1)
						.output(MAX_CHARGE_POWER_TODAY, 3000)); // 3000W
	}

	@Test
	public void testChannelIdCount() {
		var channelIds = VictronPvInverterViaDcCharger.ChannelId.values();
		assertNotNull(channelIds);
		assertEquals(true, channelIds.length > 0);
	}

	@Test
	public void testChannelIdDoc() {
		for (var channelId : VictronPvInverterViaDcCharger.ChannelId.values()) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(CHARGER_ID) //
				.setAlias("Victron BlueSolar DC Charger") //
				.setEnabled(true) //
				.setType(MeterType.PRODUCTION) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(229) //
				.build();

		assertEquals(CHARGER_ID, config.id());
		assertEquals("Victron BlueSolar DC Charger", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MeterType.PRODUCTION, config.type());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(229, config.modbusUnitId());
	}

	@Test
	public void testConstructor() throws OpenemsNamedException {
		var pvinverter = new VictronPvInverterViaDcChargerImpl();
		assertNotNull(pvinverter);
	}

	@Test
	public void testMeterType() throws OpenemsNamedException {
		// The meter type should always be PRODUCTION for this device
		var config = MyConfig.create() //
				.setId(CHARGER_ID) //
				.setType(MeterType.PRODUCTION) //
				.build();

		assertEquals(MeterType.PRODUCTION, config.type());
	}

}
