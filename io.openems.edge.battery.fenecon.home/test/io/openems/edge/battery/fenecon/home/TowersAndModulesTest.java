package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
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
				.input(TOWER_2_BMS_SOFTWARE_VERSION, 0));
		checkDynamicChannels(battery, TOWERS, MODULES, CELLS);

		// add new module (1 tower, each tower 6 modules)
		componentTest.next(new TestCase() //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES + 1));
		checkDynamicChannels(battery, TOWERS, MODULES + 1, CELLS);

		// add new tower home (2 tower, each tower 6 modules)
		componentTest.next(new TestCase() //
				.input(TOWER_1_BMS_SOFTWARE_VERSION, 1));
		checkDynamicChannels(battery, TOWERS + 1, MODULES + 1, CELLS);
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
	 * @param battery the {@link Battery}
	 * @param towers  number of given towers
	 * @param modules number of given modules
	 * @param cells   number of given cells
	 */
	private static void checkDynamicChannels(Battery battery, int towers, int modules, int cells) {
		for (var tower = 0; tower < towers; tower++) {
			// check for each tower the serial number channel is existent
			battery.channel("Tower" + tower + "BmsSerialNumber");

			for (var module = 0; module < modules; module++) {
				// check for each tower and module the serial number channel is existent
				battery.channel("Tower" + tower + "Module" + module + "SerialNumber");

				for (var cell = 0; cell < cells; cell++) {
					// check for each tower, module and cell voltage and temperature channel are
					// existent
					battery.channel(getCellChannelName(tower, module, cell) + "Voltage");
					battery.channel(getCellChannelName(tower, module, cell) + "Temperature");
				}
			}
		}
	}

	/**
	 * Builds the cell channel name e.g. Tower0Module3Cell004.
	 *
	 * @param tower  number to use
	 * @param module number to use
	 * @param cell   number to user
	 * @return The cell channel name
	 */
	private static String getCellChannelName(int tower, int module, int cell) {
		return "Tower" + tower + "Module" + module + "Cell" + String.format("%03d", cell);
	}

}
