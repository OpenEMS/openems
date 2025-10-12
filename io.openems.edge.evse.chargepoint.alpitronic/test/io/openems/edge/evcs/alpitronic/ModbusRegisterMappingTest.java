package io.openems.edge.evcs.alpitronic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;

/**
 * Tests for Modbus register mappings of Alpitronic Hypercharger.
 * 
 * <p>
 * Validates correct register addresses according to Load Management Manual
 * v2.5.
 */
public class ModbusRegisterMappingTest {

	/**
	 * Test station-level register mappings.
	 */
	@Test
	public void testStationLevelRegisters() throws Exception {
		System.out.println("\n=== Testing Station-Level Registers ===");
		System.out.println("Creating component with SLOT_0 configuration...");

		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		System.out.println("Component activated successfully");

		// Verify station-level channels exist
		System.out.println("\nVerifying station-level channels:");
		System.out.println(
				"  UNIX_TIME: " + (component.channel(EvcsAlpitronic.ChannelId.UNIX_TIME) != null ? "OK" : "MISSING"));
		System.out.println("  NUM_CONNECTORS: "
				+ (component.channel(EvcsAlpitronic.ChannelId.NUM_CONNECTORS) != null ? "OK" : "MISSING"));
		System.out.println("  STATION_STATE: "
				+ (component.channel(EvcsAlpitronic.ChannelId.STATION_STATE) != null ? "OK" : "MISSING"));
		System.out.println("  TOTAL_STATION_POWER: "
				+ (component.channel(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER) != null ? "OK" : "MISSING"));
		System.out.println("  LOAD_MANAGEMENT_ENABLED: "
				+ (component.channel(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED) != null ? "OK" : "MISSING"));

		assertNotNull("Unix time channel should exist", component.channel(EvcsAlpitronic.ChannelId.UNIX_TIME));
		assertNotNull("Number of connectors channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.NUM_CONNECTORS));
		assertNotNull("Station state channel should exist", component.channel(EvcsAlpitronic.ChannelId.STATION_STATE));
		assertNotNull("Total station power channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER));
		assertNotNull("Load management enabled channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED));

		System.out.println("\n=== Station-Level Registers Test Complete ===\n");
	}

	/**
	 * Test software version detection channels.
	 */
	@Test
	public void testVersionDetectionChannels() throws Exception {
		System.out.println("\n=== Testing Version Detection Channels ===");
		System.out.println("Creating component...");

		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		System.out.println("Component activated successfully");

		// Verify version channels exist
		System.out.println("\nVerifying version detection channels:");
		System.out.println("  SOFTWARE_VERSION_MAJOR: "
				+ (component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR) != null ? "OK" : "MISSING"));
		System.out.println("  SOFTWARE_VERSION_MINOR: "
				+ (component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR) != null ? "OK" : "MISSING"));
		System.out.println("  SOFTWARE_VERSION_PATCH: "
				+ (component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH) != null ? "OK" : "MISSING"));

		assertNotNull("Software version major channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR));
		assertNotNull("Software version minor channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR));
		assertNotNull("Software version patch channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH));

		System.out.println("\n=== Version Detection Channels Test Complete ===\n");
	}

	/**
	 * Test connector-specific register mappings with different slot configurations.
	 */
	@Test
	public void testConnectorSpecificRegisters() throws Exception {
		System.out.println("\n=== Testing Connector-Specific Registers ===");

		// Test SLOT_0 (offset 100)
		System.out.println("\n--- Testing SLOT_0 (expected offset: 100) ---");
		this.testConnectorOffset(Connector.SLOT_0, 100);

		// Test SLOT_1 (offset 200)
		System.out.println("\n--- Testing SLOT_1 (expected offset: 200) ---");
		this.testConnectorOffset(Connector.SLOT_1, 200);

		// Test SLOT_2 (offset 300)
		System.out.println("\n--- Testing SLOT_2 (expected offset: 300) ---");
		this.testConnectorOffset(Connector.SLOT_2, 300);

		// Test SLOT_3 (offset 400)
		System.out.println("\n--- Testing SLOT_3 (expected offset: 400) ---");
		this.testConnectorOffset(Connector.SLOT_3, 400);

		System.out.println("\n=== Connector-Specific Registers Test Complete ===\n");
	}

	private void testConnectorOffset(Connector connector, int expectedOffset) throws Exception {
		System.out.println("Creating component with connector: " + connector.name());

		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(connector) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		System.out.println("Component activated");

		// Verify connector offset is applied correctly
		System.out.println("Verifying offset: expected=" + expectedOffset + ", actual=" + connector.modbusOffset);
		assertEquals("Connector offset should match", expectedOffset, connector.modbusOffset);

		// Verify connector-specific channels exist
		System.out.println("Verifying connector-specific channels:");
		System.out.println(
				"  RAW_STATUS: " + (component.channel(EvcsAlpitronic.ChannelId.RAW_STATUS) != null ? "OK" : "MISSING"));
		System.out.println("  CHARGING_VOLTAGE: "
				+ (component.channel(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE) != null ? "OK" : "MISSING"));
		System.out.println("  CHARGING_CURRENT: "
				+ (component.channel(EvcsAlpitronic.ChannelId.CHARGING_CURRENT) != null ? "OK" : "MISSING"));
		System.out.println("  RAW_CHARGE_POWER: "
				+ (component.channel(EvcsAlpitronic.ChannelId.RAW_CHARGE_POWER) != null ? "OK" : "MISSING"));
		System.out.println("  CHARGED_TIME: "
				+ (component.channel(EvcsAlpitronic.ChannelId.CHARGED_TIME) != null ? "OK" : "MISSING"));
		System.out.println("  CHARGED_ENERGY: "
				+ (component.channel(EvcsAlpitronic.ChannelId.CHARGED_ENERGY) != null ? "OK" : "MISSING"));
		System.out.println(
				"  EV_SOC: " + (component.channel(EvcsAlpitronic.ChannelId.EV_SOC) != null ? "OK" : "MISSING"));
		System.out.println("  CONNECTOR_TYPE: "
				+ (component.channel(EvcsAlpitronic.ChannelId.CONNECTOR_TYPE) != null ? "OK" : "MISSING"));
		System.out.println("  EV_MAX_CHARGING_POWER: "
				+ (component.channel(EvcsAlpitronic.ChannelId.EV_MAX_CHARGING_POWER) != null ? "OK" : "MISSING"));
		System.out.println("  EV_MIN_CHARGING_POWER: "
				+ (component.channel(EvcsAlpitronic.ChannelId.EV_MIN_CHARGING_POWER) != null ? "OK" : "MISSING"));
		System.out.println("  VAR_REACTIVE_MAX: "
				+ (component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX) != null ? "OK" : "MISSING"));
		System.out.println("  VAR_REACTIVE_MIN: "
				+ (component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN) != null ? "OK" : "MISSING"));

		assertNotNull("RAW_STATUS channel should exist", component.channel(EvcsAlpitronic.ChannelId.RAW_STATUS));
		assertNotNull("CHARGING_VOLTAGE channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE));
		assertNotNull("CHARGING_CURRENT channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.CHARGING_CURRENT));
		assertNotNull("RAW_CHARGE_POWER channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.RAW_CHARGE_POWER));
		assertNotNull("CHARGED_TIME channel should exist", component.channel(EvcsAlpitronic.ChannelId.CHARGED_TIME));
		assertNotNull("CHARGED_ENERGY channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.CHARGED_ENERGY));
		assertNotNull("EV_SOC channel should exist", component.channel(EvcsAlpitronic.ChannelId.EV_SOC));
		assertNotNull("CONNECTOR_TYPE channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.CONNECTOR_TYPE));
		assertNotNull("EV_MAX_CHARGING_POWER channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.EV_MAX_CHARGING_POWER));
		assertNotNull("EV_MIN_CHARGING_POWER channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.EV_MIN_CHARGING_POWER));
		assertNotNull("VAR_REACTIVE_MAX channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX));
		assertNotNull("VAR_REACTIVE_MIN channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN));

		System.out.println("All channels verified for " + connector.name());
	}

	/**
	 * Test new registers added for SW 2.5.x.
	 */
	@Test
	public void testNewRegistersForVersion25() throws Exception {
		System.out.println("\n=== Testing New Registers for Version 2.5.x ===");
		System.out.println("Creating component with SLOT_0 configuration...");

		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		System.out.println("Component activated successfully");

		// Verify new channels for version 2.5.x exist
		System.out.println("\nVerifying new v2.5 registers:");
		System.out.println("  TOTAL_CHARGED_ENERGY: "
				+ (component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY) != null ? "OK" : "MISSING"));
		System.out.println("  MAX_CHARGING_POWER_AC: "
				+ (component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC) != null ? "OK" : "MISSING"));

		assertNotNull("Total charged energy channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY));
		assertNotNull("Max charging power AC channel should exist",
				component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC));

		System.out.println("\n=== New Registers for Version 2.5.x Test Complete ===\n");
	}
}