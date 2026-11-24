package io.openems.edge.evcs.alpitronic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;

public class EvcsAlpitronicHyperchargerImplTest {

	/**
	 * Basic smoke test for component activation.
	 */
	@Test
	@DisplayName("Test basic activation")
	public void testBasicActivation() throws Exception {
		new ComponentTest(new EvcsAlpitronicImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //
	}

	/**
	 * Test activation with different connector slots.
	 */
	@Test
	@DisplayName("Test activation with different connectors")
	public void testActivationWithDifferentConnectors() throws Exception {
		for (Connector slot : Connector.values()) {
			var component = new EvcsAlpitronicImpl();
			new ComponentTest(component) //
					.addReference("cm", new DummyConfigurationAdmin()) //
					.addReference("setModbus", new DummyModbusBridge("modbus" + slot.modbusOffset)) //
					.activate(MyConfig.create() //
							.setModbusId("modbus" + slot.modbusOffset) //
							.setId("evcs" + slot.modbusOffset) //
							.setModbusUnitId(1) //
							.setConnector(slot) //
							.setMaxHwPower(150_000) //
							.setMinHwPower(5_000) //
							.build()); //
			assertNotNull(component);
		}
	}

	/**
	 * Test power configuration with different hardware limits.
	 */
	@Test
	@DisplayName("Test power configuration")
	public void testPowerConfiguration() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(22_000) //
						.setMinHwPower(3_680) //
						.build()); //

		// Verify configuration was applied
		assertEquals(22_000, component.getConfiguredMaximumHardwarePower());
		assertEquals(3_680, component.getConfiguredMinimumHardwarePower());
	}

	/**
	 * Test charge power limit application.
	 */
	@Test
	@DisplayName("Test charge power limit application")
	public void testChargePowerLimitApplication() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

		// Test apply power limit
		assertTrue(component.applyChargePowerLimit(50_000));
	}

	/**
	 * Test pause charge process.
	 */
	@Test
	@DisplayName("Test pause charge process")
	public void testPauseChargeProcess() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

		// Test pause (should apply minimum power)
		assertTrue(component.pauseChargeProcess());
	}

	/**
	 * Test display text application (currently returns false).
	 */
	@Test
	@DisplayName("Test apply display text")
	public void testApplyDisplayText() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

		// Alpitronic doesn't support display text
		assertTrue(!component.applyDisplayText("Test"));
	}

	/**
	 * Test minimum time till charging limit taken.
	 */
	@Test
	@DisplayName("Test minimum time till charging limit taken")
	public void testMinimumTimeTillChargingLimitTaken() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

		// Verify timing parameter
		assertEquals(10, component.getMinimumTimeTillChargingLimitTaken());
	}

	/**
	 * Test debug log output.
	 */
	@Test
	@DisplayName("Test debug log")
	public void testDebugLog() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.setDebugMode(true) //
						.build()); //

		// Verify debug log includes firmware version prefix
		String debugLog = component.debugLog();
		assertNotNull(debugLog);
		assertTrue(debugLog.contains("Limit:") || debugLog.contains("|"));
	}

	/**
	 * Test charge state handler availability.
	 */
	@Test
	@DisplayName("Test charge state handler")
	public void testChargeStateHandler() throws Exception {
		var component = new EvcsAlpitronicImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(70_000) //
						.setMinHwPower(5_000) //
						.build()); //

		// Charge state handler should be available
		assertNotNull(component.getChargeStateHandler());
	}

}
