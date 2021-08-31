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
import io.openems.edge.goodwe.charger.GoodWeEtCharger1;
import io.openems.edge.goodwe.common.enums.BackupEnable;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.FeedPowerEnable;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

public class GoodWeBatteryInverterImplTest {

	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY_ID = "battery0";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String CHARGER_ID = "charger0";

	private static final Battery battery = new DummyBattery(BATTERY_ID);

	private static final ChannelAddress EMS_POWER_MODE = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerMode");
	private static final ChannelAddress EMS_POWER_SET = new ChannelAddress(BATTERY_INVERTER_ID, "EmsPowerSet");
	private static final ChannelAddress ACTUAL_POWER = new ChannelAddress(CHARGER_ID, "ActualPower");

	@Test
	public void testEt() throws Exception {
		GoodWeEtCharger1 charger = new GoodWeEtCharger1();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(io.openems.edge.goodwe.charger.MyConfig.create() //
						.setId(CHARGER_ID) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.build());

		GoodWeBatteryInverterImpl ess = new GoodWeBatteryInverterImpl();
		ess.addCharger(charger);
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum())
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setBackupEnable(BackupEnable.ENABLE) //
						.setFeedPowerEnable(FeedPowerEnable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setEmsPowerMode(EmsPowerMode.UNDEFINED) //
						.setEmsPowerSet(0) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.input(ACTUAL_POWER, 2000) //
						.onExecuteWriteCallbacks(() -> {
							ess.run(battery, 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

	@Test
	public void testNegativSetActivePoint() throws Exception {
		GoodWeBatteryInverterImpl ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum())
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setBackupEnable(BackupEnable.ENABLE) //
						.setFeedPowerEnable(FeedPowerEnable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setEmsPowerMode(EmsPowerMode.UNDEFINED) //
						.setEmsPowerSet(0) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.onExecuteWriteCallbacks(() -> {
							ess.run(battery, -1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.CHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

	@Test
	public void testDischargeBattery() throws Exception {
		GoodWeBatteryInverterImpl ess = new GoodWeBatteryInverterImpl();
		new ComponentTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("sum", new DummySum())
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(GoodWeConstants.DEFAULT_UNIT_ID) //
						.setSafetyCountry(SafetyCountry.GERMANY) //
						.setBackupEnable(BackupEnable.ENABLE) //
						.setFeedPowerEnable(FeedPowerEnable.ENABLE) //
						.setFeedPowerPara(3000) //
						.setFeedInPowerSettings(FeedInPowerSettings.PU_ENABLE_CURVE) //
						.setEmsPowerMode(EmsPowerMode.UNDEFINED) //
						.setEmsPowerSet(0) //
						.setControlMode(ControlMode.REMOTE) //
						.build()) //
				.next(new TestCase() //
						.onExecuteWriteCallbacks(() -> {
							ess.run(battery, 1000, 0);
						}) //
						.output(EMS_POWER_MODE, EmsPowerMode.DISCHARGE_BAT) //
						.output(EMS_POWER_SET, 1000));
	}

}
