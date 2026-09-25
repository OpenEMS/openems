package io.openems.edge.evse.chargepoint.voltie;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
import static io.openems.edge.evse.chargepoint.voltie.enums.LogVerbosity.NONE;
import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.ghgande.j2mod.modbus.ModbusSlaveException;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.api.task.Task.ExecuteState;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.voltie.enums.ChargeStopReason;
import io.openems.edge.evse.chargepoint.voltie.enums.EvseState;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseChargePointVoltieImplTest {

	/** Modbus exception 0x03 (illegal data value), the charger's rejection. */
	private static final ExecuteState REJECTED = new ExecuteState.Error(new ModbusSlaveException(3));

	private static ComponentTest prepareTest(EvseChargePointVoltieImpl sut, DummyModbusBridge bridge, boolean readOnly,
			TimeLeapClock clock) throws Exception {
		return new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(11) //
						.setReadOnly(readOnly) //
						.setWiring(THREE_PHASE) //
						.setPhaseRotation(L1_L2_L3) //
						.setLogVerbosity(NONE) //
						.build());
	}

	private static ComponentTest prepareTest(EvseChargePointVoltieImpl sut, DummyModbusBridge bridge, boolean readOnly)
			throws Exception {
		return prepareTest(sut, bridge, readOnly, createDummyClock());
	}

	// Default configuration: managed control on a charger whose capability bitmask
	// reports phase-switching support
	private static ComponentTest prepareTest(EvseChargePointVoltieImpl sut) throws Exception {
		return prepareTest(sut, createDummyModbusBridge(), false);
	}

	private static DummyModbusBridge createDummyModbusBridge() {
		return new DummyModbusBridge("modbus0") //
				// Status/config block 0x0000..0x001A (27 registers)
				.withRegisters(0x0000, new int[] { //
						1234, // 0x0000 CHARGER_ID
						370, // 0x0001 FIRMWARE_BUILD
						// 0x0002..0x0005 MCU_SERIAL, LSW first: 0x0001_0002_0003_0004
						0x0004, 0x0003, 0x0002, 0x0001, //
						// 0x0006..0x0009 POWER_BOARD_SERIAL, LSW first: 4242
						4242, 0x0000, 0x0000, 0x0000, //
						0x0003, // 0x000A EVSE_STATE: C, charging
						0, // 0x000B AUTOSTART_ENABLED
						1, // 0x000C CHARGING_ENABLED
						1, // 0x000D CHARGING_ACTIVE
						3, // 0x000E MAINS_PHASES
						0, // 0x000F STORED_DLM_MODE
						0, 0, // 0x0010, 0x0011 reserved
						0, // 0x0012 CHARGE_STOP_REASON
						32000, // 0x0013 AUTONOMOUS_CURRENT_LIMIT [mA]
						16000, // 0x0014 CURRENT_LIMIT [mA]
						0, // 0x0015 EFFECTIVE_DLM_MODE
						0, // 0x0016 FORCED_SINGLE_PHASE
						20, // 0x0017 COMM_WATCHDOG_TIMEOUT [s]
						32000, // 0x0018 HARDWARE_CURRENT_LIMIT [mA]
						1, // 0x0019 CAPABILITY_FLAGS: phase switching supported
						3 // 0x001A PHASES_IN_USE
				}) //
				// Meter block 0x2000..0x201D; INT32 MSW first
				.withRegisters(0x2000, new int[] { //
						0x0003, 0x8270, // 0x2000 VOLTAGE_L1: 230_000 mV
						0x0003, 0x8658, // 0x2002 VOLTAGE_L2: 231_000 mV
						0x0003, 0x7E88, // 0x2004 VOLTAGE_L3: 229_000 mV
						0x0000, 0x1770, // 0x2006 CURRENT_L1: 6_000 mA
						0x0000, 0x1B58, // 0x2008 CURRENT_L2: 7_000 mA
						0x0000, 0x1F40, // 0x200A CURRENT_L3: 8_000 mA
						0x0000, 0x0258, // 0x200C CHARGE_DURATION: 600 s
						0x0036, 0xEE80, // 0x200E ENERGY_SESSION: 3_600_000 Ws = 1_000 Wh
						0x0000, 0x12DE, // 0x2010 ACTIVE_POWER: 4_830 W
						0x0000, 0x3E80, // 0x2012 AVAILABLE_CURRENT_CAPACITY: 16_000 mA
						// 0x2014, 0x2015 board temperatures: read as part of the block, but
						// not exposed as Channels (not part of the public Modbus API)
						45, 55,
						0x0001, 0xE240, // 0x2016 lifetime energy: 123_456 Wh
						0x0000, 0x0564, // 0x2018 ACTIVE_POWER_L1: 1_380 W
						0x0000, 0x064A, // 0x201A ACTIVE_POWER_L2: 1_610 W
						0x0000, 0x0730 // 0x201C ACTIVE_POWER_L3: 1_840 W
				});
	}

	// A charger whose capability bitmask (0x0019) reports no optional feature
	private static DummyModbusBridge createDummyModbusBridgeWithoutCapabilities() {
		return createDummyModbusBridge() //
				.withRegisters(0x0019, new int[] { 0 });
	}

	@Test
	public void test() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut) //
				.next(new TestCase(), 8) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.CHARGER_ID, 1234) //
						.output(EvseChargePointVoltie.ChannelId.FIRMWARE_BUILD, 370) //
						.output(EvseChargePointVoltie.ChannelId.MCU_SERIAL, 0x0001_0002_0003_0004L) //
						.output(EvseChargePointVoltie.ChannelId.POWER_BOARD_SERIAL, 4242L) //
						.output(EvseChargePointVoltie.ChannelId.EVSE_STATE, EvseState.STATE_C) //
						.output(EvseChargePointVoltie.ChannelId.AUTOSTART_ENABLED, false) //
						.output(EvseChargePointVoltie.ChannelId.CHARGING_ENABLED, true) //
						.output(EvseChargePointVoltie.ChannelId.CHARGING_ACTIVE, true) //
						.output(EvseChargePointVoltie.ChannelId.MAINS_PHASES, 3) //
						.output(EvseChargePointVoltie.ChannelId.CHARGE_STOP_REASON, ChargeStopReason.NONE) //
						.output(EvseChargePointVoltie.ChannelId.AUTONOMOUS_CURRENT_LIMIT, 32000) //
						.output(EvseChargePointVoltie.ChannelId.CURRENT_LIMIT, 16000) //
						.output(EvseChargePointVoltie.ChannelId.FORCED_SINGLE_PHASE, false) //
						.output(EvseChargePointVoltie.ChannelId.COMM_WATCHDOG_TIMEOUT, 20) //
						.output(EvseChargePointVoltie.ChannelId.HARDWARE_CURRENT_LIMIT, 32000) //
						.output(EvseChargePointVoltie.ChannelId.CAPABILITY_FLAGS, 1) //
						.output(EvseChargePointVoltie.ChannelId.PHASES_IN_USE, 3) //

						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 7_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 8_000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 21_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4_830) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1_380) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1_610) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1_840) //

						.output(EvseChargePointVoltie.ChannelId.CHARGE_DURATION, 600) //
						.output(EvseChargePointVoltie.ChannelId.ENERGY_SESSION, 1_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 123_456L) //
						.output(EvseChargePointVoltie.ChannelId.AVAILABLE_CURRENT_CAPACITY, 16_000) //

						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
						.output(EvseChargePointVoltie.ChannelId.EVSE_FAULT, false) //
						.output(EvseChargePointVoltie.ChannelId.FIRMWARE_OUTDATED, false) //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, false)) //
				.deactivate();

		assertEquals("L:4830 W|SetCurrent:UNDEFINED|SetEnable:UNDEFINED", sut.debugLog());
	}

	/**
	 * The charger forwards Modbus requests only in the gap between its own ~500 ms
	 * internal polls, so a second request in the same Cycle waits ~470 ms - right
	 * at the bridge's hardcoded 500 ms timeout. Both read blocks must therefore be
	 * Priority.LOW, so the bridge executes exactly one of them per Cycle.
	 */
	@Test
	public void testNoHighPriorityReadTasks() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut) //
				.next(new TestCase(), 8) //
				.deactivate();

		ModbusProtocol protocol = getValueViaReflection(sut, "protocol");
		TasksManager<Task> taskManager = getValueViaReflection(protocol, "taskManager");
		// NOTE: WriteTasks always report Priority.HIGH, but they are executed only
		// when a write value has been set - so only ReadTasks are relevant here.
		assertTrue(taskManager.getTasks(Priority.HIGH).stream() //
				.noneMatch(ReadTask.class::isInstance));
		// status block + meter block
		assertEquals(2, taskManager.getTasks(Priority.LOW).stream() //
				.filter(ReadTask.class::isInstance).count());
	}

	@Test
	public void testChargePointAbilities() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut) //
				.next(new TestCase(), 8) //
				.deactivate();

		var abilities = sut.getChargePointAbilities();
		assertTrue(abilities.isEvConnected());
		assertTrue(abilities.isReadyForCharging());
		assertTrue(abilities.applySetPoint() instanceof ApplySetPoint.Ability.Ampere);
		assertEquals(6, abilities.applySetPoint().min());
		assertEquals(32, abilities.applySetPoint().max());
		assertEquals(THREE_PHASE, abilities.applySetPoint().phase());
		assertTrue(abilities.phaseSwitch().ability() instanceof ApplyPhaseSwitch.PhaseSwitchAbility.Manual);
		assertEquals(TO_SINGLE_PHASE, abilities.phaseSwitch().direction());
	}

	@Test
	public void testApplySetPoint() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var test = prepareTest(sut) //
				.next(new TestCase(), 8);

		// 8 A is written as 8000 mA; charging is already enabled, so no enable-write
		var actions = ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(8) //
				.build();

		test //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CURRENT_LIMIT, 8000) //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED, null)) //
				.deactivate();
	}

	/**
	 * Modbus API v1.3: writing 1 to register 0x000C only permits charging, and a
	 * client that repeats the write until the read-back matches can loop forever.
	 * The value is written once per intended state change instead.
	 */
	@Test
	public void testChargingEnableIsWrittenOnce() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge();
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		// The same value is never rewritten, no matter how often it is requested; a
		// changed value is written immediately
		assertTrue(sut.writeChargingEnabled(false));
		assertFalse(sut.writeChargingEnabled(false));
		assertFalse(sut.writeChargingEnabled(false));
		assertTrue(sut.writeChargingEnabled(true));
		assertFalse(sut.writeChargingEnabled(true));

		// The charger does not follow the write, so the read-back stays 0 while the
		// vehicle stays connected. The value must still not be repeated.
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> bridge //
						.withRegisters(0x000A, new int[] { 0x0002 }) // state B, connected
						.withRegisters(0x000C, new int[] { 0 }))) //
				.next(new TestCase(), 4) //
				.deactivate();
		sut.evaluateChargingEnableLatch();
		assertFalse(sut.writeChargingEnabled(true));
	}

	/**
	 * A write that answered a Modbus exception never reached the charger, so it
	 * has to be repeated - but not every Cycle: a charger with control-by-Modbus
	 * disabled rejects every write.
	 */
	@Test
	public void testChargingEnableIsRepeatedAfterARejectedWrite() throws Exception {
		final var clock = createDummyClock();
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut, createDummyModbusBridge(), false, clock) //
				.next(new TestCase(), 8) //
				.deactivate();

		assertTrue(sut.writeChargingEnabled(false));
		assertFalse(sut.writeChargingEnabled(false));

		sut.onChargingEnabledWriteExecuted(REJECTED);

		// Not retried right away
		assertFalse(sut.writeChargingEnabled(false));

		// Retried once the write interval has passed
		clock.leap(6, ChronoUnit.SECONDS);
		assertTrue(sut.writeChargingEnabled(false));
	}

	/**
	 * With no vehicle connected the controller still requests the minimum
	 * set-point, but the firmware discards an enable written in that state.
	 * Writing it anyway would set the write-once latch, and the vehicle plugged
	 * in later would never be started. The enable is written once the vehicle is
	 * connected.
	 */
	@Test
	public void testChargingEnableIsWrittenWhenAVehicleIsPluggedIn() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge() //
				.withRegisters(0x000A, new int[] { 0x0001 }) // state A, not connected
				.withRegisters(0x000C, new int[] { 0 }) //
				.withRegisters(0x000D, new int[] { 0 });
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		var actions = ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(6) //
				.build();

		test //
				// No vehicle: the current limit is written, the enable is not
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CURRENT_LIMIT, 6000) //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED, null)) //

				// The vehicle is plugged in: the enable is written
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(0x000A, new int[] { 0x0002 }))) // state B, connected
				.next(new TestCase(), 4) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED, 1)) //
				.deactivate();
	}

	/**
	 * An enable the charger has not taken over yet is discarded when the vehicle
	 * is unplugged, so it is written again for the next vehicle.
	 */
	@Test
	public void testChargingEnableIsRepeatedAfterAnUnplugBeforeConfirmation() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge() //
				.withRegisters(0x000A, new int[] { 0x0002 }) // state B, connected
				.withRegisters(0x000C, new int[] { 0 });
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		assertTrue(sut.writeChargingEnabled(true));

		// Unplugged before the read-back confirmed the enable
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> bridge //
						.withRegisters(0x000A, new int[] { 0x0001 }))) // state A, not connected
				.next(new TestCase(), 4) //
				.deactivate();
		sut.evaluateChargingEnableLatch();
		assertTrue(sut.writeChargingEnabled(true));
	}

	/**
	 * The driver follows the charger instead of rewriting, but a charger that
	 * leaves an already confirmed state - a firmware restart resets register
	 * 0x000C to 0 - is a new situation and is written again.
	 */
	@Test
	public void testChargingEnableIsRepeatedWhenTheChargerLeavesTheState() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge();
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		// The charger reports charging disabled
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> bridge //
						.withRegisters(0x000C, new int[] { 0 }))) //
				.next(new TestCase(), 4);
		assertTrue(sut.writeChargingEnabled(true));
		assertFalse(sut.writeChargingEnabled(true));

		// The vehicle is connected and the charger follows: the read-back confirms
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> bridge //
						.withRegisters(0x000C, new int[] { 1 }))) //
				.next(new TestCase(), 4);
		sut.evaluateChargingEnableLatch();
		assertFalse(sut.writeChargingEnabled(true));

		// The charger leaves that state on its own; the value is written again
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> bridge //
						.withRegisters(0x000C, new int[] { 0 }))) //
				.next(new TestCase(), 4) //
				.deactivate();
		sut.evaluateChargingEnableLatch();
		assertTrue(sut.writeChargingEnabled(true));
	}

	@Test
	public void testPhaseSwitch() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge();
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		// The Evse.Controller.Single PhaseSwitchHandler sends a zero set-point
		// together with the phase-switch action
		var actions = ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(0) //
				.setPhaseSwitchManual(TO_SINGLE_PHASE) //
				.build();

		test //
				// While a charging session is still active: only the zero set-point is
				// applied (stop via 0x000C); the switch register must not be written
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED, 0) //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_FORCED_SINGLE_PHASE, null)) //

				// Charging stopped -> the next re-sent action writes the switch register.
				// NOTE: DummyModbusBridge does not execute FC6 writes, so the charger's
				// reaction is simulated by updating the registers directly.
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(0x000C, new int[] { 0 }) //
								.withRegisters(0x000D, new int[] { 0 }))) //
				.next(new TestCase(), 4) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_FORCED_SINGLE_PHASE, 1)) //

				// The charger accepted the write; the read-back flips the Ability phase
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(0x0016, new int[] { 1 }))) //
				.next(new TestCase(), 4) //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(actions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.FORCED_SINGLE_PHASE, true) //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, false));

		// The PhaseSwitchHandler detects completion via the flipped Ability phase and
		// offers the opposite direction next
		var abilities = sut.getChargePointAbilities();
		assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(TO_THREE_PHASE, abilities.phaseSwitch().direction());

		// START_CHARGE: the handler restarts charging with a regular set-point
		var startActions = ChargePointActions.from(abilities) //
				.setApplySetPointInAmpere(6) //
				.build();
		test //
				.next(new TestCase() //
						.onAfterProcessImage(() -> sut.apply(startActions))) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CHARGING_ENABLED, 1) //
						.output(EvseChargePointVoltie.ChannelId.DEBUG_SET_CURRENT_LIMIT, 6000)) //
				.deactivate();
	}

	@Test
	public void testPhaseSwitchRejectionCallback() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var test = prepareTest(sut) //
				.next(new TestCase(), 8);

		assertNotNull(sut.getChargePointAbilities().phaseSwitch());

		// A rejected FC6 write answers Modbus exception 0x03, which the bridge
		// reports to the onExecute callback as ExecuteState.Error
		sut.onPhaseSwitchWriteExecuted(REJECTED);

		assertNull(sut.getChargePointAbilities().phaseSwitch());
		test //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, true)) //
				.deactivate();
	}

	/**
	 * The bridge reports every failed write as ExecuteState.Error, including
	 * transport failures and the gateway's own exceptions. Those are transient and
	 * must not disable phase switching.
	 */
	@Test
	public void testPhaseSwitchIsNotDisabledByATransientFailure() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var test = prepareTest(sut) //
				.next(new TestCase(), 8);

		// Transport failure
		sut.onPhaseSwitchWriteExecuted(new ExecuteState.Error(new SocketTimeoutException("Read timed out")));
		// Gateway exception 0x0B: no response from the charger's control unit
		sut.onPhaseSwitchWriteExecuted(new ExecuteState.Error(new ModbusSlaveException(0x0B)));
		// Gateway exception 0x0A: control unit not ready
		sut.onPhaseSwitchWriteExecuted(new ExecuteState.Error(new ModbusSlaveException(0x0A)));

		assertNotNull(sut.getChargePointAbilities().phaseSwitch());
		test //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, false)) //
				.deactivate();
	}

	/**
	 * A rejection latches phase switching, but unplugging the vehicle ends the
	 * situation, so the next session may try again.
	 */
	@Test
	public void testPhaseSwitchIsOfferedAgainAfterAnUnplug() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge();
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		sut.onPhaseSwitchWriteExecuted(REJECTED);
		assertNull(sut.getChargePointAbilities().phaseSwitch());

		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(0x000A, new int[] { 0x0001 }))) // state A, unplugged
				.next(new TestCase(), 4) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, false)) //
				.deactivate();
		assertNotNull(sut.getChargePointAbilities().phaseSwitch());
	}

	@Test
	public void testPhaseSwitchRejectionFallback() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		final var bridge = createDummyModbusBridge();
		final var test = prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8);

		var actions = ChargePointActions.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(0) //
				.setPhaseSwitchManual(TO_SINGLE_PHASE) //
				.build();

		// Charging is already stopped
		test //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge //
								.withRegisters(0x000C, new int[] { 0 }) //
								.withRegisters(0x000D, new int[] { 0 }))) //
				.next(new TestCase(), 4);

		// Fallback path: no Modbus exception is observed (DummyModbusBridge cannot
		// emulate the exception response), but register 0x0016 never changes. After
		// the verification cycles are exhausted, phase switching is marked
		// unavailable.
		for (var i = 0; i < 35; i++) {
			sut.apply(actions);
		}
		test //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.PHASE_SWITCH_FAILED, true)) //
				.deactivate();

		// Phase switching is no longer offered
		assertNull(sut.getChargePointAbilities().phaseSwitch());
	}

	/**
	 * Modbus API v1.3 asks clients to read bit 0 of the capability bitmask
	 * (register 0x0019) once and to offer phase switching only when it is set,
	 * instead of probing with a 0x0016 write. On hardware without the bit that
	 * write is rejected with exception 0x03, which would strand the controller at
	 * 0 A for its full 600 s phase-switch timeout.
	 */
	@Test
	public void testPhaseSwitchingUnsupportedByHardware() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut, createDummyModbusBridgeWithoutCapabilities(), false) //
				.next(new TestCase(), 8) //
				.deactivate();

		// Everything else is available; only the phase-switch ability is missing
		var abilities = sut.getChargePointAbilities();
		assertEquals(0, sut.getCapabilityFlags().get().intValue());
		assertNull(abilities.phaseSwitch());
		assertEquals(THREE_PHASE, abilities.applySetPoint().phase());
		assertEquals(32, abilities.applySetPoint().max());
	}

	/**
	 * The counterpart: with the capability bit set the ability is offered without
	 * any configuration.
	 */
	@Test
	public void testPhaseSwitchingSupportedByHardware() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut) //
				.next(new TestCase(), 8) //
				.deactivate();

		assertEquals(EvseChargePointVoltie.CAPABILITY_PHASE_SWITCHING, sut.getCapabilityFlags().get().intValue());
		assertNotNull(sut.getChargePointAbilities().phaseSwitch());
	}

	/**
	 * PhasesInUse is reported by the firmware on register 0x001A and counts the
	 * phases under load, so it is not derived from the per-phase currents: here
	 * all three phases carry current while the register reports one loaded phase.
	 */
	@Test
	public void testPhasesInUseComesFromTheRegister() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut, createDummyModbusBridge() //
				.withRegisters(0x001A, new int[] { 1 }), false) //
				.next(new TestCase(), 8) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 7_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 8_000) //
						.output(EvseChargePointVoltie.ChannelId.PHASES_IN_USE, 1)) //
				.deactivate();
	}

	@Test
	public void testOutdatedFirmware() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		// Firmware build 352 serves a shorter status area (up to 0x0018): only the
		// probe registers are provided; the full blocks must never be read
		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(0x0000, new int[] { 1234, 352 });
		prepareTest(sut, bridge, false) //
				.next(new TestCase(), 8) //
				.next(new TestCase() //
						.output(EvseChargePointVoltie.ChannelId.CHARGER_ID, 1234) //
						.output(EvseChargePointVoltie.ChannelId.FIRMWARE_BUILD, 352) //
						.output(EvseChargePointVoltie.ChannelId.FIRMWARE_OUTDATED, true) //
						.output(EvseChargePointVoltie.ChannelId.EVSE_STATE, EvseState.UNDEFINED) // never read

						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false)) //
				.deactivate();

		// The status block was never read, so nothing is advertised at all: without
		// the hardware current limit a set-point ability would have to guess a
		// maximum, and a fallback would command a vehicle that is already charging
		// down to the minimum
		var abilities = sut.getChargePointAbilities();
		assertNull(abilities.phaseSwitch());
		assertEquals(0, abilities.applySetPoint().max());
	}

	@Test
	public void testReadOnly() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut, createDummyModbusBridge(), true) //
				.next(new TestCase(), 8) //
				.deactivate();

		var abilities = sut.getChargePointAbilities();
		assertNull(abilities.phaseSwitch());
		assertEquals(0, abilities.applySetPoint().max());
		assertEquals("L:4830 W", sut.debugLog());
	}

	/**
	 * The write tasks must be registered even in read-only mode. The Modbus
	 * protocol is built once and never rebuilt on a configuration update, so
	 * omitting them here would leave the charge-point unable to write after
	 * readOnly is switched off at runtime.
	 */
	@Test
	public void testReadOnlyStillRegistersWriteTasks() throws Exception {
		final var sut = new EvseChargePointVoltieImpl();
		prepareTest(sut, createDummyModbusBridge(), true) //
				.next(new TestCase(), 8) //
				.deactivate();

		ModbusProtocol protocol = getValueViaReflection(sut, "protocol");
		TasksManager<Task> taskManager = getValueViaReflection(protocol, "taskManager");
		// 0x000C charging enabled, 0x0014 current limit, 0x0016 forced single phase
		assertEquals(3, taskManager.getTasks().stream() //
				.filter(WriteTask.class::isInstance).count());
	}
}
