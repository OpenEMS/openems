package io.openems.edge.goodwe.batteryinverter;

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
import io.openems.edge.goodwe.charger.singlestring.GoodWeChargerPv1;
import io.openems.edge.goodwe.charger.twostring.GoodWeChargerTwoStringImpl;
import io.openems.edge.goodwe.charger.twostring.PvPort;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

public class GoodWeBatteryInverterImplTest {

	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY_ID = "battery0";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String CHARGER_ID = "charger0";
	private static final String CHARGER_2_ID = "charger1";
	private static final String CHARGER_3_ID = "charger2";
	private static final String CHARGER_4_ID = "charger3";
	private static final String CHARGER_5_ID = "charger4";
	private static final String CHARGER_6_ID = "charger5";
	private static final String SUM_ID = "_sum";

	private static final Battery BATTERY = new DummyBattery(BATTERY_ID);

	private static final ChannelAddress EMS_POWER_MODE = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerMode");
	private static final ChannelAddress EMS_POWER_SET = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerSet");
	private static final ChannelAddress METER_COMMUNICATE_STATUS = new ChannelAddress(BATTERY_INVERTER_ID,
			"MeterCommunicateStatus");
	private static final ChannelAddress MAX_AC_IMPORT = new ChannelAddress(BATTERY_INVERTER_ID, "MaxAcImport");
	private static final ChannelAddress MAX_AC_EXPORT = new ChannelAddress(BATTERY_INVERTER_ID, "MaxAcExport");
	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID, "GridActivePower");
	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(BATTERY_INVERTER_ID, "ActivePower");
	private static final ChannelAddress CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID, "ChargeMaxCurrent");
	private static final ChannelAddress WBMS_CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_INVERTER_ID,
			"WbmsChargeMaxCurrent");
	private static final ChannelAddress WBMS_DISCHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_INVERTER_ID,
			"WbmsDischargeMaxCurrent");
	private static final ChannelAddress WBMS_VOLTAGE = new ChannelAddress(BATTERY_INVERTER_ID, "WbmsVoltage");
	private static final ChannelAddress MAX_APPARENT_POWER = new ChannelAddress(BATTERY_INVERTER_ID,
			"MaxApparentPower");

	private static final ChannelAddress CHARGER_ACTUAL_POWER = new ChannelAddress(CHARGER_ID, "ActualPower");
	private static final ChannelAddress CHARGER_VOLTAGE = new ChannelAddress(CHARGER_ID, "Voltage");
	private static final ChannelAddress CHARGER_CURRENT = new ChannelAddress(CHARGER_ID, "Current");
	private static final ChannelAddress CHARGER_2_ACTUAL_POWER = new ChannelAddress(CHARGER_2_ID, "ActualPower");
	private static final ChannelAddress CHARGER_2_VOLTAGE = new ChannelAddress(CHARGER_2_ID, "Voltage");
	private static final ChannelAddress CHARGER_2_CURRENT = new ChannelAddress(CHARGER_2_ID, "Current");
	private static final ChannelAddress CHARGER_3_ACTUAL_POWER = new ChannelAddress(CHARGER_3_ID, "ActualPower");
	private static final ChannelAddress CHARGER_3_VOLTAGE = new ChannelAddress(CHARGER_3_ID, "Voltage");
	private static final ChannelAddress CHARGER_3_CURRENT = new ChannelAddress(CHARGER_3_ID, "Current");
	private static final ChannelAddress CHARGER_4_ACTUAL_POWER = new ChannelAddress(CHARGER_4_ID, "ActualPower");
	private static final ChannelAddress CHARGER_4_VOLTAGE = new ChannelAddress(CHARGER_4_ID, "Voltage");
	private static final ChannelAddress CHARGER_4_CURRENT = new ChannelAddress(CHARGER_4_ID, "Current");
	private static final ChannelAddress CHARGER_5_ACTUAL_POWER = new ChannelAddress(CHARGER_5_ID, "ActualPower");
	private static final ChannelAddress CHARGER_5_VOLTAGE = new ChannelAddress(CHARGER_5_ID, "Voltage");
	private static final ChannelAddress CHARGER_5_CURRENT = new ChannelAddress(CHARGER_5_ID, "Current");
	private static final ChannelAddress CHARGER_6_ACTUAL_POWER = new ChannelAddress(CHARGER_6_ID, "ActualPower");
	private static final ChannelAddress CHARGER_6_VOLTAGE = new ChannelAddress(CHARGER_6_ID, "Voltage");
	private static final ChannelAddress CHARGER_6_CURRENT = new ChannelAddress(CHARGER_6_ID, "Current");

	private static final ChannelAddress TWO_S_MPPT1_P = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt1P");
	private static final ChannelAddress TWO_S_MPPT1_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt1I");
	private static final ChannelAddress TWO_S_MPPT2_P = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt2P");
	private static final ChannelAddress TWO_S_MPPT2_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt2I");
	private static final ChannelAddress TWO_S_MPPT3_P = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt3P");
	private static final ChannelAddress TWO_S_MPPT3_I = new ChannelAddress(BATTERY_INVERTER_ID, "TwoSMppt3I");
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

	@Test
	public void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.singlestring.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.input(CHARGER_ACTUAL_POWER, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

	@Test
	public void testNegativSetActivePoint() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, -1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

	@Test
	public void testDischargeBattery() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.DISCHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

	@Test
	public void testEmsPowerModeAutoWithBalancing() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(GRID_ACTIVE_POWER, 2000) //
						.input(ACTIVE_POWER, 4000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 6000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testEmsPowerModeAutoWithSurplus() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.singlestring.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger) //
				.addComponent(BATTERY) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(CHARGER_ACTUAL_POWER, 10000) //
						.input(CHARGE_MAX_CURRENT, 20).onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 10000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testEmsPowerModeAutoWithMaxAcImport() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_IMPORT, 3000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 3000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testEmsPowerModeAutoWithMaxAcExport() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_EXPORT, 8000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 8000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testBatteryIsFull() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_IMPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 0, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testBatteryIsEmpty() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
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
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 0, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testAcCalculation() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(BATTERY).activate(MyConfig.create() //
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
						.input(WBMS_CHARGE_MAX_CURRENT, 0) //
						.input(WBMS_DISCHARGE_MAX_CURRENT, 1) //
						.input(WBMS_VOLTAGE, 325) //
						.input(MAX_APPARENT_POWER, 10000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(BATTERY, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, 0) //
						.output(MAX_AC_EXPORT, 325));
	}

	@Test
	public void testTwoStringCharger() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerTwoStringImpl();
		var charger2 = new GoodWeChargerTwoStringImpl();
		var charger3 = new GoodWeChargerTwoStringImpl();
		var charger4 = new GoodWeChargerTwoStringImpl();
		var charger5 = new GoodWeChargerTwoStringImpl();
		var charger6 = new GoodWeChargerTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_1) //
						.build());

		new ComponentTest(charger2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_2_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_2) //
						.build());

		new ComponentTest(charger3) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_3_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_3) //
						.build());

		new ComponentTest(charger4) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_4_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_4) //
						.build());

		new ComponentTest(charger5) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_5_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_5) //
						.build());

		new ComponentTest(charger6) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId(CHARGER_6_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setPvPort(PvPort.PV_6) //
						.build());

		ess.addCharger(charger1);
		ess.addCharger(charger2);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(charger2) //
				.addComponent(BATTERY) //
				.activate(MyConfig.create() //
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
						.input(TWO_S_MPPT1_I, 20) //
						.input(TWO_S_MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 10) //
						.input(TWO_S_PV2_I, 10) //
						.input(TWO_S_PV1_V, 240) //
						.input(TWO_S_PV2_V, 240) //

						// Values applied in the next cycle
						.output(CHARGER_ACTUAL_POWER, 0) //
						.output(CHARGER_2_ACTUAL_POWER, 0) //
						.output(CHARGER_CURRENT, null) //
						.output(CHARGER_2_CURRENT, null) //
						.output(CHARGER_VOLTAGE, null) //
						.output(CHARGER_2_VOLTAGE, null)) //
				.next(new TestCase() //
						.output(CHARGER_ACTUAL_POWER, 1000) //
						.output(CHARGER_2_ACTUAL_POWER, 1000) //
						.output(CHARGER_CURRENT, 10) //
						.output(CHARGER_2_CURRENT, 10) //
						.output(CHARGER_VOLTAGE, 240) //
						.output(CHARGER_2_VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(TWO_S_MPPT1_I, 20) //
						.input(TWO_S_MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 5) //
						.input(TWO_S_PV2_I, 15) //
						.output(CHARGER_ACTUAL_POWER, 1000) //
						.output(CHARGER_2_ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output(CHARGER_ACTUAL_POWER, 500) //
						.output(CHARGER_2_ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(TWO_S_MPPT1_I, 20) //
						.input(TWO_S_MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 20) //
						.input(TWO_S_PV2_I, 0) //
						.output(CHARGER_ACTUAL_POWER, 500) //
						.output(CHARGER_2_ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output(CHARGER_ACTUAL_POWER, 2000) //
						.output(CHARGER_2_ACTUAL_POWER, 0) //
				);

		/*
		 * Test MPPT 2 - PV3 & PV4
		 */
		ess.addCharger(charger3);
		ess.addCharger(charger4);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger3) //
				.addComponent(charger4) //
				.addComponent(BATTERY) //
				.activate(MyConfig.create() //
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
						.input(TWO_S_MPPT2_I, 20) //
						.input(TWO_S_MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 10) //
						.input(TWO_S_PV4_I, 10) //
						.input(TWO_S_PV3_V, 240) //
						.input(TWO_S_PV4_V, 240) //

						// Values applied in the next cycle
						.output(CHARGER_3_ACTUAL_POWER, 0) //
						.output(CHARGER_4_ACTUAL_POWER, 0) //
						.output(CHARGER_3_CURRENT, null) //
						.output(CHARGER_4_CURRENT, null) //
						.output(CHARGER_3_VOLTAGE, null) //
						.output(CHARGER_4_VOLTAGE, null)) //
				.next(new TestCase() //
						.output(CHARGER_3_ACTUAL_POWER, 1000) //
						.output(CHARGER_4_ACTUAL_POWER, 1000) //
						.output(CHARGER_3_CURRENT, 10) //
						.output(CHARGER_4_CURRENT, 10) //
						.output(CHARGER_3_VOLTAGE, 240) //
						.output(CHARGER_4_VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(TWO_S_MPPT2_I, 20) //
						.input(TWO_S_MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 5) //
						.input(TWO_S_PV4_I, 15) //
						.output(CHARGER_3_ACTUAL_POWER, 1000) //
						.output(CHARGER_4_ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output(CHARGER_3_ACTUAL_POWER, 500) //
						.output(CHARGER_4_ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(TWO_S_MPPT2_I, 20) //
						.input(TWO_S_MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 20) //
						.input(TWO_S_PV4_I, 0) //
						.output(CHARGER_3_ACTUAL_POWER, 500) //
						.output(CHARGER_4_ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output(CHARGER_3_ACTUAL_POWER, 2000) //
						.output(CHARGER_4_ACTUAL_POWER, 0) //
				);

		/*
		 * Test MPPT 3 - PV5 & PV6
		 */
		ess.addCharger(charger5);
		ess.addCharger(charger6);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger5) //
				.addComponent(charger6) //
				.addComponent(BATTERY) //
				.activate(MyConfig.create() //
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
						.input(TWO_S_MPPT3_I, 20) //
						.input(TWO_S_MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 10) //
						.input(TWO_S_PV6_I, 10) //
						.input(TWO_S_PV5_V, 240) //
						.input(TWO_S_PV6_V, 240) //

						// Values applied in the next cycle
						.output(CHARGER_5_ACTUAL_POWER, 0) //
						.output(CHARGER_6_ACTUAL_POWER, 0) //
						.output(CHARGER_5_CURRENT, null) //
						.output(CHARGER_6_CURRENT, null) //
						.output(CHARGER_5_VOLTAGE, null) //
						.output(CHARGER_6_VOLTAGE, null)) //
				.next(new TestCase() //
						.output(CHARGER_5_ACTUAL_POWER, 1000) //
						.output(CHARGER_6_ACTUAL_POWER, 1000) //
						.output(CHARGER_5_CURRENT, 10) //
						.output(CHARGER_6_CURRENT, 10) //
						.output(CHARGER_5_VOLTAGE, 240) //
						.output(CHARGER_6_VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(TWO_S_MPPT3_I, 20) //
						.input(TWO_S_MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 5) //
						.input(TWO_S_PV6_I, 15) //
						.output(CHARGER_5_ACTUAL_POWER, 1000) //
						.output(CHARGER_6_ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output(CHARGER_5_ACTUAL_POWER, 500) //
						.output(CHARGER_6_ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(TWO_S_MPPT3_I, 20) //
						.input(TWO_S_MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 20) //
						.input(TWO_S_PV6_I, 0) //
						.output(CHARGER_5_ACTUAL_POWER, 500) //
						.output(CHARGER_6_ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output(CHARGER_5_ACTUAL_POWER, 2000) //
						.output(CHARGER_6_ACTUAL_POWER, 0) //
				);
	}
}
