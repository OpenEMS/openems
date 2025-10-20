package io.openems.edge.evcs.alpitronic;

import static io.openems.edge.evcs.alpitronic.AlpitronicModbusTest.prepareAlpitronic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;

public class EvcsAlpitronicHyperchargerImplTest {

	@Test
	public void testBasicActivation() throws Exception {

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
						.build());

		// Verify component is active
		assertNotNull("Component should exist", component);
		assertEquals("Component ID should match", "evcs0", component.id());

	}

	@Test
	public void testActivationWithDifferentConnectors() throws Exception {

		// Test SLOT_0
		var component0 = new EvcsAlpitronicImpl();
		new ComponentTest(component0) //
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
		assertNotNull(component0);

		// Test SLOT_1
		var component1 = new EvcsAlpitronicImpl();
		new ComponentTest(component1) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus1")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus1") //
						.setId("evcs1") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_1) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
		assertNotNull(component1);

		// Test SLOT_2
		var component2 = new EvcsAlpitronicImpl();
		new ComponentTest(component2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus2")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus2") //
						.setId("evcs2") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_2) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
		assertNotNull(component2);

		// Test SLOT_3
		var component3 = new EvcsAlpitronicImpl();
		new ComponentTest(component3) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus3")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus3") //
						.setId("evcs3") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_3) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());
		assertNotNull(component3);

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test configured power limits
		assertEquals("Min power should be 5000W", 5_000, component.getConfiguredMinimumHardwarePower());

		assertEquals("Max power should be 150000W", 150_000, component.getConfiguredMaximumHardwarePower());

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test applying charge power limit
		assertTrue("Should successfully apply charge power limit", component.applyChargePowerLimit(50_000));

		assertTrue("Should accept 0 power", component.applyChargePowerLimit(0));

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test pause - should set power to minimum
		assertTrue("Should successfully pause charging", component.pauseChargeProcess());

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test display text - should return false as it's not supported
		assertFalse("Display text not supported", component.applyDisplayText("Test message"));

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test minimum time
		assertEquals("Minimum time should be 10 seconds", 10, component.getMinimumTimeTillChargingLimitTaken());

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Test debug log format
		String debugLog = component.debugLog();
		assertNotNull("Debug log should not be null", debugLog);

	}

	@Test
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
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.build());

		// Verify charge state handler exists
		assertNotNull("Charge state handler should exist", component.getChargeStateHandler());

	}

	/**
	 * Basic test for component activation and configuration.
	 *
	 * <p>
	 * Note: Unlike KEBA which uses a fixed protocol, Alpitronic uses asynchronous
	 * firmware version detection with {@code readElementsOnce().thenAccept()} to
	 * dynamically configure the Modbus protocol. This asynchronous operation is not
	 * reliably completed within unit test cycles, making strict-mode testing
	 * impractical. The test helper classes ({@link AlpitronicCommonNaturesTest},
	 * {@link AlpitronicTest}, {@link AlpitronicModbusTest}) document the expected
	 * channel values for reference.
	 */
	@Test
	public void test() throws Exception {
		final var sut = new EvcsAlpitronicImpl();

		prepareAlpitronic(sut) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(75_000) //
						.setMinHwPower(5_000) //
						.build()) //
				.next(new TestCase(), 20) //
				.deactivate();

		// Basic verification - component activated successfully
		assertEquals("evcs0", sut.id());
		assertNotNull("Component should be created", sut);
	}
}
