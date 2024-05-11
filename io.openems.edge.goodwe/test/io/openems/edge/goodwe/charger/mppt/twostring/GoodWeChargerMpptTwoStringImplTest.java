package io.openems.edge.goodwe.charger.mppt.twostring;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.goodwe.GoodWeConstants;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

public class GoodWeChargerMpptTwoStringImplTest {

	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY_ID = "battery0";
	private static final String CHARGER_1_ID = "charger0";
	private static final String CHARGER_2_ID = "charger1";
	private static final String CHARGER_3_ID = "charger2";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";

	private static final Battery BATTERY = new DummyBattery(BATTERY_ID);

	private static final ChannelAddress MPPT1_P = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt1P");
	private static final ChannelAddress MPPT1_I = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt1I");
	private static final ChannelAddress MPPT2_P = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt2P");
	private static final ChannelAddress MPPT2_I = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt2I");
	private static final ChannelAddress MPPT3_P = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt3P");
	private static final ChannelAddress MPPT3_I = new ChannelAddress(BATTERY_INVERTER_ID, "Mppt3I");
	private static final ChannelAddress TWO_S_PV1_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv1I");
	private static final ChannelAddress TWO_S_PV1_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv1V");
	private static final ChannelAddress TWO_S_PV2_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv2I");
	private static final ChannelAddress TWO_S_PV2_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv2V");
	private static final ChannelAddress TWO_S_PV3_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv3I");
	private static final ChannelAddress TWO_S_PV3_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv3V");
	private static final ChannelAddress TWO_S_PV4_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv4I");
	private static final ChannelAddress TWO_S_PV4_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv4V");
	private static final ChannelAddress TWO_S_PV5_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv5I");
	private static final ChannelAddress TWO_S_PV5_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv5V");
	private static final ChannelAddress TWO_S_PV6_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv6I");
	private static final ChannelAddress TWO_S_PV6_V = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSPv6V");
	private static final ChannelAddress CHARGER_1_ACTUAL_POWER = new ChannelAddress(CHARGER_1_ID, "ActualPower");
	private static final ChannelAddress CHARGER_1_VOLTAGE = new ChannelAddress(CHARGER_1_ID, "Voltage");
	private static final ChannelAddress CHARGER_1_CURRENT = new ChannelAddress(CHARGER_1_ID, "Current");
	private static final ChannelAddress CHARGER_2_ACTUAL_POWER = new ChannelAddress(CHARGER_2_ID, "ActualPower");
	private static final ChannelAddress CHARGER_2_VOLTAGE = new ChannelAddress(CHARGER_2_ID, "Voltage");
	private static final ChannelAddress CHARGER_2_CURRENT = new ChannelAddress(CHARGER_2_ID, "Current");
	private static final ChannelAddress CHARGER_3_ACTUAL_POWER = new ChannelAddress(CHARGER_3_ID, "ActualPower");
	private static final ChannelAddress CHARGER_3_VOLTAGE = new ChannelAddress(CHARGER_3_ID, "Voltage");
	private static final ChannelAddress CHARGER_3_CURRENT = new ChannelAddress(CHARGER_3_ID, "Current");

	@Test
	public void test() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerMpptTwoStringImpl();
		var charger2 = new GoodWeChargerMpptTwoStringImpl();
		var charger3 = new GoodWeChargerMpptTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(MyConfig.create() //
						.setId(CHARGER_1_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setMpptPort(MpptPort.MPPT_1) //
						.build());

		new ComponentTest(charger2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(MyConfig.create() //
						.setId(CHARGER_2_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setMpptPort(MpptPort.MPPT_2) //
						.build());

		new ComponentTest(charger3) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(MyConfig.create() //
						.setId(CHARGER_3_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setMpptPort(MpptPort.MPPT_3) //
						.build());

		ess.addCharger(charger1);
		ess.addCharger(charger2);
		ess.addCharger(charger3);

		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(charger2) //
				.addComponent(charger3) //
				.addComponent(BATTERY) //
				.activate(io.openems.edge.goodwe.batteryinverter.MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.build()) //
				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 10) //
						.input(TWO_S_PV2_I, 10) //
						.input(TWO_S_PV1_V, 240) //
						.input(TWO_S_PV2_V, 240)) //
				// Values applied in the next cycle
				.next(new TestCase() //
						.output(CHARGER_1_ACTUAL_POWER, 2000) //
						.output(CHARGER_1_CURRENT, 20) //
						.output(CHARGER_1_VOLTAGE, 240) //
						.output(CHARGER_2_ACTUAL_POWER, null) //
						.output(CHARGER_2_CURRENT, null) //
						.output(CHARGER_2_VOLTAGE, null) //
						.output(CHARGER_3_ACTUAL_POWER, null) //
						.output(CHARGER_3_CURRENT, null) //
						.output(CHARGER_3_VOLTAGE, null) //
				) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 3000) //
						.input(TWO_S_PV1_I, 5) //
						.input(TWO_S_PV2_I, 15) //
						.input(TWO_S_PV1_V, 250) //
						.input(TWO_S_PV2_V, 250)) //
				.next(new TestCase() //
						.output(CHARGER_1_ACTUAL_POWER, 3000) //
						.output(CHARGER_1_CURRENT, 20) //
						.output(CHARGER_1_VOLTAGE, 250) //
						.output(CHARGER_2_ACTUAL_POWER, null) //
						.output(CHARGER_2_CURRENT, null) //
						.output(CHARGER_2_VOLTAGE, null). //
						output(CHARGER_3_ACTUAL_POWER, null) //
						.output(CHARGER_3_CURRENT, null) //
						.output(CHARGER_3_VOLTAGE, null) //
				)

				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 2000) //
						.input(MPPT2_I, 30) //
						.input(MPPT2_P, 3000) //
						.input(MPPT3_I, 40) //
						.input(MPPT3_P, 4000) //
						.input(TWO_S_PV1_I, 10) //
						.input(TWO_S_PV1_V, 250) //
						.input(TWO_S_PV2_I, 10) //
						.input(TWO_S_PV2_V, 250) //
						.input(TWO_S_PV3_I, 15) //
						.input(TWO_S_PV3_V, 280) //
						.input(TWO_S_PV4_I, 15) //
						.input(TWO_S_PV4_V, 280) //
						.input(TWO_S_PV5_I, 20) //
						.input(TWO_S_PV5_V, 299) //
						.input(TWO_S_PV6_I, 20) //
						.input(TWO_S_PV6_V, 299)) //
				.next(new TestCase() //
						.output(CHARGER_1_ACTUAL_POWER, 2000) //
						.output(CHARGER_1_CURRENT, 20) //
						.output(CHARGER_1_VOLTAGE, 250) //
						.output(CHARGER_2_ACTUAL_POWER, 3000) //
						.output(CHARGER_2_CURRENT, 30) //
						.output(CHARGER_2_VOLTAGE, 280). //
						output(CHARGER_3_ACTUAL_POWER, 4000) //
						.output(CHARGER_3_CURRENT, 40) //
						.output(CHARGER_3_VOLTAGE, 299) //
				);
	}
}
