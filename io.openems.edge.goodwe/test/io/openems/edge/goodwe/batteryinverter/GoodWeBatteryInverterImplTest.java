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
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;
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
	private static final String SUM_ID = "_sum";

	private static final Battery BATTERY = new DummyBattery(BATTERY_ID);

	private static final ChannelAddress EMS_POWER_MODE = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerMode");
	private static final ChannelAddress EMS_POWER_SET = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerSet");
	private static final ChannelAddress ACTUAL_POWER = new ChannelAddress(CHARGER_ID, "ActualPower");
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

	@Test
	public void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
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
						.input(ACTUAL_POWER, 2000) //
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
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
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
						.input(ACTUAL_POWER, 10000) //
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

}
