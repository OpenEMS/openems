package io.openems.edge.battery.fenecon.commercial;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.io.test.DummyInputOutput;

public class DynamicChannelsAndSerialNumbersTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final String IO_ID = "io0";

	private static final int TOWERS = 1;
	private static final int MODULES = 10;
	private static final int CELLS = 120;/* Read from register as cells*modules */

	private static final ChannelAddress NUMBER_OF_MODULES_PER_TOWER = new ChannelAddress(BATTERY_ID,
			"NumberOfModulesPerTower");
	private static final ChannelAddress NUMBER_OF_TOWERS = new ChannelAddress(BATTERY_ID, "NumberOfTowers");
	private static final ChannelAddress NUMBER_OF_CELLS_PER_MODULE = new ChannelAddress(BATTERY_ID,
			"NumberOfCellsPerModule");
	private static final ChannelAddress SUB_MASTER_HARDWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"Tower0SubMasterHardwareVersion");
	private static final ChannelAddress MASTER_MCU_HARDWARE_VERSION = new ChannelAddress(BATTERY_ID,
			"MasterMcuHardwareVersion");

	@Test
	public void testSerialNum() throws Exception {
		var battery = new BatteryFeneconCommercialImpl();

		var componentTest = new ComponentTest(battery) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setBatteryStartStopRelay("io0/InputOutput0")//
						.setStartStop(StartStopConfig.AUTO) //
						.build());

		// initial home (1 tower, each tower 5 modules)
		componentTest.next(new TestCase() //
				.input(NUMBER_OF_TOWERS, TOWERS) //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES) //
				.input(NUMBER_OF_CELLS_PER_MODULE, CELLS) //
				.input(SUB_MASTER_HARDWARE_VERSION, "109101BM60"));
		checkDynamicChannels(battery, TOWERS, MODULES, CELLS / MODULES);

		assertEquals("011910MB06", BatteryFeneconCommercialImpl.VERSION_CONVERTER.elementToChannel("109101BM60"));

		componentTest.next(new TestCase());
		componentTest.next(new TestCase());
		componentTest.next(new TestCase());
		componentTest.next(new TestCase());

		componentTest.next(new TestCase() //
				.input(NUMBER_OF_TOWERS, TOWERS) //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES) //
				.input(NUMBER_OF_CELLS_PER_MODULE, CELLS) //
				.input(MASTER_MCU_HARDWARE_VERSION, "100201MS50"));

		assertEquals("012010SM05", BatteryFeneconCommercialImpl.VERSION_CONVERTER.elementToChannel("100201MS50"));
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
			// check for each tower the sub master hardware/software serial number channel
			// is existent
			battery.channel("Tower" + tower + "SubMasterHardwareVersion");
			battery.channel("Tower" + tower + "SubMasterFirmwareVersion");

			for (var module = 0; module < modules; module++) {
				for (var cell = 0; cell < cells; cell++) {
					// check for each tower, module and cell voltage and temperature channel are
					// existent
					battery.channel(getCellChannelName(tower, module, cell) + "Voltage");
				}
				// There are only 8 temperature sensors
				for (var c = 0; c < 8; c++) {
					battery.channel(getCellChannelName(tower, module, c) + "Temperature");
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
