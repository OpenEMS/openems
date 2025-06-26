package io.openems.edge.battery.fenecon.commercial;

import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.MASTER_MCU_HARDWARE_VERSION;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.NUMBER_OF_CELLS_PER_MODULE;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.NUMBER_OF_MODULES_PER_TOWER;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.NUMBER_OF_TOWERS;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercialImpl.VERSION_CONVERTER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.io.test.DummyInputOutput;

public class DynamicChannelsAndSerialNumbersTest {

	private static final int TOWERS = 1;
	private static final int MODULES = 10;
	private static final int CELLS = 120;/* Read from register as cells*modules */

	@Test
	public void testSerialNum() throws Exception {
		var battery = new BatteryFeneconCommercialImpl();

		var componentTest = new ComponentTest(battery) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setBatteryStartStopRelay("io0/InputOutput0")//
						.setStartStop(StartStopConfig.AUTO) //
						.build());

		// initial home (1 tower, each tower 5 modules)
		componentTest.next(new TestCase() //
				.input(NUMBER_OF_TOWERS, TOWERS) //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES) //
				.input(NUMBER_OF_CELLS_PER_MODULE, CELLS) //
				.input("battery0", "Tower0SubMasterHardwareVersion", "109101BM60"));
		checkDynamicChannels(battery, TOWERS, MODULES, CELLS / MODULES);

		assertEquals("011910MB06", VERSION_CONVERTER.elementToChannel("109101BM60"));

		componentTest.next(new TestCase());
		componentTest.next(new TestCase());
		componentTest.next(new TestCase());
		componentTest.next(new TestCase());

		componentTest.next(new TestCase() //
				.input(NUMBER_OF_TOWERS, TOWERS) //
				.input(NUMBER_OF_MODULES_PER_TOWER, MODULES) //
				.input(NUMBER_OF_CELLS_PER_MODULE, CELLS) //
				.input(MASTER_MCU_HARDWARE_VERSION, "100201MS50"));

		assertEquals("012010SM05", VERSION_CONVERTER.elementToChannel("100201MS50"));
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
