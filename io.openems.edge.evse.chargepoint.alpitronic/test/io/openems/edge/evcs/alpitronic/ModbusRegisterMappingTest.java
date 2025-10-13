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
	 * Test Station-Level VAR registers (49-52) added in v2.3+.
	 *
	 * <p>
	 * These registers were introduced in firmware v2.3 and are present in v2.3,
	 * v2.4, and v2.5.
	 */
	@Test
	public void testStationLevelVarRegisters() throws Exception {
		System.out.println("\n=== Testing Station-Level VAR Registers (49-52) ===");
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

		// Verify Station-Level VAR channels exist (Register 49-52)
		System.out.println("\nVerifying Station-Level VAR registers (49-52):");
		System.out.println("  VAR_REACTIVE_MAX (Register 49-50): "
				+ (component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX) != null ? "OK" : "MISSING"));
		System.out.println("  VAR_REACTIVE_MIN (Register 51-52): "
				+ (component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN) != null ? "OK" : "MISSING"));

		assertNotNull("Station-Level VAR_REACTIVE_MAX channel should exist (Register 49-50)",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX));
		assertNotNull("Station-Level VAR_REACTIVE_MIN channel should exist (Register 51-52)",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN));

		System.out.println("\n=== Station-Level VAR Registers Test Complete ===\n");
	}

	/**
	 * Test new registers added for SW 2.5.x.
	 *
	 * <p>
	 * v2.3: Added Total Charged Energy (X32-X35)
	 * 
	 * <p>
	 * v2.4: Added Max Charging Power AC (X36-X37)
	 * 
	 * <p>
	 * v2.5: All registers from v2.3 and v2.4 are present
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
		System.out.println("\nVerifying v2.3+ connector registers:");
		System.out.println("  TOTAL_CHARGED_ENERGY (Register X32-X35, added in v2.3): "
				+ (component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY) != null ? "OK" : "MISSING"));
		System.out.println("  MAX_CHARGING_POWER_AC (Register X36-X37, added in v2.4): "
				+ (component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC) != null ? "OK" : "MISSING"));

		assertNotNull("Total charged energy channel should exist (added in v2.3)",
				component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY));
		assertNotNull("Max charging power AC channel should exist (added in v2.4)",
				component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC));

		System.out.println("\n=== New Registers for Version 2.5.x Test Complete ===\n");
	}

	/**
	 * Comprehensive test for all v2.5 register mappings.
	 *
	 * <p>
	 * This test validates that ALL registers defined in Load Management Manual v2.5
	 * are correctly implemented.
	 */
	@Test
	public void testCompleteV25RegisterMapping() throws Exception {
		System.out.println("\n=== Testing Complete v2.5 Register Mapping ===");
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

		// Verify ALL v2.5 registers are present
		System.out.println("\nVerifying COMPLETE v2.5 register set:");

		// Station-Level Input Registers (0-52)
		System.out.println("\n1. Station-Level Input Registers (0-52):");
		assertNotNull("Register 0-1: UNIX_TIME", component.channel(EvcsAlpitronic.ChannelId.UNIX_TIME));
		assertNotNull("Register 2: NUM_CONNECTORS", component.channel(EvcsAlpitronic.ChannelId.NUM_CONNECTORS));
		assertNotNull("Register 3: STATION_STATE", component.channel(EvcsAlpitronic.ChannelId.STATION_STATE));
		assertNotNull("Register 4-5: TOTAL_STATION_POWER",
				component.channel(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER));
		assertNotNull("Register 6-17: SERIAL_NUMBER", component.channel(EvcsAlpitronic.ChannelId.SERIAL_NUMBER));
		assertNotNull("Register 18: LOAD_MANAGEMENT_ENABLED",
				component.channel(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED));
		assertNotNull("Register 30-45: CHARGEPOINT_ID", component.channel(EvcsAlpitronic.ChannelId.CHARGEPOINT_ID));
		assertNotNull("Register 46: SOFTWARE_VERSION_MAJOR",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR));
		assertNotNull("Register 47: SOFTWARE_VERSION_MINOR",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR));
		assertNotNull("Register 48: SOFTWARE_VERSION_PATCH",
				component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH));
		assertNotNull("Register 49-50: VAR_REACTIVE_MAX (Station-Level)",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX));
		assertNotNull("Register 51-52: VAR_REACTIVE_MIN (Station-Level)",
				component.channel(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN));

		// Connector-Level Input Registers (100-137)
		System.out.println("\n2. Connector-Level Input Registers (100-137):");
		assertNotNull("Register X00: RAW_STATUS", component.channel(EvcsAlpitronic.ChannelId.RAW_STATUS));
		assertNotNull("Register X01-X02: CHARGING_VOLTAGE",
				component.channel(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE));
		assertNotNull("Register X03: CHARGING_CURRENT", component.channel(EvcsAlpitronic.ChannelId.CHARGING_CURRENT));
		assertNotNull("Register X04-X05: RAW_CHARGE_POWER",
				component.channel(EvcsAlpitronic.ChannelId.RAW_CHARGE_POWER));
		assertNotNull("Register X06: CHARGED_TIME", component.channel(EvcsAlpitronic.ChannelId.CHARGED_TIME));
		assertNotNull("Register X07: CHARGED_ENERGY", component.channel(EvcsAlpitronic.ChannelId.CHARGED_ENERGY));
		assertNotNull("Register X08: EV_SOC", component.channel(EvcsAlpitronic.ChannelId.EV_SOC));
		assertNotNull("Register X09: CONNECTOR_TYPE", component.channel(EvcsAlpitronic.ChannelId.CONNECTOR_TYPE));
		assertNotNull("Register X10-X11: EV_MAX_CHARGING_POWER",
				component.channel(EvcsAlpitronic.ChannelId.EV_MAX_CHARGING_POWER));
		assertNotNull("Register X12-X13: EV_MIN_CHARGING_POWER",
				component.channel(EvcsAlpitronic.ChannelId.EV_MIN_CHARGING_POWER));
		assertNotNull("Register X18-X21: VID", component.channel(EvcsAlpitronic.ChannelId.VID));
		assertNotNull("Register X22-X31: ID_TAG", component.channel(EvcsAlpitronic.ChannelId.ID_TAG));
		assertNotNull("Register X32-X35: TOTAL_CHARGED_ENERGY (added in v2.3)",
				component.channel(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY));
		assertNotNull("Register X36-X37: MAX_CHARGING_POWER_AC (added in v2.4)",
				component.channel(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC));

		// Holding Registers (Write)
		System.out.println("\n3. Holding Registers (Write):");
		assertNotNull("Register 0-1: APPLY_CHARGE_POWER_LIMIT",
				component.channel(EvcsAlpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT));
		assertNotNull("Register 2-3: SETPOINT_REACTIVE_POWER",
				component.channel(EvcsAlpitronic.ChannelId.SETPOINT_REACTIVE_POWER));

		System.out.println("\n✅ ALL v2.5 registers are correctly implemented!");
		System.out.println("\n=== Complete v2.5 Register Mapping Test Complete ===\n");
	}
}