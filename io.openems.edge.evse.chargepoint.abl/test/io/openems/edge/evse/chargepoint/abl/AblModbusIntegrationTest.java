package io.openems.edge.evse.chargepoint.abl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator;

/**
 * Integration test for ABL component with Modbus simulator.
 *
 * <p>
 * This test demonstrates how to use the Modbus simulator for integration
 * testing of the real ABL component implementation.
 */
public class AblModbusIntegrationTest {

	private AblModbusSimulator simulator;
	private static final int TEST_PORT = 15502; // Non-standard port for testing

	@Before
	public void setUp() throws Exception {
		// Start Modbus simulator on localhost
		this.simulator = new AblModbusSimulator("127.0.0.1", TEST_PORT, 1);
		this.simulator.start();

		// Wait for simulator to be ready
		Thread.sleep(500);
	}

	@After
	public void tearDown() {
		if (this.simulator != null) {
			this.simulator.stop();
		}
	}

	@Test
	public void testSimulatorInitialState() {
		// Verify simulator starts in state A1
		assertEquals(ChargingState.A1, this.simulator.getCurrentState());
		assertEquals(false, this.simulator.isEvConnected());
	}

	@Test
	public void testEvConnection() throws InterruptedException {
		// Simulate EV connection
		this.simulator.connectEv();
		Thread.sleep(100);

		// Verify state transition
		assertEquals(ChargingState.B1, this.simulator.getCurrentState());
		assertTrue(this.simulator.isEvConnected());
	}

	@Test
	public void testChargingCycle() throws InterruptedException {
		// 1. Connect EV
		this.simulator.connectEv();
		Thread.sleep(100);
		assertEquals(ChargingState.B1, this.simulator.getCurrentState());

		// 2. Set current via register write (simulating Modbus write from OpenEMS)
		// Duty cycle for 16A ≈ 0x00A6 (166, which is 16.6%)
		this.simulator.getRegisterMap().setIcmaxSetpoint(0x00A6);
		this.simulator.getStateMachine().onCurrentSetpointChanged(0x00A6);
		Thread.sleep(600); // Wait for B2 -> C2 transition

		// 3. Verify charging state
		assertEquals(ChargingState.C2, this.simulator.getCurrentState());

		// 4. Verify phase currents are set
		assertTrue(this.simulator.getPhaseCurrentL1() > 0);
		assertTrue(this.simulator.getPhaseCurrentL2() > 0);
		assertTrue(this.simulator.getPhaseCurrentL3() > 0);

		// 5. Stop charging
		this.simulator.getRegisterMap().setIcmaxSetpoint(0);
		this.simulator.getStateMachine().onCurrentSetpointChanged(0);
		Thread.sleep(100);

		// 6. Verify stopped
		assertEquals(ChargingState.B2, this.simulator.getCurrentState());
		assertEquals(0, this.simulator.getPhaseCurrentL1());
	}

	@Test
	public void testErrorInjection() throws InterruptedException {
		// Connect EV and start charging
		this.simulator.connectEv();
		this.simulator.getRegisterMap().setIcmaxSetpoint(0x00A6);
		this.simulator.getStateMachine().onCurrentSetpointChanged(0x00A6);
		Thread.sleep(600);

		// Inject overcurrent error
		this.simulator.injectError(ChargingState.F9, null);
		Thread.sleep(100);

		// Verify error state
		assertEquals(ChargingState.F9, this.simulator.getCurrentState());

		// Verify currents are reset
		assertEquals(0, this.simulator.getPhaseCurrentL1());

		// Clear error
		this.simulator.clearError();
		Thread.sleep(100);

		// Verify recovery to E2
		assertEquals(ChargingState.E2, this.simulator.getCurrentState());
	}

	@Test
	public void testRegisterReadWrite() {
		// Test reading device info
		int deviceId = this.simulator.getRegisterMap().getDeviceId();
		assertEquals(1, deviceId);

		String fwVersion = this.simulator.getRegisterMap().getFirmwareVersion();
		assertEquals("1.2", fwVersion);

		// Test writing and reading setpoint
		this.simulator.getRegisterMap().setIcmaxSetpoint(0x0100);
		assertEquals(0x0100, this.simulator.getRegisterMap().getIcmaxSetpoint());
	}

	/**
	 * NOTE: To test with real OpenEMS component, you would:
	 *
	 * <pre>
	 * 1. Start the Modbus simulator (as shown in setUp())
	 * 2. Create and activate an EvseChargePointAblImpl instance
	 * 3. Configure it to connect to localhost:TEST_PORT
	 * 4. Use the component's apply() method to control charging
	 * 5. Verify the component's channels reflect the simulator state
	 * </pre>
	 *
	 * Example:
	 *
	 * <pre>
	 * // Create component (requires full OpenEMS environment)
	 * Config config = ... // configure with modbus_id pointing to test bridge
	 * EvseChargePointAblImpl component = new EvseChargePointAblImpl();
	 * component.activate(context, config);
	 *
	 * // Simulate EV via simulator
	 * simulator.connectEv();
	 *
	 * // Control via component
	 * ChargePointAbilities abilities = component.getChargePointAbilities();
	 * ChargePointActions actions = ChargePointActions.create(abilities)
	 *     .setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000))
	 *     .build();
	 * component.apply(actions);
	 *
	 * // Verify
	 * assertEquals(ChargingState.C2, simulator.getCurrentState());
	 * </pre>
	 */
}
