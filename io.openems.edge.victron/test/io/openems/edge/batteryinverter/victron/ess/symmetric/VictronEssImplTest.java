package io.openems.edge.batteryinverter.victron.ess.symmetric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;

/**
 * Tests for {@link VictronEssImpl}.
 */
public class VictronEssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void testChannelIdCount() {
		// Verify that all ChannelIds are defined
		var channelIds = VictronEss.ChannelId.values();
		assertNotNull(channelIds);
		// Should have many channels defined
		assertEquals(true, channelIds.length > 50);
	}

	@Test
	public void testChannelIdDoc() {
		// Verify that all ChannelIds have a doc
		for (var channelId : VictronEss.ChannelId.values()) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setAlias("Victron ESS") //
				.setEnabled(true) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(227) //
				.setPhase(SingleOrAllPhase.ALL) //
				.setDebugMode(false) //
				.setReadOnlyMode(false) //
				.setCapacity(10000) //
				.setMaxApparentPower(5000) //
				.build();

		assertEquals(ESS_ID, config.id());
		assertEquals("Victron ESS", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(227, config.modbusUnitId());
		assertEquals(SingleOrAllPhase.ALL, config.phase());
		assertEquals(false, config.debugMode());
		assertEquals(false, config.readOnlyMode());
		assertEquals(10000, config.capacity());
		assertEquals(5000, config.maxApparentPower());
	}

	@Test
	public void testConfigSinglePhaseL1() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L1) //
				.build();

		assertEquals(SingleOrAllPhase.L1, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL2() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L2) //
				.build();

		assertEquals(SingleOrAllPhase.L2, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL3() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L3) //
				.build();

		assertEquals(SingleOrAllPhase.L3, config.phase());
	}

	@Test
	public void testConfigReadOnlyMode() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setReadOnlyMode(true) //
				.build();

		assertEquals(true, config.readOnlyMode());
	}

	@Test
	public void testVictronEssConstructor() {
		var victronEss = new VictronEssImpl();
		assertNotNull(victronEss);
	}

	@Test
	public void testGetPowerPrecision() {
		var victronEss = new VictronEssImpl();
		assertEquals(100, victronEss.getPowerPrecision());
	}

}
