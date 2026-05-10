package io.openems.edge.dccharger.victron;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.victron.dccharger.VictronDcCharger;
import io.openems.edge.victron.dccharger.VictronDcChargerImpl;

public class VictronDcChargerImplTest {

	private static final String CHARGER_ID = "charger0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress ACTUAL_POWER = new ChannelAddress(CHARGER_ID, "ActualPower");
	private static final ChannelAddress VOLTAGE = new ChannelAddress(CHARGER_ID, "Voltage");
	private static final ChannelAddress CURRENT = new ChannelAddress(CHARGER_ID, "Current");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronDcChargerImpl()) //
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
								// CHARGE_STATE (register 775)
								3,
								// VOLTAGE (register 776)
								380,
								// CURRENT (register 777)
								15)
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
								// MAX_CHARGE_POWER_TODAY (register 785)
								3000,
								// YIELD_YESTERDAY (register 786) - 450 = 45.0kWh
								450,
								// MAX_CHARGE_POWER_YESTERDAY (register 787)
								2800,
								// ERROR_CODE (register 788)
								0,
								// ACTUAL_POWER (register 789) - 25000 = 2500.0W
								25000,
								// ACTUAL_ENERGY (register 790)
								100,
								// MPP_OPERATION_MODE (register 791)
								2)
						.withRegisters(3700,
								// PV_VOLTAGE_TRACKER_0 (register 3700) - 38000 = 380.00V
								38000,
								// PV_VOLTAGE_TRACKER_1 (register 3701)
								0,
								// PV_VOLTAGE_TRACKER_2 (register 3702)
								0,
								// PV_VOLTAGE_TRACKER_3 (register 3703)
								0)
						.withRegisters(3708,
								// YIELD_TODAY_TRACKER_0 (register 3708)
								500,
								// YIELD_TODAY_TRACKER_1 (register 3709)
								0,
								// YIELD_TODAY_TRACKER_2 (register 3710)
								0,
								// YIELD_TODAY_TRACKER_3 (register 3711)
								0,
								// YIELD_YESTERDAY_TRACKER_0 (register 3712)
								450,
								// YIELD_YESTERDAY_TRACKER_1 (register 3713)
								0,
								// YIELD_YESTERDAY_TRACKER_2 (register 3714)
								0,
								// YIELD_YESTERDAY_TRACKER_3 (register 3715)
								0,
								// MAX_CHARGE_POWER_TODAY_TRACKER_0 (register 3716)
								3000,
								// MAX_CHARGE_POWER_TODAY_TRACKER_1 (register 3717)
								0,
								// MAX_CHARGE_POWER_TODAY_TRACKER_2 (register 3718)
								0,
								// MAX_CHARGE_POWER_TODAY_TRACKER_3 (register 3719)
								0,
								// MAX_CHARGE_POWER_YESTERDAY_TRACKER_0 (register 3720)
								2800,
								// MAX_CHARGE_POWER_YESTERDAY_TRACKER_1 (register 3721)
								0,
								// MAX_CHARGE_POWER_YESTERDAY_TRACKER_2 (register 3722)
								0,
								// MAX_CHARGE_POWER_YESTERDAY_TRACKER_3 (register 3723)
								0)) //
				.activate(MyConfig.create() //
						.setId(CHARGER_ID) //
						.setAlias("Victron BlueSolar") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(229) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(ACTUAL_POWER, 2500) // 25000 / 10 (SCALE_FACTOR_MINUS_1) = 2500W
						.output(VOLTAGE, 3800) // 380 * 10 (SCALE_FACTOR_1) = 3800mV
						.output(CURRENT, 150)); // 15 * 10 (SCALE_FACTOR_1) = 150mA
	}

	@Test
	public void testChannelIds() {
		var channelIds = VictronDcCharger.ChannelId.values();
		for (var channelId : channelIds) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testDebugLog() throws Exception {
		var charger = new VictronDcChargerImpl();
		assertNotNull(charger);
	}

}
