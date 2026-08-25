package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseAlfenImplTest {

	private static final String COMPONENT_ID = "evseChargePoint0";
	private static final String MODBUS_ID = "modbus0";

	/**
	 * Convert a float value to two Modbus registers (IEEE 754 big-endian).
	 *
	 * @param value the float value
	 * @return int array with two register values
	 */
	private static int[] floatToRegisters(float value) {
		int bits = Float.floatToIntBits(value);
		return new int[] { (bits >> 16) & 0xFFFF, bits & 0xFFFF };
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseAlfen} and realistic Modbus
	 * register values.
	 *
	 * @param sut      the system under test
	 * @param readOnly whether read-only mode should be enabled
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareAlfenTest(EvseAlfenImpl sut, boolean readOnly) throws Exception {
		return prepareAlfenTest(sut, readOnly, THREE_PHASE);
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseAlfen} and realistic Modbus
	 * register values.
	 *
	 * @param sut      the system under test
	 * @param readOnly whether read-only mode should be enabled
	 * @param wiring   the configured {@link SingleOrThreePhase} wiring
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareAlfenTest(EvseAlfenImpl sut, boolean readOnly, SingleOrThreePhase wiring)
			throws Exception {
		// Create float register values for realistic test data
		final var voltageL1 = floatToRegisters(230.5f); // V
		final var voltageL2 = floatToRegisters(231.2f); // V
		final var voltageL3 = floatToRegisters(229.8f); // V
		final var currentN = floatToRegisters(0.5f); // A
		final var currentL1 = floatToRegisters(10.5f); // A (will be scaled to mA)
		final var currentL2 = floatToRegisters(10.3f); // A
		final var currentL3 = floatToRegisters(10.7f); // A
		final var powerFactorL1 = floatToRegisters(0.98f);
		final var powerFactorL2 = floatToRegisters(0.97f);
		final var powerFactorL3 = floatToRegisters(0.99f);
		final var powerFactorSum = floatToRegisters(0.98f);
		final var frequency = floatToRegisters(50.01f); // Hz
		final var chargePowerL1 = floatToRegisters(2300.0f); // W
		final var chargePowerL2 = floatToRegisters(2280.0f); // W
		final var chargePowerL3 = floatToRegisters(2320.0f); // W
		final var chargePowerTotal = floatToRegisters(6900.0f); // W
		final var apparentPowerSum = floatToRegisters(7041.0f); // VA
		final var reactivePowerSum = floatToRegisters(500.0f); // var
		final var actualAppliedMaxCurrent = floatToRegisters(16.0f); // A
		final var setCurrent = floatToRegisters(16.0f); // A
		final var activeLoadBalancingSafeCurrent = floatToRegisters(6.0f); // A

		return new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						// Meter data block (starting at 300)
						.withRegisters(300, new int[] { 0x0001 }) // METER_STATE
						// METER_LAST_VALUE_TIMESTAMP (1000ms)
						.withRegisters(301, new int[] { 0x0000, 0x0000, 0x0000, 0x03E8 })
						.withRegisters(305, new int[] { 0x0002 }) // METER_TYPE
						.withRegisters(306, voltageL1) // VOLTAGE_L1_RAW
						.withRegisters(308, voltageL2) // VOLTAGE_L2_RAW
						.withRegisters(310, voltageL3) // VOLTAGE_L3_RAW
						.withRegisters(312, floatToRegisters(399.5f)) // VOLTAGE_L1_L2
						.withRegisters(314, floatToRegisters(400.1f)) // VOLTAGE_L2_L3
						.withRegisters(316, floatToRegisters(398.8f)) // VOLTAGE_L3_L1
						.withRegisters(318, currentN) // CURRENT_N
						.withRegisters(320, currentL1) // CURRENT_L1_RAW
						.withRegisters(322, currentL2) // CURRENT_L2_RAW
						.withRegisters(324, currentL3) // CURRENT_L3_RAW
						.withRegisters(326, floatToRegisters(31.5f)) // CURRENT_SUM
						.withRegisters(328, powerFactorL1) // POWER_FACTOR_L1
						.withRegisters(330, powerFactorL2) // POWER_FACTOR_L2
						.withRegisters(332, powerFactorL3) // POWER_FACTOR_L3
						.withRegisters(334, powerFactorSum) // POWER_FACTOR_SUM
						.withRegisters(336, frequency) // Frequency
						.withRegisters(338, chargePowerL1) // CHARGE_POWER_L1
						.withRegisters(340, chargePowerL2) // CHARGE_POWER_L2
						.withRegisters(342, chargePowerL3) // CHARGE_POWER_L3
						.withRegisters(344, chargePowerTotal) // CHARGE_POWER (total)
						.withRegisters(346, floatToRegisters(2350.0f)) // APPARENT_POWER_L1
						.withRegisters(348, floatToRegisters(2330.0f)) // APPARENT_POWER_L2
						.withRegisters(350, floatToRegisters(2370.0f)) // APPARENT_POWER_L3
						.withRegisters(352, apparentPowerSum) // APPARENT_POWER_SUM
						.withRegisters(354, floatToRegisters(170.0f)) // REACTIVE_POWER_L1
						.withRegisters(356, floatToRegisters(160.0f)) // REACTIVE_POWER_L2
						.withRegisters(358, floatToRegisters(170.0f)) // REACTIVE_POWER_L3
						.withRegisters(360, reactivePowerSum) // REACTIVE_POWER_SUM
						// Energy registers (362-425) - FloatQuadrupleword (4 registers each)
						// ENERGY_DELIVERED_L1 (~20000 Wh)
						.withRegisters(362, new int[] { 0x40D3, 0x8800, 0x0000, 0x0000 })
						.withRegisters(366, new int[] { 0x40D3, 0x8800, 0x0000, 0x0000 }) // ENERGY_DELIVERED_L2
						.withRegisters(370, new int[] { 0x40D3, 0x8800, 0x0000, 0x0000 }) // ENERGY_DELIVERED_L3
						// ENERGY_DELIVERED_SUM (~60000 Wh)
						.withRegisters(374, new int[] { 0x40ED, 0x4C00, 0x0000, 0x0000 })
						.withRegisters(378, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // ENERGY_CONSUMED_L1
						.withRegisters(382, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // ENERGY_CONSUMED_L2
						.withRegisters(386, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // ENERGY_CONSUMED_L3
						.withRegisters(390, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // ENERGY_CONSUMED_SUM
						.withRegisters(394, new int[] { 0x40D4, 0x0000, 0x0000, 0x0000 }) // APPARENT_ENERGY_L1
						.withRegisters(398, new int[] { 0x40D4, 0x0000, 0x0000, 0x0000 }) // APPARENT_ENERGY_L2
						.withRegisters(402, new int[] { 0x40D4, 0x0000, 0x0000, 0x0000 }) // APPARENT_ENERGY_L3
						.withRegisters(406, new int[] { 0x40EE, 0x0000, 0x0000, 0x0000 }) // APPARENT_ENERGY_SUM
						.withRegisters(410, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // REACTIVE_ENERGY_L1
						.withRegisters(414, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // REACTIVE_ENERGY_L2
						.withRegisters(418, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // REACTIVE_ENERGY_L3
						.withRegisters(422, new int[] { 0x0000, 0x0000, 0x0000, 0x0000 }) // REACTIVE_ENERGY_SUM
						// Status block (starting at 1200)
						.withRegisters(1200, new int[] { 0x0001 }) // AVAILABILITY (true)
						.withRegisters(1201, new int[] { 0x4332, 0x0000, 0x0000, 0x0000, 0x0000 }) // MODE_3_STATE "C2"
						.withRegisters(1206, actualAppliedMaxCurrent) // ACTUAL_APPLIED_MAX_CURRENT
						.withRegisters(1208, new int[] { 0x0000, 0x012C }) // MODBUS_SLAVE_MAX_CURRENT_VALID_TIME (300s)
						.withRegisters(1210, setCurrent) // SET_CURRENT
						.withRegisters(1212, activeLoadBalancingSafeCurrent) // ACTIVE_LOAD_BALANCING_SAFE_CURRENT
						// MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR (true)
						// SET_PHASES (3 phases)
						.withRegisters(1214, new int[] { 0x0001 }).withRegisters(1215, new int[] { 0x0003 })) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID) //
						.setPhaseRotation(L1_L2_L3) //
						.setWiring(wiring) //
						.setReadOnly(readOnly) //
						.setMinCurrent(6000) //
						.setMaxCurrent(32000) //
						.build());
	}

	@Test
	public void testReadMeterValues() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);

		// Run several cycles to let Modbus communication settle
		test.next(new TestCase(), 20);

		// Verify meter values are read correctly
		test.next(new TestCase() //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				.output(EvseAlfen.ChannelId.METER_STATE, 1) //
				.output(EvseAlfen.ChannelId.METER_TYPE, 2) //
				.output(EvseAlfen.ChannelId.AVAILABILITY, true) //
		);

		// Verify voltage values (raw floats converted to mV in listeners)
		// 230.5V -> 230500 mV
		test.next(new TestCase() //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230500) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231200) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229800) //
		);

		// Verify current values (raw floats in A, scaled by 1000 to mA)
		// 10.5A -> 10500 mA
		test.next(new TestCase() //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 10300) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 10700) //
		);

		// Verify power values
		test.next(new TestCase() //
				.output(EvseAlfen.ChannelId.CHARGE_POWER, 6900.0f) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 6900) //
		);

		test.deactivate();
	}

	@Test
	public void testMode3StateAndReadyForCharging() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// Initial state: "C2" = Charging
		test.next(new TestCase(), 20);
		test.next(new TestCase() //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
		);

		// Change to "A" = Not connected
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(1201, new int[] { 0x4100, 0x0000, 0x0000, 0x0000, 0x0000 }) // "A"
		));
		test.next(new TestCase(), 10);
		test.next(new TestCase() //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false) //
		);

		// Change to "B1" = Connected, not ready
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(1201, new int[] { 0x4231, 0x0000, 0x0000, 0x0000, 0x0000 }) // "B1"
		));
		test.next(new TestCase(), 10);
		test.next(new TestCase() //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
		);

		test.deactivate();
	}

	@Test
	public void testWriteSetCurrent() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);

		test.next(new TestCase(), 20);

		// Write a new current setpoint
		test.next(new TestCase() //
				.input(EvseAlfen.ChannelId.SET_CURRENT, 12.5f) //
		);
		test.next(new TestCase(), 5);

		// Verify the debug channel shows the written value
		test.next(new TestCase() //
				.output(EvseAlfen.ChannelId.DEBUG_SET_CURRENT, 12.5f) //
		);

		test.deactivate();
	}

	@Test
	public void testWriteSetPhases() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);

		test.next(new TestCase(), 20);

		// Write phase setting to 1 phase
		test.next(new TestCase() //
				.input(EvseAlfen.ChannelId.SET_PHASES, 1) //
		);
		test.next(new TestCase(), 5);

		// Verify the debug channel shows the written value
		test.next(new TestCase() //
				.output(EvseAlfen.ChannelId.DEBUG_SET_PHASES, 1) //
		);

		test.deactivate();
	}

	@Test
	public void testReadOnlyMode() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, true);

		test.next(new TestCase(), 20);

		// Verify component is in read-only mode
		assertTrue("Component should be in read-only mode", sut.isReadOnly());

		// Read operations should still work
		test.next(new TestCase() //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				.output(EvseAlfen.ChannelId.METER_STATE, 1) //
		);

		// ChargePointAbilities should have empty setpoint ability (min=0, max=0)
		var abilities = sut.getChargePointAbilities();
		assertEquals("Read-only mode should have zero max current", 0, abilities.applySetPoint().max());

		test.deactivate();
	}

	@Test
	public void testPhaseDetection() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// Initial state: all phases have current > 400mA -> THREE_PHASE
		test.next(new TestCase(), 20);

		var abilities = sut.getChargePointAbilities();
		assertTrue("Should detect three-phase charging with valid max current", abilities.applySetPoint().max() > 0);

		// Change to single-phase (only L1 has current)
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(320, floatToRegisters(10.5f)) // CURRENT_L1_RAW: 10.5A
				.withRegisters(322, floatToRegisters(0.1f)) // CURRENT_L2_RAW: 0.1A (below threshold)
				.withRegisters(324, floatToRegisters(0.1f)) // CURRENT_L3_RAW: 0.1A (below threshold)
		));
		test.next(new TestCase(), 15);

		// The phase detection should now show single-phase
		abilities = sut.getChargePointAbilities();
		assertTrue("Should still have setpoint ability with valid max current", abilities.applySetPoint().max() > 0);

		test.deactivate();
	}

	@Test
	public void testPhaseSwitchAbility() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// Register 1215 reads "3" -> three-phase; possible switch is TO_SINGLE_PHASE
		test.next(new TestCase(), 20);
		var abilities = sut.getChargePointAbilities();
		assertEquals(THREE_PHASE, abilities.applySetPoint().phase());
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, abilities.phaseSwitch().direction());

		// Register 1215 reads "1" -> single-phase, even while no current flows
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(1215, new int[] { 0x0001 })));
		test.next(new TestCase(), 10);
		abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, abilities.phaseSwitch().direction());

		// Register 1215 is not available (filled with 0xFFFF) -> fall back to the
		// current activity heuristic; here only L1 carries current
		test.next(new TestCase().onBeforeProcessImage(() -> bridge //
				.withRegisters(1215, new int[] { 0xFFFF }) //
				.withRegisters(322, floatToRegisters(0.1f)) //
				.withRegisters(324, floatToRegisters(0.1f))));
		test.next(new TestCase(), 10);
		abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());

		test.deactivate();
	}

	@Test
	public void testSinglePhaseWiringHasNoPhaseSwitch() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false, SINGLE_PHASE);

		test.next(new TestCase(), 20);

		// Register 1215 reads "3", but the charge point is wired single-phased
		var abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
		assertNull("Single-phase wiring must not offer phase switching", abilities.phaseSwitch().direction());

		test.deactivate();
	}

	@Test
	public void testEnergyValues() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);

		test.next(new TestCase(), 20);

		// Verify energy values are mapped correctly
		// The ENERGY_DELIVERED_SUM should be mapped to ACTIVE_PRODUCTION_ENERGY
		// Check the float value is approximately 60000 Wh
		var energyChannel = sut.channel(EvseAlfen.ChannelId.ENERGY_DELIVERED_SUM);
		var energyObj = energyChannel.value().get();
		float energyValue = energyObj != null ? (Float) energyObj : 0.0f;
		assertTrue("Energy should be approximately 60000 Wh", energyValue > 59000.0f && energyValue < 61000.0f);

		test.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, false);

		test.next(new TestCase(), 20);

		// Verify debug log output format
		var debugLog = sut.debugLog();
		assertTrue("Debug log should contain power info", debugLog.contains("L:"));
		assertTrue("Debug log should contain SetCurrent info", debugLog.contains("SetCurrent:"));

		test.deactivate();
	}

	@Test
	public void testDebugLogReadOnly() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, true);

		test.next(new TestCase(), 20);

		// In read-only mode, debug log should not contain SetCurrent
		var debugLog = sut.debugLog();
		assertTrue("Debug log should contain power info", debugLog.contains("L:"));
		assertFalse("Debug log should NOT contain SetCurrent in read-only mode", debugLog.contains("SetCurrent:"));

		test.deactivate();
	}
}
