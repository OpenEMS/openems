package io.openems.edge.evcs.alpitronic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alpitronic.enums.Connector;
import io.openems.edge.meter.api.PhaseRotation;

public class EvcsAlpitronicHyperchargerImplTest {

	@Test
	public void testBasicActivation() throws Exception {
		System.out.println("\n=== Testing Basic Activation ===");

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

		System.out.println("Basic activation test passed");
		System.out.println("=== Basic Activation Test Complete ===\n");
	}

	@Test
	public void testActivationWithDifferentConnectors() throws Exception {
		System.out.println("\n=== Testing Activation with Different Connectors ===");

		// Test SLOT_0
		System.out.println("Testing SLOT_0...");
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
		System.out.println("Testing SLOT_1...");
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
		System.out.println("Testing SLOT_2...");
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
		System.out.println("Testing SLOT_3...");
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

		System.out.println("All connector activation tests passed");
		System.out.println("=== Different Connectors Test Complete ===\n");
	}

	@Test
	public void testPowerConfiguration() throws Exception {
		System.out.println("\n=== Testing Power Configuration ===");

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
		System.out.println("Testing getConfiguredMinimumHardwarePower()");
		assertEquals("Min power should be 5000W", 5_000, component.getConfiguredMinimumHardwarePower());

		System.out.println("Testing getConfiguredMaximumHardwarePower()");
		assertEquals("Max power should be 150000W", 150_000, component.getConfiguredMaximumHardwarePower());

		System.out.println("Power configuration tests passed");
		System.out.println("=== Power Configuration Test Complete ===\n");
	}

	@Test
	public void testChargePowerLimitApplication() throws Exception {
		System.out.println("\n=== Testing Charge Power Limit Application ===");

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
		System.out.println("Testing applyChargePowerLimit(50000)");
		assertTrue("Should successfully apply charge power limit", component.applyChargePowerLimit(50_000));

		System.out.println("Testing applyChargePowerLimit(0) - should work");
		assertTrue("Should accept 0 power", component.applyChargePowerLimit(0));

		System.out.println("Charge power limit tests passed");
		System.out.println("=== Charge Power Limit Test Complete ===\n");
	}

	@Test
	public void testPauseChargeProcess() throws Exception {
		System.out.println("\n=== Testing Pause Charge Process ===");

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
		System.out.println("Testing pauseChargeProcess() - should set to minHwPower");
		assertTrue("Should successfully pause charging", component.pauseChargeProcess());

		System.out.println("Pause charge process test passed");
		System.out.println("=== Pause Charge Process Test Complete ===\n");
	}

	@Test
	public void testPhaseRotation() throws Exception {
		System.out.println("\n=== Testing Phase Rotation ===");

		// Test with default phase rotation (L1_L2_L3)
		System.out.println("Testing default phase rotation L1_L2_L3");
		var component1 = new EvcsAlpitronicImpl();
		new ComponentTest(component1) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus0") //
						.setId("evcs0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.build());

		assertEquals("Phase rotation should be L1_L2_L3", PhaseRotation.L1_L2_L3, component1.getPhaseRotation());

		// Test with L1_L3_L2
		System.out.println("Testing phase rotation L1_L3_L2");
		var component2 = new EvcsAlpitronicImpl();
		new ComponentTest(component2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus1")) //
				.activate(MyConfig.create() //
						.setModbusId("modbus1") //
						.setId("evcs1") //
						.setModbusUnitId(1) //
						.setConnector(Connector.SLOT_0) //
						.setMaxHwPower(150_000) //
						.setMinHwPower(5_000) //
						.setPhaseRotation(PhaseRotation.L2_L3_L1) //
						.build());

		assertEquals("Phase rotation should be L1_L3_L2", PhaseRotation.L2_L3_L1, component2.getPhaseRotation());

		System.out.println("Phase rotation tests passed");
		System.out.println("=== Phase Rotation Test Complete ===\n");
	}

	@Test
	public void testApplyDisplayText() throws Exception {
		System.out.println("\n=== Testing Apply Display Text ===");

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
		System.out.println("Testing applyDisplayText() - should return false (not supported)");
		assertFalse("Display text not supported", component.applyDisplayText("Test message"));

		System.out.println("Apply display text test passed");
		System.out.println("=== Apply Display Text Test Complete ===\n");
	}

	@Test
	public void testMinimumTimeTillChargingLimitTaken() throws Exception {
		System.out.println("\n=== Testing Minimum Time Till Charging Limit Taken ===");

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
		System.out.println("Testing getMinimumTimeTillChargingLimitTaken()");
		assertEquals("Minimum time should be 10 seconds", 10, component.getMinimumTimeTillChargingLimitTaken());

		System.out.println("Minimum time test passed");
		System.out.println("=== Minimum Time Test Complete ===\n");
	}

	@Test
	public void testDebugLog() throws Exception {
		System.out.println("\n=== Testing Debug Log ===");

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
		System.out.println("Testing debugLog()");
		String debugLog = component.debugLog();
		assertNotNull("Debug log should not be null", debugLog);
		System.out.println("Debug log output: " + debugLog);

		System.out.println("Debug log test passed");
		System.out.println("=== Debug Log Test Complete ===\n");
	}

	@Test
	public void testChargeStateHandler() throws Exception {
		System.out.println("\n=== Testing Charge State Handler ===");

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
		System.out.println("Testing getChargeStateHandler()");
		assertNotNull("Charge state handler should exist", component.getChargeStateHandler());

		System.out.println("Charge state handler test passed");
		System.out.println("=== Charge State Handler Test Complete ===\n");
	}
}
