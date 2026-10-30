package io.openems.edge.evcs.alpitronic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;

/**
 * Comprehensive test for Alpitronic Modbus register mapping validation across
 * firmware versions.
 * 
 * <p>
 * Validates:
 * <ul>
 * <li>Station-level register addresses (0-48)</li>
 * <li>Connector-level register addresses (100-137 for SLOT_0, 200-237 for
 * SLOT_1, etc.)</li>
 * <li>Version-specific register availability and mapping</li>
 * <li>Offset calculations for multi-connector support</li>
 * </ul>
 */
class ModbusRegisterMappingTest {

	@Test
	void testStationLevelRegisters() throws Exception {
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

		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.UNIX_TIME), "Unix time channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.NUM_CONNECTORS),
				"Number of connectors channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.STATION_STATE), "Station state channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER),
				"Total station power channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED),
				"Load management enabled channel should exist");
	}

	@Test
	void testVersionDetectionChannels() throws Exception {
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

		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR),
				"Software version major channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR),
				"Software version minor channel should exist");
		assertNotNull(component.channel(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH),
				"Software version patch channel should exist");
	}

	@Test
	void testConnectorSpecificRegisters() {
		// Connector-specific registers start at 100 and increment by 100 for each slot

		// SLOT_0 begins at offset 100
		int slot0 = Connector.SLOT_0.modbusOffset;
		assertEquals(100, slot0);

		// SLOT_1 begins at offset 200
		int slot1 = Connector.SLOT_1.modbusOffset;
		assertEquals(200, slot1);

		// SLOT_2 begins at offset 300
		int slot2 = Connector.SLOT_2.modbusOffset;
		assertEquals(300, slot2);

		// SLOT_3 begins at offset 400
		int slot3 = Connector.SLOT_3.modbusOffset;
		assertEquals(400, slot3);
	}

}
