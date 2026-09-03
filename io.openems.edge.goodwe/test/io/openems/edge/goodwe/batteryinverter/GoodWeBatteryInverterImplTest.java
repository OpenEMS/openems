package io.openems.edge.goodwe.batteryinverter;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.ACTUAL_POWER;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.CURRENT;
import static io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId.VOLTAGE;
import static io.openems.edge.goodwe.GoodWeConstants.DEFAULT_UNIT_ID;
import static io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.calculateWbmsChargeMaxCurrent;
import static io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.calculateWbmsDischargeMaxCurrent;
import static io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.doSetBmsVoltage;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.EMS_POWER_MODE;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.EMS_POWER_SET;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.FIXED_POWER_FACTOR_V2;
import static io.openems.edge.goodwe.common.GoodWe.ChannelId.GOODWE_TYPE;
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
import static io.openems.edge.goodwe.common.GoodWePowerSetting.ChannelId.V2_APM_GENERAL_OUTPUT_ACTIVE_POWER;
import static io.openems.edge.goodwe.common.GoodWePowerSetting.ChannelId.V2_RPM_FIXED_Q_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.common.test.DummySerialNumberStorage;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.goodwe.battery.cluster.AbstractGoodWeBatteryCluster;
import io.openems.edge.goodwe.battery.cluster.GoodWeBatteryClusterFeneconHomeImpl;
import io.openems.edge.goodwe.charger.mppt.twostring.GoodWeChargerMpptTwoStringImpl;
import io.openems.edge.goodwe.charger.mppt.twostring.MpptPort;
import io.openems.edge.goodwe.charger.singlestring.GoodWeChargerPv1;
import io.openems.edge.goodwe.charger.twostring.GoodWeChargerTwoStringImpl;
import io.openems.edge.goodwe.charger.twostring.PvPort;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.GoodWePowerSetting;
import io.openems.edge.goodwe.common.enums.BatteryProtocol;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.EnableDisableOrUndefined;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings.FixedPowerFactor;
import io.openems.edge.goodwe.common.enums.GoodWeType;
import io.openems.edge.goodwe.common.enums.GridCode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.common.enums.PvMode;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.common.enums.SafetyParameterEnums;
import io.openems.edge.goodwe.common.enums.WaveformDetection;

@SuppressWarnings("deprecation")
class GoodWeBatteryInverterImplTest {

	private static final DummyMeta META = new DummyMeta();

