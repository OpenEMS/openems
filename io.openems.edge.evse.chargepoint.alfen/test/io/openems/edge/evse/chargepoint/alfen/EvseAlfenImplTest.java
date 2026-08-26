package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseAlfenImplTest {

	private static enum AccessMode {
		READ_ONLY, READ_WRITE
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseAlfen} and realistic Modbus
	 * register values.
	 *
	 * @param sut        the system under test
	 * @param accessMode the {@link AccessMode}
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareAlfenTest(EvseAlfenImpl sut, AccessMode accessMode) throws Exception {
		return prepareAlfenTest(sut, accessMode, THREE_PHASE);
	}

	/**
	 * Prepares a {@link ComponentTest} with {@link EvseAlfen} and realistic Modbus
	 * register values.
	 *
	 * @param sut        the system under test
	 * @param accessMode the {@link AccessMode}
	 * @param wiring     the configured {@link SingleOrThreePhase} wiring
	 * @return the prepared ComponentTest
	 * @throws Exception on error
	 */
	private static ComponentTest prepareAlfenTest(EvseAlfenImpl sut, AccessMode accessMode, SingleOrThreePhase wiring)
			throws Exception {
		return new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Meter data block (starting at 300)
						.withRegisters(300, new int[] { 0x0001 }) // METER_STATE
						// METER_LAST_VALUE_TIMESTAMP (1000ms)
						.withRegisters(301, new int[] { 0x0000, 0x0000, 0x0000, 0x03E8 })
						.withRegisters(305, new int[] { 0x0002 }) // METER_TYPE
						.withRegistersFloat32(306, 230.5f /* V */) // VOLTAGE_L1_RAW
						.withRegistersFloat32(308, 231.2f /* V */) // VOLTAGE_L2_RAW
						.withRegistersFloat32(310, 229.8f /* V */) // VOLTAGE_L3_RAW
						.withRegistersFloat32(312, 399.5f /* V */) // VOLTAGE_L1_L2
						.withRegistersFloat32(314, 400.1f /* V */) // VOLTAGE_L2_L3
						.withRegistersFloat32(316, 398.8f /* V */) // VOLTAGE_L3_L1
						.withRegistersFloat32(318, 0.5f /* A */) // CURRENT_N
						.withRegistersFloat32(320, 10.5f /* A (will be scaled to mA) */) // CURRENT_L1_RAW
						.withRegistersFloat32(322, 10.3f) // CURRENT_L2_RAW
						.withRegistersFloat32(324, 10.7f) // CURRENT_L3_RAW
						.withRegistersFloat32(326, 31.5f) // CURRENT_SUM
						.withRegistersFloat32(328, 0.98f) // POWER_FACTOR_L1
						.withRegistersFloat32(330, 0.97f) // POWER_FACTOR_L2
						.withRegistersFloat32(332, 0.99f) // POWER_FACTOR_L3
						.withRegistersFloat32(334, 0.98f) // POWER_FACTOR_SUM
						.withRegistersFloat32(336, 50.01f /* Hz */) // Frequency
						.withRegistersFloat32(338, 2300.0f /* W */) // CHARGE_POWER_L1
						.withRegistersFloat32(340, 2280.0f /* W */) // CHARGE_POWER_L2
						.withRegistersFloat32(342, 2320.0f /* W */) // CHARGE_POWER_L3
						.withRegistersFloat32(344, 6900.0f /* W */) // CHARGE_POWER (total)
						.withRegistersFloat32(346, 2350.0f) // APPARENT_POWER_L1
						.withRegistersFloat32(348, 2330.0f) // APPARENT_POWER_L2
						.withRegistersFloat32(350, 2370.0f) // APPARENT_POWER_L3
						.withRegistersFloat32(352, 7041.0f /* VA */) // APPARENT_POWER_SUM
						.withRegistersFloat32(354, 170.0f) // REACTIVE_POWER_L1
						.withRegistersFloat32(356, 160.0f) // REACTIVE_POWER_L2
						.withRegistersFloat32(358, 170.0f) // REACTIVE_POWER_L3
						.withRegistersFloat32(360, 500.0f /* var */) // REACTIVE_POWER_SUM
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
						.withRegistersFloat32(1206, 16.0f /* A */) // ACTUAL_APPLIED_MAX_CURRENT
						.withRegisters(1208, new int[] { 0x0000, 0x012C }) // MODBUS_SLAVE_MAX_CURRENT_VALID_TIME (300s)
						.withRegistersFloat32(1210, 16.0f /* A */) // SET_CURRENT
						.withRegistersFloat32(1212, 6.0f /* A */) // ACTIVE_LOAD_BALANCING_SAFE_CURRENT
						// MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR (true)
						// SET_PHASES (3 phases)
						.withRegisters(1214, new int[] { 0x0001 }) //
						.withRegisters(1215, new int[] { 0x0003 })) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setPhaseRotation(L1_L2_L3) //
						.setWiring(wiring) //
						.setReadOnly(switch (accessMode) {
						case READ_ONLY -> false;
						case READ_WRITE -> true;
						}) //
						.setMinCurrent(6000) //
						.setMaxCurrent(32000) //
						.build());
	}

	@Test
	public void testReadMeterValues() throws Exception {
		prepareAlfenTest(new EvseAlfenImpl(), AccessMode.READ_ONLY) //
				.next(new TestCase("Run several cycles to let Modbus communication settle"), 20) //

				.next(new TestCase("Verify meter values are read correctly") //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(EvseAlfen.ChannelId.METER_STATE, 1) //
						.output(EvseAlfen.ChannelId.METER_TYPE, 2) //
						.output(EvseAlfen.ChannelId.AVAILABILITY, true))

				.next(new TestCase(
						"Verify voltage values (raw floats converted to mV in listeners): 230.5V -> 230500 mV") //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231200) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229800))

				.next(new TestCase("Verify current values (raw floats in A, scaled by 1000 to mA): 10.5A -> 10500 mA") //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10500) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 10300) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 10700)) //

				.next(new TestCase("Verify power values") //
						.output(EvseAlfen.ChannelId.CHARGE_POWER, 6900.0f) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 6900))

				.deactivate();
	}

	@Test
	public void testMode3StateAndReadyForCharging() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		test //
				.next(new TestCase(), 20) //
				.next(new TestCase("Initial state: 'C2' = Charging") //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true))

				.next(new TestCase("Change to 'A' = Not connected") //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(1201, new int[] { 0x4100, 0x0000, 0x0000, 0x0000, 0x0000 }) // "A"
						)) //
				.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false))

				.next(new TestCase("Change to 'B1' = Connected, not ready") //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(1201, new int[] { 0x4231, 0x0000, 0x0000, 0x0000, 0x0000 }) // "B1"
						))

				.next(new TestCase(), 10) //
				.next(new TestCase() //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true))

				.deactivate();
	}

	@Test
	public void testWriteSetCurrent() throws Exception {
		prepareAlfenTest(new EvseAlfenImpl(), AccessMode.READ_ONLY) //
				.next(new TestCase(), 20) //

				.next(new TestCase("Write a new current setpoint") //
						.input(EvseAlfen.ChannelId.SET_CURRENT, 12.5f))

				.next(new TestCase(), 5)

				.next(new TestCase("Verify the debug channel shows the written value") //
						.output(EvseAlfen.ChannelId.DEBUG_SET_CURRENT, 12.5f))

				.deactivate();
	}

	@Test
	public void testWriteSetPhases() throws Exception {
		prepareAlfenTest(new EvseAlfenImpl(), AccessMode.READ_ONLY) //
				.next(new TestCase(), 20)

				.next(new TestCase("Write phase setting to 1 phase") //
						.input(EvseAlfen.ChannelId.SET_PHASES, 1))

				.next(new TestCase(), 5)

				.next(new TestCase("Verify the debug channel shows the written value") //
						.output(EvseAlfen.ChannelId.DEBUG_SET_PHASES, 1))

				.deactivate();
	}

	@Test
	public void testReadOnlyMode() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_WRITE) //
				.next(new TestCase(), 20);

		// Verify component is in read-only mode
		assertTrue(sut.isReadOnly(), "Component should be in read-only mode");

		// Read operations should still work
		test.next(new TestCase() //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
				.output(EvseAlfen.ChannelId.METER_STATE, 1));

		// ChargePointAbilities should have empty setpoint ability (min=0, max=0)
		var abilities = sut.getChargePointAbilities();
		assertEquals(0, abilities.applySetPoint().max(), "Read-only mode should have zero max current");

		test.deactivate();
	}

	@Test
	public void testPhaseDetection() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY);
		final var bridge = (DummyModbusBridge) sut.getBridgeModbus();

		// Initial state: all phases have current > 400mA -> THREE_PHASE
		test.next(new TestCase(), 20);

		var abilities = sut.getChargePointAbilities();
		assertTrue(abilities.applySetPoint().max() > 0, "Should detect three-phase charging with valid max current");

		test.next(new TestCase("Change to single-phase (only L1 has current)") //
				.onBeforeProcessImage(() -> bridge //
						.withRegistersFloat32(320, 10.5f) // CURRENT_L1_RAW: 10.5A
						.withRegistersFloat32(322, 0.1f) // CURRENT_L2_RAW: 0.1A (below threshold)
						.withRegistersFloat32(324, 0.1f) // CURRENT_L3_RAW: 0.1A (below threshold)
				)) //
				.next(new TestCase(), 15);

		// The phase detection should now show single-phase
		abilities = sut.getChargePointAbilities();
		assertTrue(abilities.applySetPoint().max() > 0, "Should still have setpoint ability with valid max current");

		test.deactivate();
	}

	@Test
	public void testPhaseSwitchAbility() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY);
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
				.withRegistersFloat32(322, 0.1f) //
				.withRegistersFloat32(324, 0.1f)));
		test.next(new TestCase(), 10);
		abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());

		test.deactivate();
	}

	@Test
	public void testSinglePhaseWiringHasNoPhaseSwitch() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY, SINGLE_PHASE) //
				.next(new TestCase(), 20);

		// Register 1215 reads "3", but the charge point is wired single-phased
		var abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
		assertNull(abilities.phaseSwitch().direction(), "Single-phase wiring must not offer phase switching");

		test.deactivate();
	}

	@Test
	public void testEnergyValues() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY) //
				.next(new TestCase(), 20);

		// Verify energy values are mapped correctly
		// The ENERGY_DELIVERED_SUM should be mapped to ACTIVE_PRODUCTION_ENERGY
		// Check the float value is approximately 60000 Wh
		var energyChannel = sut.channel(EvseAlfen.ChannelId.ENERGY_DELIVERED_SUM);
		var energyObj = energyChannel.value().get();
		float energyValue = energyObj != null ? (Float) energyObj : 0.0f;
		assertTrue(energyValue > 59000.0f && energyValue < 61000.0f, "Energy should be approximately 60000 Wh");

		test.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_ONLY) //
				.next(new TestCase(), 20);

		// Verify debug log output format
		var debugLog = sut.debugLog();
		assertTrue(debugLog.contains("L:"), "Debug log should contain power info");
		assertTrue(debugLog.contains("SetCurrent:"), "Debug log should contain SetCurrent info");

		test.deactivate();
	}

	@Test
	public void testDebugLogReadOnly() throws Exception {
		final var sut = new EvseAlfenImpl();
		final var test = prepareAlfenTest(sut, AccessMode.READ_WRITE) //
				.next(new TestCase(), 20);

		// In read-only mode, debug log should not contain SetCurrent
		var debugLog = sut.debugLog();
		assertTrue(debugLog.contains("L:"), "Debug log should contain power info");
		assertFalse(debugLog.contains("SetCurrent:"), "Debug log should NOT contain SetCurrent in read-only mode");

		test.deactivate();
	}
}
