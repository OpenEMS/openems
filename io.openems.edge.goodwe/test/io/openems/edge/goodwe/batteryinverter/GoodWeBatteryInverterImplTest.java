package io.openems.edge.goodwe.batteryinverter;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.ACTUAL_POWER;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.CURRENT;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.VOLTAGE;
import static io.openems.edge.goodwe.GoodWeConstants.DEFAULT_UNIT_ID;
import static io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.doSetBmsVoltage;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.EMS_POWER_MODE;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.EMS_POWER_SET;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MAX_AC_EXPORT;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MAX_AC_IMPORT;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.METER_COMMUNICATE_STATUS;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT1_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT1_P;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT2_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT2_P;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT3_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.MPPT3_P;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV1_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV1_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV2_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV2_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV3_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV3_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV4_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV4_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV5_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV5_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV6_I;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.TWO_S_PV6_V;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.WBMS_VOLTAGE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.goodwe.charger.singlestring.GoodWeChargerPv1;
import io.openems.edge.goodwe.charger.twostring.GoodWeChargerTwoStringImpl;
import io.openems.edge.goodwe.charger.twostring.PvPort;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

@SuppressWarnings("deprecation")
public class GoodWeBatteryInverterImplTest {

	@Test
	public void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.goodwe.charger.singlestring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.input("charger0", ACTUAL_POWER, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 1000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), -1000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, 0) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 1000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(GRID_ACTIVE_POWER, 2000) //
						.input(ACTIVE_POWER, 4000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 6000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.AUTO) //
						.output(EMS_POWER_SET, 0));
	}

	@Test
	public void testEmsPowerModeAutoWithSurplus() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(io.openems.edge.goodwe.charger.singlestring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.build());

		var ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger) //
				.addComponent(new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input("charger0", ACTUAL_POWER, 10000) //
						.input("battery0", CHARGE_MAX_CURRENT, 20).onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 10000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_IMPORT, 3000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 3000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_EXPORT, 8000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 8000, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_IMPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 0, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(METER_COMMUNICATE_STATUS, MeterCommunicateStatus.OK) //
						.input(MAX_AC_EXPORT, 0) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 0, 0);
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
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyBattery("battery0")).activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(WBMS_CHARGE_MAX_CURRENT, 0) //
						.input(WBMS_DISCHARGE_MAX_CURRENT, 1) //
						.input(WBMS_VOLTAGE, 325) //
						.input(MAX_APPARENT_POWER, 10000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 0, 0);
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
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_1) //
						.build());

		new ComponentTest(charger2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger1") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_2) //
						.build());

		new ComponentTest(charger3) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger2") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_3) //
						.build());

		new ComponentTest(charger4) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger3") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_4) //
						.build());

		new ComponentTest(charger5) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger4") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_5) //
						.build());

		new ComponentTest(charger6) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger5") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_6) //
						.build());

		ess.addCharger(charger1);
		ess.addCharger(charger2);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(charger2) //
				.addComponent(new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 10) //
						.input(TWO_S_PV2_I, 10) //
						.input(TWO_S_PV1_V, 240) //
						.input(TWO_S_PV2_V, 240) //

						// Values applied in the next cycle
						.output("charger0", ACTUAL_POWER, 0) //
						.output("charger1", ACTUAL_POWER, 0) //
						.output("charger0", CURRENT, null) //
						.output("charger1", CURRENT, null) //
						.output("charger0", VOLTAGE, null) //
						.output("charger1", VOLTAGE, null)) //
				.next(new TestCase() //
						.output("charger0", ACTUAL_POWER, 1000) //
						.output("charger1", ACTUAL_POWER, 1000) //
						.output("charger0", CURRENT, 10) //
						.output("charger1", CURRENT, 10) //
						.output("charger0", VOLTAGE, 240) //
						.output("charger1", VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 5) //
						.input(TWO_S_PV2_I, 15) //
						.output("charger0", ACTUAL_POWER, 1000) //
						.output("charger1", ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output("charger0", ACTUAL_POWER, 500) //
						.output("charger1", ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(MPPT1_I, 20) //
						.input(MPPT1_P, 2000) //
						.input(TWO_S_PV1_I, 20) //
						.input(TWO_S_PV2_I, 0) //
						.output("charger0", ACTUAL_POWER, 500) //
						.output("charger1", ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output("charger0", ACTUAL_POWER, 2000) //
						.output("charger1", ACTUAL_POWER, 0) //
				);

		/*
		 * Test MPPT 2 - PV3 & PV4
		 */
		ess.addCharger(charger3);
		ess.addCharger(charger4);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger3) //
				.addComponent(charger4) //
				.addComponent(new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(MPPT2_I, 20) //
						.input(MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 10) //
						.input(TWO_S_PV4_I, 10) //
						.input(TWO_S_PV3_V, 240) //
						.input(TWO_S_PV4_V, 240) //

						// Values applied in the next cycle
						.output("charger2", ACTUAL_POWER, 0) //
						.output("charger3", ACTUAL_POWER, 0) //
						.output("charger2", CURRENT, null) //
						.output("charger3", CURRENT, null) //
						.output("charger2", VOLTAGE, null) //
						.output("charger3", VOLTAGE, null)) //
				.next(new TestCase() //
						.output("charger2", ACTUAL_POWER, 1000) //
						.output("charger3", ACTUAL_POWER, 1000) //
						.output("charger2", CURRENT, 10) //
						.output("charger3", CURRENT, 10) //
						.output("charger2", VOLTAGE, 240) //
						.output("charger3", VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(MPPT2_I, 20) //
						.input(MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 5) //
						.input(TWO_S_PV4_I, 15) //
						.output("charger2", ACTUAL_POWER, 1000) //
						.output("charger3", ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output("charger2", ACTUAL_POWER, 500) //
						.output("charger3", ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(MPPT2_I, 20) //
						.input(MPPT2_P, 2000) //
						.input(TWO_S_PV3_I, 20) //
						.input(TWO_S_PV4_I, 0) //
						.output("charger2", ACTUAL_POWER, 500) //
						.output("charger3", ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output("charger2", ACTUAL_POWER, 2000) //
						.output("charger3", ACTUAL_POWER, 0) //
				);

		/*
		 * Test MPPT 3 - PV5 & PV6
		 */
		ess.addCharger(charger5);
		ess.addCharger(charger6);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger5) //
				.addComponent(charger6) //
				.addComponent(new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(MPPT3_I, 20) //
						.input(MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 10) //
						.input(TWO_S_PV6_I, 10) //
						.input(TWO_S_PV5_V, 240) //
						.input(TWO_S_PV6_V, 240) //

						// Values applied in the next cycle
						.output("charger4", ACTUAL_POWER, 0) //
						.output("charger5", ACTUAL_POWER, 0) //
						.output("charger4", CURRENT, null) //
						.output("charger5", CURRENT, null) //
						.output("charger4", VOLTAGE, null) //
						.output("charger5", VOLTAGE, null)) //
				.next(new TestCase() //
						.output("charger4", ACTUAL_POWER, 1000) //
						.output("charger5", ACTUAL_POWER, 1000) //
						.output("charger4", CURRENT, 10) //
						.output("charger5", CURRENT, 10) //
						.output("charger4", VOLTAGE, 240) //
						.output("charger5", VOLTAGE, 240)) //

				// Chargers with different current values
				.next(new TestCase() //
						.input(MPPT3_I, 20) //
						.input(MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 5) //
						.input(TWO_S_PV6_I, 15) //
						.output("charger4", ACTUAL_POWER, 1000) //
						.output("charger5", ACTUAL_POWER, 1000)) //
				.next(new TestCase() //
						.output("charger4", ACTUAL_POWER, 500) //
						.output("charger5", ACTUAL_POWER, 1500)) //

				.next(new TestCase() //
						.input(MPPT3_I, 20) //
						.input(MPPT3_P, 2000) //
						.input(TWO_S_PV5_I, 20) //
						.input(TWO_S_PV6_I, 0) //
						.output("charger4", ACTUAL_POWER, 500) //
						.output("charger5", ACTUAL_POWER, 1500)) //
				.next(new TestCase() //
						.output("charger4", ACTUAL_POWER, 2000) //
						.output("charger5", ACTUAL_POWER, 0) //
				);
	}

	@Test
	public void testDoSetBmsVoltage() {
		final var battery = new DummyBattery("battery0");
		final var bmsChargeMaxVoltage = new Value<Integer>(null, 123);
		final var bmsDischargeMinVoltage = new Value<Integer>(null, 456);

		// No battery values
		assertFalse(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 1, bmsDischargeMinVoltage, 1));
		battery //
				.withChargeMaxCurrent(234) //
				.withDischargeMaxCurrent(234);

		// Battery full
		battery //
				.withChargeMaxCurrent(0); //
		assertFalse(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 1, bmsDischargeMinVoltage, 1));

		// Battery empty
		battery //
				.withDischargeMaxCurrent(0); //
		assertFalse(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 1, bmsDischargeMinVoltage, 1));

		// Values are already set
		battery //
				.withChargeMaxCurrent(234) //
				.withDischargeMaxCurrent(234);
		assertFalse(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 123, bmsDischargeMinVoltage, 456));

		// Values should be updated
		assertTrue(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 1, bmsDischargeMinVoltage, 456));
		assertTrue(doSetBmsVoltage(battery, bmsChargeMaxVoltage, 123, bmsDischargeMinVoltage, 1));
	}
}