	@Test
	void testEt() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
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
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
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
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, -2000) //
						.input(MAX_AC_EXPORT, 2000) //
						.input("charger0", ACTUAL_POWER, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000L));
	}

	@Test
	void testNegativSetActivePoint() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, -2000) //
						.input(MAX_AC_EXPORT, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), -1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000L));
	}

	@Test
	void testDischargeBattery() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ACTIVE_POWER, 0) //
						.input(MAX_AC_IMPORT, -2000) //
						.input(MAX_AC_EXPORT, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(new DummyBattery("battery0"), 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.DISCHARGE_BAT) //
						.output(EMS_POWER_SET, 1000L));
	}

	@Test
	void testEmsPowerModeAutoWithBalancing() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testEmsPowerModeAutoWithSurplus() throws Exception {
		var charger = new GoodWeChargerPv1();
		new ComponentTest(charger) //
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
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testEmsPowerModeAutoWithMaxAcImport() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testEmsPowerModeAutoWithMaxAcExport() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testBatteryIsFull() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testBatteryIsEmpty() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
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
						.output(EMS_POWER_SET, 0L));
	}

	@Test
	void testMaxAcImportExportCalculation() throws Exception {

		var inverter = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerMpptTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("essOrBatteryInverter", inverter) //
				.activate(io.openems.edge.goodwe.charger.mppt.twostring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setMpptPort(MpptPort.MPPT_1) //
						.build());

		charger1._setActualPower(5000);
		charger1.getActualPowerChannel().nextProcessImage();
		inverter.addCharger(charger1);
		var battery0 = new DummyBattery("battery0");
		new ComponentTest(inverter) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(35011, // Deprecated GoodWe type register
								new int[] { 0x4757, 0x3135, 0x4b2d, 0x4554, 0x3230 })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0xc350, 0x0001, 0x3730, 0x3530, 0x4b45, 0x5446, 0x3235, 0x3830, 0x3030,
										0x3037 }))
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(battery0) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase(), 10) //

				.next(new TestCase("Limited by MaxApparentPower and PV") //
						.input(MAX_APPARENT_POWER, 55000) //
						.input("battery0", Battery.ChannelId.VOLTAGE, 500) //
						.input("battery0", Battery.ChannelId.CHARGE_MAX_CURRENT, 110) //
						.input("battery0", Battery.ChannelId.DISCHARGE_MAX_CURRENT, 110) //

						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
							assertEquals(55_000, (int) inverter.getGoodweType().maxBatChargeP);
							assertEquals(55_000, (int) inverter.getGoodweType().maxBatDischargeP);
						}) //
						.output(MAX_AC_IMPORT, -50000) // (55kW - 5kW PV)
						.output(MAX_AC_EXPORT, 55000)) //

				.next(new TestCase("Limited to zero, because of missing values") //
						.input(MAX_APPARENT_POWER, null) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, 0) //
						.output(MAX_AC_EXPORT, 0)) //

				.next(new TestCase("Limited by max DC-power (55kW -/+ PV)") //
						.input(MAX_APPARENT_POWER, 60000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, -50000) //
						.output(MAX_AC_EXPORT, 60000)) //

				.next(new TestCase("Limited by Battery") //
						.input("battery0", Battery.ChannelId.VOLTAGE, 600) //
						.input("battery0", Battery.ChannelId.CHARGE_MAX_CURRENT, 50) //
						.input("battery0", Battery.ChannelId.DISCHARGE_MAX_CURRENT, 50) //
						.input(MAX_APPARENT_POWER, 50_000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K) //
						.output(MAX_AC_IMPORT, -25_000) //
						.output(MAX_AC_EXPORT, 35_000)) //

				.next(new TestCase("Limited by MaxApparentPower") //
						/*
						 * MaxApparentPower has higher priority as AllowedChargePower as
						 * maxAcImport/Export can not be higher than apparent power
						 */
						.input("battery0", Battery.ChannelId.VOLTAGE, 700) //
						.input("battery0", Battery.ChannelId.CHARGE_MAX_CURRENT, 100) //
						.input("battery0", Battery.ChannelId.DISCHARGE_MAX_CURRENT, 100) //
						.input(MAX_APPARENT_POWER, 50_000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K) //
						.output(MAX_AC_IMPORT, -50_000) //
						.output(MAX_AC_EXPORT, 50_000)) //
		; //
	}

	@Test
	void testMaxAcImportExportCalculationWithForceCharge() throws Exception {
		var inverter = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerMpptTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("essOrBatteryInverter", inverter) //
				.activate(io.openems.edge.goodwe.charger.mppt.twostring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setMpptPort(MpptPort.MPPT_1) //
						.build());
		inverter.addCharger(charger1);

		/*
		 * WBMS_x_MAX_CURRENT not longer used for calculating the maxAcPower as the
		 * inverter is not able to handle minus values for force charge/discharge
		 */

		var battery0 = new DummyBattery("battery0");
		new ComponentTest(inverter) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withRegisters(35011, // Deprecated GoodWe type register
								new int[] { 0x4757, 0x3135, 0x4b2d, 0x4554, 0x3230 })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0xc350, 0x0001, 0x3730, 0x3530, 0x4b45, 0x5446, 0x3235, 0x3830, 0x3030,
										0x3037 }))
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.addComponent(charger1) //
				.addComponent(battery0) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase("Limited by max battery MaxCurrent & Voltage is null") //
						.input("charger0", EssDcCharger.ChannelId.ACTUAL_POWER, 5000) //
						.input("battery0", Battery.ChannelId.VOLTAGE, null) //
						.input("battery0", Battery.ChannelId.CHARGE_MAX_CURRENT, 5) //
						.input("battery0", Battery.ChannelId.DISCHARGE_MAX_CURRENT, -2) //
						.input(MAX_APPARENT_POWER, 10000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, 0) //
						.output(MAX_AC_EXPORT, 5000) //
				) //

				.next(new TestCase("Limited by max battery MaxCurrent & Force Charge") //
						.input("battery0", Battery.ChannelId.VOLTAGE, 600) //
						.input("battery0", Battery.ChannelId.CHARGE_MAX_CURRENT, 10) //
						.input("battery0", Battery.ChannelId.DISCHARGE_MAX_CURRENT, -2) //
						.input(MAX_APPARENT_POWER, 10000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, -1000) // 600V x 10A = 6kW allowed import minus 5kW DC-PV
						.output(MAX_AC_EXPORT, 3800)) // 600V x -2A = -1.2kW allowed export plus 5kW DC-PV

				.next(new TestCase("Limited to force charge values without DC-PV") //
						.input("charger0", EssDcCharger.ChannelId.ACTUAL_POWER, null) //
						.input(MAX_APPARENT_POWER, 10000) //
						.onExecuteWriteCallbacks(() -> {
							inverter.run(battery0, 0, 0);
						}) //
						.output(MAX_AC_IMPORT, -6000) // 600V x 10A = 6kW allowed import without DC-PV
						.output(MAX_AC_EXPORT, -1200)) // 600V x -2A = -1.2kW allowed export without DC-PV
		;
	}

	@Test
	void testTwoStringCharger() throws Exception {
		var ess = new GoodWeBatteryInverterImpl();
		var charger1 = new GoodWeChargerTwoStringImpl();
		var charger2 = new GoodWeChargerTwoStringImpl();
		var charger3 = new GoodWeChargerTwoStringImpl();
		var charger4 = new GoodWeChargerTwoStringImpl();
		var charger5 = new GoodWeChargerTwoStringImpl();
		var charger6 = new GoodWeChargerTwoStringImpl();

		new ComponentTest(charger1) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger0") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_1) //
						.build());

		new ComponentTest(charger2) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger1") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_2) //
						.build());

		new ComponentTest(charger3) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger2") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_3) //
						.build());

		new ComponentTest(charger4) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger3") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_4) //
						.build());

		new ComponentTest(charger5) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger4") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_5) //
						.build());

		new ComponentTest(charger6) //
				.addReference("essOrBatteryInverter", ess) //
				.activate(io.openems.edge.goodwe.charger.twostring.MyConfig.create() //
						.setId("charger5") //
						.setBatteryInverterId("batteryInverter0") //
						.setPvPort(PvPort.PV_6) //
						.build());

		ess.addCharger(charger1);
		ess.addCharger(charger2);
		new ComponentTest(ess) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
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
				.addReference("meta", META) //
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
				.addReference("meta", META) //
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
	void testDoSetBmsVoltage() {
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

	@Test
	void testReadFromModbus() throws Exception {
		var sut = new GoodWeBatteryInverterImpl();
		new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withRegisters(35011, // Deprecated GoodWe type register
								new int[] { 0x4757, 0x3135, 0x4b2d, 0x4554, 0x3230 })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0x3a98, 0x0001, 0x3730, 0x3135, 0x4b45, 0x5542, 0x3234, 0x3730, 0x3031,
										0x3734 })
						.withRegisters(35180, // Battery values of GoodWe
								new int[] { 0x056e, 0x0000, 0xffff, 0xfffb, 0x0002 })
						.withRegisters(35016, // GoodWe Software Versions
								new int[] { 0, 0, 0x07df, 0x0006, 0x0185 })
						.withRegisters(35111, // PV data including GridMode
								new int[] { 0x8FC, 0, 0, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0x0200, 0x8EF, 0x0054,
										0x1389, 0xFFFF, 0xF869, 0x08E3, 0x0055, 0x138B, 0xFFFF, 0xF870, 0x08EC, 0x0056,
										0x138B, 0xFFFF, 0xF86b, 0x0001 /* GridMode */ }))
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus2") //
						.setMpptForShadowEnable(EnableDisable.DISABLE) //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase() //
						.output(GoodWe.ChannelId.SERIAL_NUMBER, "7015KEUB24700174") //
						.output(SymmetricEss.ChannelId.MAX_APPARENT_POWER, 15_000) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.UNDEFINED)) //
				.next(new TestCase() //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_GEN2_15K)) // read element once

				.next(new TestCase() // register 35111 - 35136
						.output(GoodWe.ChannelId.V_PV3, 230) // register not 0xFFFF
						.output(GoodWe.ChannelId.I_PV3, 0) //
						.output(GoodWe.ChannelId.P_PV3, 0L) //
						.output(GoodWe.ChannelId.V_PV4, null) //
						.output(GoodWe.ChannelId.I_PV4, null) //
						.output(GoodWe.ChannelId.P_PV4, null) //
						.output(GoodWe.ChannelId.PV_MODE, PvMode.UNDEFINED) //
						.output(SymmetricBatteryInverter.ChannelId.GRID_MODE, GridMode.ON_GRID)) //
				.deactivate();
	}

	@Test
	void testPowerModeFromModbus() throws Exception {
		var sut = new GoodWeBatteryInverterImpl();
		new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						// Not part of the test
						.withRegisters(35011, // Deprecated GoodWe type register
								new int[] { 0x4757, 0x3135, 0x4b2d, 0x4554, 0x3230 })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0x3a98, 0x0001, 0x3730, 0x3135, 0x4b45, 0x5542, 0x3234, 0x3730, 0x3031,
										0x3734 })
						.withRegisters(35180, // Battery values of GoodWe
								new int[] { 0x056e, 0x0000, 0xffff, 0xfffb, 0x0002 })
						.withRegisters(35016, // GoodWe Software Versions
								new int[] { 0, 0, 0x07df, 0x0006, 0x0185 })
						.withRegisters(35111, // PV data including GridMode
								new int[] { 0x8FC, 0, 0, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0x0200, 0x8EF, 0x0054,
										0x1389, 0xFFFF, 0xF869, 0x08E3, 0x0055, 0x138B, 0xFFFF, 0xF870, 0x08EC, 0x0056,
										0x138B, 0xFFFF, 0xF86b, 0x0001 /* GridMode */ })

						// Power Mode
						.withRegisters(45472, //
								new int[] { 0x000, 0x0c8, 0x816, 0, 0x898, 0x3e8, 0x9c4, 0x3e8, 0xa5a, 0x0c8, 0x061, 0,
										0x3e8 }))
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus2") //
						.setMpptForShadowEnable(EnableDisable.DISABLE) //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase() //
						.output(GoodWe.ChannelId.ENABLE_PU_CURVE, EnableCurve.ENABLE) // WriteValue
						.output(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_1_OR_NONE) //
				);
	}

	@Test
	void testNoStatesReadFromModbus() throws Exception {
		var inv = "batteryInverter0";
		var sut = new GoodWeBatteryInverterImpl();
		new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withRegisters(32000, // GoodWe State Register
								new int[] {
										0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
										0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xffff, 0xffff, 0xffff,
										0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0x3a98, 0x0001, 0x3730, 0x3135, 0x4b45, 0x5542, 0x3234, 0x3730, 0x3031,
										0x3734 })
						.withRegisters(35180, // Battery values of GoodWe
								new int[] { 0x056e, 0x0000, 0xffff, 0xfffb, 0x0002 })
						.withRegisters(35016, // GoodWe Software Versions
								new int[] { 0, 0, 0x07df, 0x0006, 0x0185 })
						.withRegisters(35111, // PV data including GridMode
								new int[] { 0x8FC, 0, 0, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0x0200, 0x8EF, 0x0054,
										0x1389, 0xFFFF, 0xF869, 0x08E3, 0x0055, 0x138B, 0xFFFF, 0xF870, 0x08EC, 0x0056,
										0x138B, 0xFFFF, 0xF86b, 0x0001 /* GridMode */ }))
				.activate(MyConfig.create() //
						.setId(inv) //
						.setModbusId("modbus2") //
						.setMpptForShadowEnable(EnableDisable.DISABLE) //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase() //
						.output(GoodWe.ChannelId.SERIAL_NUMBER, "7015KEUB24700174") //
						.output(SymmetricEss.ChannelId.MAX_APPARENT_POWER, 15_000) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.UNDEFINED)) //

				.next(new TestCase() // Initially null because of low modbus priority
						.output(inv, "GwState32000B0", null) //
						.output(inv, "GwState32000B1", null) //
						.output(inv, "GwState32000B2", null) //
						.output(inv, "GwState32000B3", null) //
						.output(inv, "GwState32000B4", null) //
						.output(inv, "GwState32000B5", null) //
						.output(inv, "GwState32000B6", null) //
						.output(inv, "GwState32000B7", null) //
						.output(inv, "GwState32000B8", null) //
						.output(inv, "GwState32000B9", null) //
						.output(inv, "GwState32000B10", null) //
						.output(inv, "GwState32000B11", null) //
						.output(inv, "GwState32000B12", null) //
						.output(inv, "GwState32000B13", null) //
						.output(inv, "GwState32000B14", null) //
						.output(inv, "GwState32000B15", null)) //

				.next(new TestCase(), 50).next(new TestCase() //
						.output(inv, "GwState32000B0", false) //
						.output(inv, "GwState32000B1", false) //
						.output(inv, "GwState32000B2", false) //
						.output(inv, "GwState32000B3", false) //
						.output(inv, "GwState32000B4", false) //
						.output(inv, "GwState32000B5", false) //
						.output(inv, "GwState32000B6", false) //
						.output(inv, "GwState32000B7", false) //
						.output(inv, "GwState32000B8", false) //
						.output(inv, "GwState32000B9", false) //
						.output(inv, "GwState32000B10", false) //
						.output(inv, "GwState32000B11", false) //
						.output(inv, "GwState32000B12", false) //
						.output(inv, "GwState32000B13", false) //
						.output(inv, "GwState32000B14", false) //
						.output(inv, "GwState32000B15", false)) //
				.deactivate();

	}

	@Test
	void testGoodWePowerSettings() throws Exception {
		var inv = "batteryInverter0";
		var sut = new GoodWeBatteryInverterImpl();
		new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withRegisters(32000, // GoodWe State Register
								new int[] {
										0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
										0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xffff, 0xffff, 0xffff,
										0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0xc350, 0x0001, 0x3730, 0x3530, 0x4b45, 0x5446, 0x3235, 0x3830, 0x3030,
										0x3037 })
						.withRegisters(35011, new int[] { 0, 0, 0, 0, 0 }) //
						.withRegisters(35180, // Battery values of GoodWe
								new int[] { 0x056e, 0x0000, 0xffff, 0xfffb, 0x0002 })
						.withRegisters(35016, // GoodWe Software Versions
								new int[] { 0, 0, 0x07df, 0x0006, 0x0185 })
						.withRegisters(35111, // PV data including GridMode
								new int[] { 0x8FC, 0, 0, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0x0200, 0x8EF, 0x0054,
										0x1389, 0xFFFF, 0xF869, 0x08E3, 0x0055, 0x138B, 0xFFFF, 0xF870, 0x08EC, 0x0056,
										0x138B, 0xFFFF, 0xF86b, 0x0001 /* GridMode */ }))
				.activate(MyConfig.create() //
						.setId(inv) //
						.setModbusId("modbus2") //
						.setMpptForShadowEnable(EnableDisable.DISABLE) //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase() //
						.output(GoodWe.ChannelId.SERIAL_NUMBER, "7050KETF25800007") //
						.output(SymmetricEss.ChannelId.MAX_APPARENT_POWER, 50000) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.UNDEFINED)) //

				.next(new TestCase(), 50) //
				.next(new TestCase() //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K)) //

				.next(new TestCase(), 50).next(new TestCase() //
						.output(inv, "GwState32000B0", false) //
						.output(inv, "GwState32000B1", false) //
						.output(inv, "GwState32000B2", false) //
						.output(inv, "GwState32000B3", false) //
						.output(inv, "GwState32000B4", false) //
						.output(inv, "GwState32000B5", false) //
						.output(inv, "GwState32000B6", false) //
						.output(inv, "GwState32000B7", false) //
						.output(inv, "GwState32000B8", false) //
						.output(inv, "GwState32000B9", false) //
						.output(inv, "GwState32000B10", false) //
						.output(inv, "GwState32000B11", false) //
						.output(inv, "GwState32000B12", false) //
						.output(inv, "GwState32000B13", false) //
						.output(inv, "GwState32000B14", false) //
						.output(inv, "GwState32000B15", false)) //
				.deactivate();

	}

	@Test
	void testStatesReadFromModbus() throws Exception {
		var inv = "batteryInverter0";
		var sut = new GoodWeBatteryInverterImpl();
		new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withRegisters(32000, // GoodWe State Register
								new int[] { 0x2044, 0x0000, 0x0000, 0xffff, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
										0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xffff, 0xffff,
										0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
										0xffff })
						.withRegisters(35011, // Deprecated GoodWe type register
								new int[] { 0x4757, 0x3135, 0x4b2d, 0x4554, 0x3230 })
						.withRegisters(35001, // Block including GoodWe Serial Number
								new int[] { 0x3a98, 0x0001, 0x3730, 0x3135, 0x4b45, 0x5542, 0x3234, 0x3730, 0x3031,
										0x3734 })
						.withRegisters(35180, // Battery values of GoodWe
								new int[] { 0x056e, 0x0000, 0xffff, 0xfffb, 0x0002 })
						.withRegisters(35016, // GoodWe Software Versions
								new int[] { 0, 0, 0x07df, 0x0006, 0x0185 })
						.withRegisters(35111, // PV data including GridMode
								new int[] { 0x8FC, 0, 0, 0, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0, 0x0200, 0x8EF, 0x0054,
										0x1389, 0xFFFF, 0xF869, 0x08E3, 0x0055, 0x138B, 0xFFFF, 0xF870, 0x08EC, 0x0056,
										0x138B, 0xFFFF, 0xF86b, 0x0001 /* GridMode */ }))
				.activate(MyConfig.create() //
						.setId(inv) //
						.setModbusId("modbus2") //
						.setMpptForShadowEnable(EnableDisable.DISABLE) //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.SMART) //
						.setStartStop(StartStopConfig.START) //
						.build()) //

				.next(new TestCase() //
						.output(GoodWe.ChannelId.SERIAL_NUMBER, "7015KEUB24700174") //
						.output(SymmetricEss.ChannelId.MAX_APPARENT_POWER, 15_000) //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.UNDEFINED)) //
				.next(new TestCase() //
						.output(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_GEN2_15K)) // read element once

				.next(new TestCase(), 50).next(new TestCase() //
						.output(inv, "GwState32000B0", false) //
						.output(inv, "GwState32000B1", false) //
						.output(inv, "GwState32000B2", true) //
						.output(inv, "GwState32000B3", false) //
						.output(inv, "GwState32000B4", false) //
						.output(inv, "GwState32000B5", false) //
						.output(inv, "GwState32000B6", true) //
						.output(inv, "GwState32000B7", false) //
						.output(inv, "GwState32000B8", false) //
						.output(inv, "GwState32000B9", false) //
						.output(inv, "GwState32000B10", false) //
						.output(inv, "GwState32000B11", false) //
						.output(inv, "GwState32000B12", false) //
						.output(inv, "GwState32000B13", true) //
						.output(inv, "GwState32000B14", false) //
						.output(inv, "GwState32000B15", false)) //
		;

		assertNotNull(sut.channel("GwState32004B0"));

		// 32008 has only 4 bits
		assertNotNull(sut.channel("GwState32008B4"));
		assertThrows(IllegalArgumentException.class, //
				() -> sut.channel("GwState32008B5"));

		// complete register was undefined
		assertThrows(IllegalArgumentException.class, //
				() -> sut.channel("GwState32003B0"));
		assertThrows(IllegalArgumentException.class, //
				() -> sut.channel("GwState32021B0"));

		sut.deactivate();
	}

	@Test
	void testDynamicState14Text() throws Exception {
		var component = new GoodWeBatteryInverterImpl();
		final var docForState14 = component.channel(GoodWe.ChannelId.STATE_14).channelDoc();

		var test = getComponentTest(component, GridCode.VDE_4105) //
				.next(new TestCase() //
						.input(GOODWE_TYPE, GoodWeType.GOODWE_5K_BT));

		assertEquals(
				"Utility Phase Failure | Phasenfehler | Überprüfen Sie das Drehfeld am Wechselrichter. Ggf. Kommunikationsadapter (ET+) nicht (richtig) gesteckt",
				docForState14.getText());

		test.next(new TestCase() //
				.input(GOODWE_TYPE, GoodWeType.FENECON_FHI_10_DAH));

		assertEquals("Utility Phase Failure | Phasenfehler | Überprüfen Sie das Drehfeld am Wechselrichter.",
				docForState14.getText());
	}

	@Test
	void testWaveFormDetectionWith4105() throws Exception {
		getComponentTest(GridCode.VDE_4105, //
				new TestCase() //
						.input(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K)) //
				.next(new TestCase() //
						.output(GoodWe.ChannelId.WAVE_FORM_DETECTION, WaveformDetection.HIGH_PRECISION));
	}

	@Test
	void testWaveFormDetectionWith4110() throws Exception {
		getComponentTest(GridCode.VDE_4110, //
				new TestCase() //
						.input(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K)) //
				.next(new TestCase() //
						.input(GoodWe.ChannelId.GOODWE_TYPE, GoodWeType.FENECON_50K) //
						.output(GoodWe.ChannelId.WAVE_FORM_DETECTION, WaveformDetection.DETECTION_DISABLED));
	}

	private static ComponentTest getComponentTest(//
			GridCode gridCode, //
			TestCase... inputsBeforeActive //
	) throws Exception {
		return getComponentTest(new GoodWeBatteryInverterImpl(), gridCode, inputsBeforeActive);
	}

	private static ComponentTest getComponentTest(//
			GoodWeBatteryInverter component, //
			GridCode gridCode, //
			TestCase... inputsBeforeActive //
	) throws Exception {
		final var componentTest = new ComponentTest(component) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()); //

		for (var input : inputsBeforeActive) {
			componentTest.next(input);
		}

		componentTest.activate(MyConfig.create() //
				.setId("batteryInverter0") //
				.setModbusId("modbus0") //
				.setModbusUnitId(DEFAULT_UNIT_ID) //
				.setSafetyCountry(SafetyCountry.GERMANY) //
				.setMpptForShadowEnable(EnableDisable.ENABLE) //
				.setBackupEnable(EnableDisable.ENABLE) //
				.setFeedPowerEnable(EnableDisable.ENABLE) //
				.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
				.setControlMode(ControlMode.REMOTE) //
				.setStartStop(StartStopConfig.START) //
				.setGridCode(gridCode) //
				.build());

		return componentTest;
	}

	@Test
	void testCalculateWbmsChargeMaxCurrent() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		final WriteChannel<Integer> wbmsMaxCharge = mock();
		when(wbmsMaxCharge.value()).thenReturn(new Value<>(null, 100));

		final var result = calculateWbmsChargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, wbmsMaxCharge, null, null, null, null, null, null, null, null, null,
						null, null, null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, false), 100);

		assertEquals(100, result);
	}

	@Test
	void testCalculateWbmsChargeMaxCurrentLock() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		when(lock.getNextWriteValue()).thenReturn(Optional.of(true));

		final var result = calculateWbmsChargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
						null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, false), 100);

		assertEquals(0, result);
	}

	@Test
	void testCalculateWbmsChargeMaxCurrentNegativeDischargeSelf() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(-2);

		final WriteChannel<Boolean> lock = mock();
		final WriteChannel<Integer> wbmsMaxCharge = mock();
		when(wbmsMaxCharge.value()).thenReturn(new Value<>(null, 100));

		final var result = calculateWbmsChargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, wbmsMaxCharge, null, null, null, null, null, null, null, null, null,
						null, null, null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, true), 100);

		assertEquals(100, result);
	}

	@Test
	void testCalculateWbmsChargeMaxCurrentNegativeDischargeOther() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		when(lock.getNextWriteValue()).thenReturn(Optional.of(true));

		final var result = calculateWbmsChargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
						null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, true), 100);

		assertEquals(0, result);
	}

	@Test
	void testCalculateWbmsDischargeMaxCurrent() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		final WriteChannel<Integer> wbmsMaxDischarge = mock();
		when(wbmsMaxDischarge.value()).thenReturn(new Value<>(null, 100));

		final var result = calculateWbmsDischargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, wbmsMaxDischarge, null, null, null, null, null, null,
						null, null, null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, false), 100);

		assertEquals(100, result);
	}

	@Test
	void testCalculateWbmsDischargeMaxCurrentLock() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		when(lock.getNextWriteValue()).thenReturn(Optional.of(true));

		final var result = calculateWbmsDischargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
						null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(false, false), 100);

		assertEquals(0, result);
	}

	@Test
	void testCalculateWbmsDischargeMaxCurrentNegativeDischargeSelf() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(-2) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		final WriteChannel<Integer> wbmsMaxDischarge = mock();
		when(wbmsMaxDischarge.value()).thenReturn(new Value<>(null, 100));

		final var result = calculateWbmsDischargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, wbmsMaxDischarge, null, null, null, null, null, null,
						null, null, null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(true, false), 100);

		assertEquals(100, result);
	}

	@Test
	void testCalculateWbmsDischargeMaxCurrentNegativeDischargeOther() {
		final var battery = new DummyBattery("battery0") //
				.withChargeMaxCurrent(200) //
				.withDischargeMaxCurrent(200);

		final WriteChannel<Boolean> lock = mock();
		when(lock.getNextWriteValue()).thenReturn(Optional.of(true));

		final var result = calculateWbmsDischargeMaxCurrent(battery,
				new GoodWeBatteryInverterImpl.BatteryLimitsChannel(null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
						null, lock),
				new GoodWeBatteryInverterImpl.ClusterInfo(true, false), 100);

		assertEquals(0, result);
	}

	@Test
	void testUpdatePowerCluster() throws Exception {

		final var battery1 = new BatteryFeneconHomeImpl();
		final var battery2 = new BatteryFeneconHomeImpl();

		final var cluster = new GoodWeBatteryClusterFeneconHomeImpl();
		new ComponentTest(cluster) //
				.addReference("addBattery", battery1) //
				.addReference("addBattery", battery2);

		final var inverter = new GoodWeBatteryInverterImpl();
		final var test = getComponentTest(inverter, GridCode.VDE_4105);
		test.next(new TestCase()//
				.onBeforeWriteCallbacks(() -> {
					inverter.run(cluster, 0, 0);
				}) //
				.input(GoodWe.ChannelId.P_BATTERY1, 1000) //
				.input(GoodWe.ChannelId.P_BATTERY2, 2000) //
				.output(GoodWe.ChannelId.DC_DISCHARGE_POWER_BATTERY_1, 1000) //
				.output(GoodWe.ChannelId.DC_DISCHARGE_POWER_BATTERY_2, 2000) //
				.output(HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, 3000) //
		);
	}

	@Test
	void testUpdatePowerSingleBattery() throws Exception {

		final var battery = new BatteryFeneconHomeImpl();

		final var inverter = new GoodWeBatteryInverterImpl();
		final var test = getComponentTest(inverter, GridCode.VDE_4105);
		test.next(new TestCase()//
				.onBeforeWriteCallbacks(() -> {
					inverter.run(battery, 0, 0);
				}) //
				.input(GoodWe.ChannelId.P_BATTERY1, 1000) //
				.input(GoodWe.ChannelId.P_BATTERY2, 2000) //
				.output(GoodWe.ChannelId.DC_DISCHARGE_POWER_BATTERY_1, 1000) //
				.output(GoodWe.ChannelId.DC_DISCHARGE_POWER_BATTERY_2, null) //
				.output(HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, 1000) //
		);
	}

	@Test
	void testUpdateBatteryLimits() throws Exception {

		final var battery1 = new DummyBattery("battery1") //
				.withVoltage(401) //
				.withCurrent(11) //
				.withSoc(31) //
				.withSoh(32) //
				.withMaxCellTemperature(200) //
				.withMinCellTemperature(200) //
				.withChargeMaxCurrent(91) //
				.withDischargeMaxCurrent(92) //
				.withChargeMaxVoltage(411) //
				.withDischargeMinVoltage(412) //
		;

		final var battery2 = new DummyBattery("battery2") //
				.withVoltage(402) //
				.withCurrent(12) //
				.withSoc(33) //
				.withSoh(34) //
				.withMaxCellTemperature(210) //
				.withMinCellTemperature(210) //
				.withChargeMaxCurrent(93) //
				.withDischargeMaxCurrent(94) //
				.withChargeMaxVoltage(413) //
				.withDischargeMinVoltage(414) //
		;

		final AbstractGoodWeBatteryCluster cluster = mock();
		when(cluster.getBatteries()).thenReturn(List.of(battery1, battery2));
		when(cluster.getChargeMaxCurrent()).thenReturn(new Value<>(null, 100));
		when(cluster.getDischargeMaxCurrent()).thenReturn(new Value<>(null, 100));
		when(cluster.getVoltage()).thenReturn(new Value<>(null, 401));

		final var inverter = new GoodWeBatteryInverterImpl();
		final var test = getComponentTest(inverter, GridCode.VDE_4105);
		test.next(new TestCase()//
				.onBeforeWriteCallbacks(() -> {
					inverter.run(cluster, 0, 0);
				})

				// battery 1
				.input(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, 0) //
				.input(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, 0) //

				.output(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, BatteryProtocol.EMS_USE) //
				.output(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, 25) //
				.output(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, 25) //
				.output(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, 411) //
				.output(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, 412) //
				.output(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, 0) //
				.output(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN, 0) //
				.output(GoodWe.ChannelId.BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE, 412) //
				.output(GoodWe.ChannelId.BMS_CAPACITY, 50) //
				.output(GoodWe.ChannelId.WBMS_VERSION, 1) //
				.output(GoodWe.ChannelId.WBMS_STRINGS, 9) //
				.output(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, 411) //
				.output(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, 1) //
				.output(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, 412) //
				.output(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, 1) //
				.output(GoodWe.ChannelId.WBMS_VOLTAGE, 401) //
				.output(GoodWe.ChannelId.WBMS_CURRENT, 11) //
				.output(GoodWe.ChannelId.WBMS_SOC, 31) //
				.output(GoodWe.ChannelId.WBMS_SOH, 32) //
				.output(GoodWe.ChannelId.WBMS_TEMPERATURE, 200) //
				.output(GoodWe.ChannelId.WBMS_WARNING_CODE, 0) //
				.output(GoodWe.ChannelId.WBMS_ALARM_CODE, 0) //
				.output(GoodWe.ChannelId.WBMS_STATUS, 0) //
				.output(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, null) //
				.output(GoodWe.ChannelId.BATTERY_1_LOCK, null) //

				// battery 2
				.input(GoodWe.ChannelId.BATTERY_2_CHARGE_CURRENT_MAX, 0) //
				.input(GoodWe.ChannelId.BATTERY_2_DISCHARGE_CURRENT_MAX, 0) //

				.output(GoodWe.ChannelId.BATTERY_2_PROTOCOL, BatteryProtocol.EMS_USE) //
				.output(GoodWe.ChannelId.BATTERY_2_CHARGE_CURRENT_MAX, 25) //
				.output(GoodWe.ChannelId.BATTERY_2_DISCHARGE_CURRENT_MAX, 25) //
				.output(GoodWe.ChannelId.BATTERY_2_CHARGE_VOLTAGE_MAX, 413) //
				.output(GoodWe.ChannelId.BATTERY_2_VOLTAGE_UNDER_MIN, 414) //
				.output(GoodWe.ChannelId.BATTERY_2_SOC_UNDER_MIN, 0) //
				.output(GoodWe.ChannelId.BATTERY_2_OFFLINE_SOC_UNDER_MIN, 0) //
				.output(GoodWe.ChannelId.BATTERY_2_OFFLINE_VOLTAGE_UNDER_MIN, 414) //
				.output(GoodWe.ChannelId.BATTERY_2_CAPACITY, 50) //
				.output(GoodWe.ChannelId.WBMS_VERSION_2, 1) //
				.output(GoodWe.ChannelId.WBMS_STRINGS_2, 9) //
				.output(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE_2, 413) //
				.output(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT_2, 1) //
				.output(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE_2, 414) //
				.output(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT_2, 1) //
				.output(GoodWe.ChannelId.WBMS_VOLTAGE_2, 402) //
				.output(GoodWe.ChannelId.WBMS_CURRENT_2, 12) //
				.output(GoodWe.ChannelId.WBMS_SOC_2, 33) //
				.output(GoodWe.ChannelId.WBMS_SOH_2, 34) //
				.output(GoodWe.ChannelId.WBMS_TEMPERATURE_2, 210) //
				.output(GoodWe.ChannelId.WBMS_WARNING_CODE_2, 0) //
				.output(GoodWe.ChannelId.WBMS_ALARM_CODE_2, 0) //
				.output(GoodWe.ChannelId.WBMS_STATUS_2, 0) //
				.output(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION_2, null) //
				.output(GoodWe.ChannelId.BATTERY_2_LOCK, null) //
		);
	}

	@Test
	void testappendV3Tasks() throws Exception {
		final var bridge = new DummyModbusBridge("modbus1")
				.withRegisters(43506, 0x0064, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0007, 0x00A0)
				.withRegisters(43610, 0x0005, 0x000A, 0x0000, 0x044C, 0x0384, 0x139C, 0x1374, 0x001E, 0x003C, 0x0000,
						0x047E, 0x0352, 0x13EC, 0x12F2) //
				.withRegisters(43640, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x012C, 0x01F4, 0x0438, 0x012C, 0x0398,
						0xFED4, 0x04B0, 0x01F4, 0x0320, 0xFE0C, 0x0000, 0x0000, 0x0000, 0x0000, 0x0003, 0x0000, 0x0000,
						0x03E8, 0x03E8, 0x02BC, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x044C, 0x00C8, 0x0384, 0x0384, 0x047E, 0x012C, 0x0352,
						0x0064, 0x0000, 0x0000, 0x0032, 0x0000, 0x1388, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x01F4, 0x02BC, 0x00C8, 0x0000, 0x0320, 0x0384, 0x03E8, 0x03B6, 0x0000, 0x07D0, 0x07D0, 0x0000,
						0x0000, 0x0438, 0x0000, 0x0000, 0x0000, 0x0006, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0001, 0x0000) //
				.withRegisters(43780, 0x0000, 0x0000, 0x1388, 0x0000, 0x0190, 0x00C8, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x139C, 0x0003, 0x0000, 0x0064,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x1388, 0x0000, 0x0258, 0x0078,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0001,
						0x1374, 0x0005, 0x0000, 0x0096, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xFF9C, 0x05DC, 0x02BC,
						0xFA24, 0x012C, 0x09C4, 0xFED4, 0xF63C, 0x01F4, 0x01F4, 0xFE0C, 0xFE0C, 0x0000, 0x0000, 0x0000,
						0x0000, 0x000A, 0x0000, 0x0000, 0x000A, 0x0014) //
				.withRegisters(43910, 0x0398, 0x0000, 0x01F4, 0x04B0, 0x0000, 0x03E8, 0x0320, 0x0000, 0x05DC, 0x044C,
						0x0000, 0x07D0, 0x0384, 0x0000, 0x0BB8, 0x047E, 0x0000, 0x01F4, 0x0352, 0x0000, 0x03E8, 0x0438,
						0x0000, 0x05DC, 0x13EC, 0x0000, 0x07D0, 0x12F2, 0x0000, 0x0BB8, 0x139C, 0x0000, 0x01F4, 0x1374,
						0x0000, 0x03E8, 0x13EC, 0x0000, 0x05DC, 0x12F2, 0x0000, 0x07D0, 0x139C, 0x0000, 0x0BB8, 0x1374,
						0x0000, 0x01F4, 0x0398, 0x0000, 0x03E8, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0032, 0x0000, 0x0050, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0001, 0x86A0, 0x0001, 0x86A0, 0x0000, 0x0000, 0x0000, 0x0000, 0x04B0, 0x0320,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0005, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x044C) //
				.withRegisters(44040, 0x0384, 0x0064, 0x047E, 0x0096, 0x0352, 0x00C8, 0x0438, 0x00FA, 0x0398, 0x0014,
						0x04B0, 0x001E, 0x0320, 0x0032, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x044C, 0x0384, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0008,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x047E, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
						0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0352, 0x0064, 0x0438, 0x0096, 0x0398, 0x00C8,
						0x04B0, 0x00FA, 0x0320, 0x0014, 0x044C, 0x001E, 0x0384, 0x0032) //
				.withRegisters(44150, 0x0000, 0xBB80, 0x0000, 0x05DC, 0xAFC8, 0x0000, 0x07D0, 0xC830, 0x0000, 0x0BB8,
						0xA314, 0x0000, 0x01F4, 0x9F2C, 0x0000, 0x03E8) //
				.withRegisters(44166, 0xC830, 0x0000, 0x05DC); //

		var sut = new GoodWeBatteryInverterImpl();

		final var test = new ComponentTest(sut) //
				.addReference("meta", META) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", bridge) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus1") //
						.setModbusUnitId(DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setMpptForShadowEnable(EnableDisable.ENABLE) //
						.setBackupEnable(EnableDisable.ENABLE) //
						.setFeedPowerEnable(EnableDisable.ENABLE) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setControlMode(ControlMode.REMOTE) //
						.setStartStop(StartStopConfig.START) //
						.build()); //

		sut.getGoodweTypeChannel().setNextValue(GoodWeType.FENECON_100K);
		sut.getGoodweTypeChannel().nextProcessImage();
		sut.getDspFmVersionMasterChannel().setNextValue(1);
		sut.getDspFmVersionMasterChannel().nextProcessImage();
		sut.getDspBetaVersionChannel().setNextValue(212);
		sut.getDspBetaVersionChannel().nextProcessImage();

		sut.addPowerSettingTasks();

		test.next(new TestCase(), 10);

		test.next(new TestCase() //
				.activateStrictMode() //
				.withIgnoredChannelsForStrictMode(channelIdsExceptGoodWePowerSettings())
				.outputReadValue(V2_APM_GENERAL_OUTPUT_ACTIVE_POWER, 100) //
				.outputReadValue(FIXED_POWER_FACTOR_V2, FixedPowerFactor.LAGGING_0_93) //
				.outputReadValue(V2_RPM_FIXED_Q_VALUE, 160)
				// -- block starting at 43610 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_OBSERVATION_TIME, 5) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT, 10) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_VOLTAGE, 1100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_VOLTAGE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_FREQUENCY, 50200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_FREQUENCY, 49800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_OBSERVATION_TIME, 30) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT, 60) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_VOLTAGE, 1150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_VOLTAGE, 850) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_FREQUENCY, 51000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_FREQUENCY, 48500) //

				// -- block starting at 43640 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_ENABLE_QU_CURVE, EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_EXTENDED_FUNCTIONS,
						EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_CURVE_MODE, SafetyParameterEnums.Rpm.Mode.BASIC) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PU_CURVE, EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_OUTPUT_RESPONSE_MODE,
						SafetyParameterEnums.Vrt.GeneralRecoveryMode.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_CURVE_COS_PHI_P, EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT_ENABLE,
						EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_CURVE_MODE,
						SafetyParameterEnums.Rpm.Mode.BASIC) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_OVEREXCITED_SLOPE, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_UNDEREXCITED_SLOPE, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_UNDEREXCITED_SLOPE, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COS_PHI_P_OVEREXCITED_SLOPE, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_IN_POWER, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_COS_PHI, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_COS_PHI, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_COS_PHI, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_OUT_POWER, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VOLTAGE, 1080) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VALUE, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VOLTAGE, 920) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VALUE, -300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VOLTAGE, 1200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VALUE, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VOLTAGE, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VALUE, -500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_TIME_CONSTANT, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QU_VOLTAGE_DEAD_BAND, 700) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VOLTAGE, 1100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VALUE, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VOLTAGE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VALUE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VOLTAGE, 1150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VALUE, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VOLTAGE, 850) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VALUE, 100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE, 50) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE, 5000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_POWER, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_POWER, 700) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_POWER, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_COS_PHI, 950) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_EXTENDED_FUNCTIONS,
						EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE, 1080) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_TIME_CONSTANT, 600) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE, true) //

				// -- block starting at 43780 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE, EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE,
						EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_QP_CURVE, EnableCurve.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_CURVE_MODE, SafetyParameterEnums.Rpm.Mode.BASIC) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_SLOPE, 400) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_TIME, 20000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_START, 50000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_THRESHOLD, 50000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE, false) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT, 50200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME, 3000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE, 100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_SLOPE, 600) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_TIME, 12) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE, true) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT, 49800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE, 150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_POWER, -100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_REACTIVE_POWER, 1500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_POWER, 700) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_REACTIVE_POWER, -1500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_POWER, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_REACTIVE_POWER, 2500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_POWER, -300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_REACTIVE_POWER, -2500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_POWER, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_REACTIVE_POWER, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_POWER, -500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_REACTIVE_POWER, -500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_TIME_CONSTANT, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_OVEREXCITED_SLOPE, 10) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_RPM_QP_UNDEREXCITED_SLOPE, 20) //

				// -- block starting at 43910 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_CURRENT_DISTRIBUTION_MODE,
						SafetyParameterEnums.Vrt.CurrentDistributionMode.REACTIVE_POWER_PRIO) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_MODE,
						SafetyParameterEnums.Vrt.GeneralRecoveryMode.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_MODE_END,
						SafetyParameterEnums.Vrt.GeneralRecoveryMode.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_ENABLE, EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENABLE,
						EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SLOPE, 100000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SLOPE, 100000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_VALUE, 920) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME, 500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_VALUE, 1200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME, 1000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_VALUE, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME, 1500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_VALUE, 1100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME, 2000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_VALUE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME, 3000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_VALUE, 1150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME, 500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_VALUE, 850) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME, 1000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_VALUE, 1080) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME, 1500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_VALUE, 51000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME, 2000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_VALUE, 48500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME, 3000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_VALUE, 50200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME, 500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_VALUE, 49800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME, 1000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_VALUE, 51000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME, 1500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_VALUE, 48500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME, 2000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_VALUE, 50200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME, 3000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_VALUE, 49800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME, 500L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE, 920) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_STAGE_TRIP_TIME, 1000L) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SPEED, 50) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SPEED, 80) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_ENTER_THRESHOLD, 1200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_EXIT_ENDPOINT, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_K1_SLOPE, 5) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD, 1100) //

				// -- block starting at 44040 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_ENABLE, EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENABLE,
						EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_VOLTAGE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_TIME, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_VOLTAGE, 1150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_TIME, 1500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_VOLTAGE, 850) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_TIME, 2000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_VOLTAGE, 1080) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_TIME, 2500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_VOLTAGE, 920) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_TIME, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_VOLTAGE, 1200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_TIME, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_VOLTAGE, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_TIME, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_ENTER_HIGH_CROSSING, 1100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_EXIT_HIGH_CROSSING, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_K2_SLOPE, 8) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD, 1150) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_VOLTAGE, 850) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_TIME, 1000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_VOLTAGE, 1080) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_TIME, 1500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_VOLTAGE, 920) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_TIME, 2000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_VOLTAGE, 1200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_TIME, 2500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_VOLTAGE, 800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_TIME, 200) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_VOLTAGE, 1100) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_TIME, 300) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_VOLTAGE, 900) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_TIME, 500) //

				// -- block starting at 44150 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_ENABLE, EnableDisableOrUndefined.DISABLE) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF1_FREQUENCY, 4800) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF1_TIME, 1500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF2_FREQUENCY, 4500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF2_TIME, 2000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF3_FREQUENCY, 5124) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_UF3_TIME, 3000) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF1_FREQUENCY, 4174) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF1_TIME, 500) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF2_FREQUENCY, 4074) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF2_TIME, 1000) //

				// -- block starting at 44166 --
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF3_FREQUENCY, 5124) //
				.outputReadValue(GoodWePowerSetting.ChannelId.V2_FRT_OF3_TIME, 1500) //

		);
	}

	private static List<ChannelId> channelIdsExceptGoodWePowerSettings() {

		final var excludingChannels = Stream.of(//
				GoodWePowerSetting.ChannelId.V2_APM_GENERAL_POWER_GRADIENT, //
				GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_POWER, //
				GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_POWER, //
				GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_COS_PHI, //
				GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_IN_VOLTAGE, //
				GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_FIXED_Q //
		);

		final var debugChannels = Arrays.stream(GoodWePowerSetting.ChannelId.values())
				.filter(c -> c.toString().startsWith("DEBUG"));

		final var debugAndExcludingChannels = Stream.concat(debugChannels, excludingChannels);

		final var singles = Stream.of(//
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
				SymmetricBatteryInverter.ChannelId.REACTIVE_POWER, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY //
		);

		final var fromArrays = Stream.<ChannelId[]>of(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				HybridManagedSymmetricBatteryInverter.ChannelId.values(), //
				GoodWe.ChannelId.values(), //
				GoodWeBatteryInverter.ChannelId.values()) //
				.flatMap(Stream::of);

		return Stream.concat(debugAndExcludingChannels, Stream.concat(singles, fromArrays)).toList();
	}
}
