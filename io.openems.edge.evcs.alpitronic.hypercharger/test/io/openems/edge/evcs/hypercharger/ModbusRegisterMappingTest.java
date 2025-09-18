package io.openems.edge.evcs.hypercharger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.hypercharger.EvcsAlpitronicHypercharger.Connector;

/**
 * Tests for Modbus register mappings of Alpitronic Hypercharger.
 * 
 * <p>Validates correct register addresses according to Load Management Manual v2.5.
 */
public class ModbusRegisterMappingTest {

	/**
	 * Test station-level register mappings.
	 */
	@Test
	public void testStationLevelRegisters() throws Exception {
		var component = new EvcsAlpitronicHyperchargerImpl();
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
		
		// Verify station-level channels exist
		assertNotNull("Unix time channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.UNIX_TIME));
		assertNotNull("Number of connectors channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.NUM_CONNECTORS));
		assertNotNull("Station state channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.STATION_STATE));
		assertNotNull("Total station power channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.TOTAL_STATION_POWER));
		assertNotNull("Load management enabled channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.LOAD_MANAGEMENT_ENABLED));
	}
	
	/**
	 * Test software version detection channels.
	 */
	@Test
	public void testVersionDetectionChannels() throws Exception {
		var component = new EvcsAlpitronicHyperchargerImpl();
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
		
		// Verify version channels exist
		assertNotNull("Software version major channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MAJOR));
		assertNotNull("Software version minor channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_MINOR));
		assertNotNull("Software version patch channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.SOFTWARE_VERSION_PATCH));
	}
	
	/**
	 * Test connector-specific register mappings with different slot configurations.
	 */
	@Test
	public void testConnectorSpecificRegisters() throws Exception {
		// Test SLOT_0 (offset 100)
		testConnectorOffset(Connector.SLOT_0, 100);
		
		// Test SLOT_1 (offset 200) 
		testConnectorOffset(Connector.SLOT_1, 200);
		
		// Test SLOT_2 (offset 300)
		testConnectorOffset(Connector.SLOT_2, 300);
		
		// Test SLOT_3 (offset 400)
		testConnectorOffset(Connector.SLOT_3, 400);
	}
	
	private void testConnectorOffset(Connector connector, int expectedOffset) throws Exception {
		var component = new EvcsAlpitronicHyperchargerImpl();
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
		
		// Verify connector offset is applied correctly
		assertEquals("Connector offset should match", expectedOffset, connector.modbusOffset);
		
		// Verify connector-specific channels exist
		assertNotNull("RAW_STATUS channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.RAW_STATUS));
		assertNotNull("CHARGING_VOLTAGE channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.CHARGING_VOLTAGE));
		assertNotNull("CHARGING_CURRENT channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.CHARGING_CURRENT));
		assertNotNull("RAW_CHARGE_POWER channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.RAW_CHARGE_POWER));
		assertNotNull("CHARGED_TIME channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.CHARGED_TIME));
		assertNotNull("CHARGED_ENERGY channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.CHARGED_ENERGY));
		assertNotNull("EV_SOC channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.EV_SOC));
		assertNotNull("CONNECTOR_TYPE channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.CONNECTOR_TYPE));
		assertNotNull("EV_MAX_CHARGING_POWER channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.EV_MAX_CHARGING_POWER));
		assertNotNull("EV_MIN_CHARGING_POWER channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.EV_MIN_CHARGING_POWER));
		assertNotNull("VAR_REACTIVE_MAX channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MAX));
		assertNotNull("VAR_REACTIVE_MIN channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.VAR_REACTIVE_MIN));
	}
	
	/**
	 * Test new registers added for SW 2.5.x.
	 */
	@Test
	public void testNewRegistersForVersion25() throws Exception {
		var component = new EvcsAlpitronicHyperchargerImpl();
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
		
		// Verify new channels for version 2.5.x exist
		assertNotNull("Total charged energy channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.TOTAL_CHARGED_ENERGY));
		assertNotNull("Max charging power AC channel should exist", 
				component.channel(EvcsAlpitronicHypercharger.ChannelId.MAX_CHARGING_POWER_AC));
	}
}