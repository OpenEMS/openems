package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class TowersAndModulesTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress NUMBER_OF_MODULES_PER_TOWER = new ChannelAddress(BATTERY_ID,
			"NumberOfModulesPerTower");
	private static final ChannelAddress TOWER_0_BMS_SOFTWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower0BmsSoftwareVersion");
	private static final ChannelAddress TOWER_1_BMS_SOFTWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower1BmsSoftwareVersion");
	private static final ChannelAddress TOWER_2_BMS_SOFTWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower2BmsSoftwareVersion");
	private static final ChannelAddress TOWER_3_BMS_SOFTWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower3BmsSoftwareVersion");
	private static final ChannelAddress TOWER_4_BMS_SOFTWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower4BmsSoftwareVersion");
	private static final ChannelAddress BATTERY_HARDWARE_TYPE = new ChannelAddress(BATTERY_ID, "BatteryHardwareType");

	private static final int TOWERS = 1;
	private static final int MODULES = 5;
	private static final int CELLS = 14;

	@Test
	public void testChannelsCreatedDynamically() throws Exception {
		var battery = new BatteryFeneconHomeImpl();
		var componentTest = new ComponentTest(battery) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setBatteryStartUpRelay("io0/Relay4") //
						.setStartStop(StartStopConfig.AUTO) //
						.build());

		// initial home (1 tower, each tower 5 modules)
		componentTest.next(new TestCase() //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES) //
				.input(TOWER_0_BMS_SOFTWARE_VERSION, 1) //
				.input(TOWER_1_BMS_SOFTWARE_VERSION, 0) //
				.input(TOWER_2_BMS_SOFTWARE_VERSION, 0) //
				.input(TOWER_3_BMS_SOFTWARE_VERSION, 0) //
				.input(TOWER_4_BMS_SOFTWARE_VERSION, 0) //
				.input(BATTERY_HARDWARE_TYPE, BatteryFeneconHomeHardwareType.BATTERY_52));
		checkDynamicChannels(battery, TOWERS, MODULES, CELLS, BatteryFeneconHomeHardwareType.BATTERY_52);

		// add new module (1 tower, each tower 6 modules)
		componentTest.next(new TestCase() //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES + 1));
		checkDynamicChannels(battery, TOWERS, MODULES + 1, CELLS, BatteryFeneconHomeHardwareType.BATTERY_52);

		// add new tower home (2 tower, each tower 6 modules)
		componentTest.next(new TestCase() //
				.input(TOWER_1_BMS_SOFTWARE_VERSION, 1));
		checkDynamicChannels(battery, TOWERS + 1, MODULES + 1, CELLS, BatteryFeneconHomeHardwareType.BATTERY_52);
	}

	@Test
	public void testSerialNumberFormatterForBms() {
		assertEquals("519100001009210104000035", //
				BatteryFeneconHomeImpl.buildSerialNumber("519100001009", 707002403));
	}

	@Test
	public void testSerialNumberFormatterForOldBms() {
		assertNull(BatteryFeneconHomeImpl.buildSerialNumber("519100001009", 0));
	}

	@Test
	public void testSerialNumberFormatterForBattery() {
		assertEquals("519110001210201219000039", //
				BatteryFeneconHomeImpl.buildSerialNumber("519110001210", 697499687));
	}

	/**
	 * Check if all dynamic channels depending on tower, modules and cells
	 * parameters are created. If channel not exists an exception will be thrown and
	 * the test fails.
	 *
	 * @param battery      the {@link Battery}
	 * @param towers       number of given towers
	 * @param modules      number of given modules
	 * @param cells        number of given cells
	 * @param hardwareType hardware type
	 */
	private static void checkDynamicChannels(Battery battery, int towers, int modules, int cells,
			BatteryFeneconHomeHardwareType hardwareType) {
		for (var tower = 0; tower < towers; tower++) {
			// check for each tower the serial number channel is existent
			battery.channel("Tower" + tower + "BmsSerialNumber");

			for (var module = 0; module < modules; module++) {
				// check for each tower and module the serial number channel is existent
				battery.channel("Tower" + tower + "Module" + module + "SerialNumber");

				// check for each tower, module and cell voltage
				for (var cell = 0; cell < hardwareType.cellsPerModule; cell++) {
					battery.channel(ChannelId.channelIdUpperToCamel(
							BatteryFeneconHomeImpl.generateCellVoltageChannelName(tower, module, cell)));
				}
				// check for each tower, module and temperature sensor
				for (var sensor = 0; sensor < hardwareType.tempSensorsPerModule; sensor++) {
					battery.channel(ChannelId.channelIdUpperToCamel(
							BatteryFeneconHomeImpl.generateTempSensorChannelName(tower, module, sensor + 1)));
				}
				// check for each tower, module and temperature balancing
				for (var balancing = 0; balancing < 2; balancing++) {
					battery.channel(ChannelId.channelIdUpperToCamel(
							BatteryFeneconHomeImpl.generateTempBalancingChannelName(tower, module, balancing + 1)));
				}
			}
		}
	}
}
