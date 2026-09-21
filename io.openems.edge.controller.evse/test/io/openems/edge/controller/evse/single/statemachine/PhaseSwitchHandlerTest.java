package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImpl;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchAbility;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;

class PhaseSwitchHandlerTest {

	@Test
	void testInternalToThreePhase() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BiConsumer<Integer, PhaseSwitchDirection> test = (setPoint, phaseSwitch) -> {
			var cpa = chargePoint.getLastChargePointActions();
			if (setPoint == null && phaseSwitch == null) {
				assertNull(cpa);
			} else {
				assertEquals(setPoint, Integer.valueOf(cpa.applySetPoint().value()));
				assertEquals(phaseSwitch, cpa.phaseSwitch() != null ? cpa.phaseSwitch().direction() : null);
			}
		};

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.build();
		ctrl.apply(mode, actions);

		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal())) //
				.build();
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		test.accept(6900, PhaseSwitchDirection.TO_THREE_PHASE);

		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase");
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);// extra needed because of new Entry state
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-0s");
		clock.leap(29, SECONDS);
		chargePoint.withActivePower(1380);
		ctrl.apply(mode, actions);
		test.accept(6900, PhaseSwitchDirection.TO_THREE_PHASE); // setpoint sent to chargepoint is 6900
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-29s");
		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(4140) //
				.build();
		ctrl.apply(mode, actions);
		test.accept(6900, PhaseSwitchDirection.TO_THREE_PHASE); // setpoint sent to chargepoint is 6900
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-29s");
	}

	@Test
	void testInternalToThreePhaseKeepsSwitchWithOneShotAction() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());

		var actionsWithSwitch = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal())) //
				.build();
		ctrl.apply(mode, actionsWithSwitch);
		ctrl.apply(mode, actionsWithSwitch);

		var actionsWithoutSwitch = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.build();
		ctrl.apply(mode, actionsWithoutSwitch);
		ctrl.apply(mode, actionsWithoutSwitch);

		clock.leap(29, SECONDS);
		chargePoint.withActivePower(1380);
		ctrl.apply(mode, actionsWithoutSwitch);

		var cpa = chargePoint.getLastChargePointActions();
		assertEquals(Integer.valueOf(6900), Integer.valueOf(cpa.applySetPoint().value()));
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE,
				cpa.phaseSwitch() != null ? cpa.phaseSwitch().direction() : null);

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_SINGLE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		var actionsAfterSwitch = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(4140) //
				.build();
		ctrl.apply(mode, actionsAfterSwitch);

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actionsAfterSwitch);
		assertDebugLog(ctrl, "Mode:Minimum|Charging");
	}

	@Test
	void testInternalToThreePhaseRequiresAbilityChangeAndActivePower() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal())) //
				.build();

		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-0s");
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);// extra needed because of new Entry state
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-0s");

		clock.leap(29, SECONDS);
		chargePoint.withActivePower(0);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-29s");

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_SINGLE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(4140) //
				.build();

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Zero|Charging");

		chargePoint.withActivePower(1380);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|EvNotConnected");
	}

	@Test
	void testInternalToSinglePhase() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BiConsumer<Integer, PhaseSwitchDirection> test = (setPoint, phaseSwitch) -> {
			var cpa = chargePoint.getLastChargePointActions();
			if (setPoint == null && phaseSwitch == null) {
				assertNull(cpa);
			} else {
				assertEquals(setPoint, Integer.valueOf(cpa.applySetPoint().value()));
				assertEquals(phaseSwitch, cpa.phaseSwitch() != null ? cpa.phaseSwitch().direction() : null);
			}
		};

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_SINGLE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(4140) //
				.build();
		ctrl.apply(mode, actions);

		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(4140) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_SINGLE_PHASE, //
						new PhaseSwitchAbility.Internal())) //
				.build();

		ctrl.apply(mode, actions);

		ctrl.apply(mode, actions);
		test.accept(2300, PhaseSwitchDirection.TO_SINGLE_PHASE); //

		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToSinglePhase");
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions); //
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToSinglePhase-PhaseSwitchInternal-DeadTime-0s");

		clock.leap(29, SECONDS);
		chargePoint.withActivePower(4140);
		ctrl.apply(mode, actions);
		test.accept(2300, PhaseSwitchDirection.TO_SINGLE_PHASE); //
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToSinglePhase-PhaseSwitchInternal-DeadTime-29s");

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(1380) //
				.build();
		ctrl.apply(mode, actions);
		test.accept(2300, null); //
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToSinglePhase-PhaseSwitchInternal-DeadTime-29s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|Charging");
	}

	@Test
	void testToThreePhase() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BiConsumer<Integer, PhaseSwitchDirection> test = (setPoint, phaseSwitch) -> {
			var cpa = chargePoint.getLastChargePointActions();
			if (setPoint == null && phaseSwitch == null) {
				assertNull(cpa);
			} else {
				assertEquals(setPoint, Integer.valueOf(cpa.applySetPoint().value()));
				assertEquals(phaseSwitch, cpa.phaseSwitch() != null ? cpa.phaseSwitch().direction() : null);
			}
		};

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();
		ctrl.apply(mode, actions);

		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE) //
				.build();

		ctrl.apply(mode, actions);
		test.accept(null, null); //

		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase");
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);//
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-0s");
		ctrl.apply(mode, actions);

		clock.leap(29, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-29s");

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(null);
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-30s");

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(100); //
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-31s");

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(99); //
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateTrue-32s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitchDirection.TO_THREE_PHASE); //
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchManual-DeadTime-1s");

		clock.leap(28, SECONDS);
		chargePoint.withActivePower(0); //
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitchDirection.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitchManual-DeadTime-29s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitchDirection.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitchManual-PredicateFalse-30s");

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 32)) //
				.build());
		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitchDirection.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitchManual-PredicateTrue-31s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(6, null); // Restart charging
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-StartCharge-DeadTime-1s");

		clock.leap(29, SECONDS);
		chargePoint.withActivePower(1); // Non-Zero
		ctrl.apply(mode, actions);
		test.accept(6, null); // Restart charging
		assertDebugLog(ctrl, "Mode:Minimum|Charging");
	}

	@Test
	void testInternalTimeout() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BooleanSupplier phaseSwitchFailed = () -> (boolean) ctrl
				.channel(ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED).getNextValue().get();

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal()))
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInWatt(2300) //
				.setPhaseSwitch(new ApplyPhaseSwitch(PhaseSwitchDirection.TO_THREE_PHASE, //
						new PhaseSwitchAbility.Internal())) //
				.build();

		chargePoint.withActivePower(1380);
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-0s");

		clock.leap(29, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-DeadTime-29s");
		assertFalse(phaseSwitchFailed.getAsBoolean());

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		// Phase has not switched yet -> PREDICATE_FALSE
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-PredicateFalse-30s");

		clock.leap(569, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitchInternal-PredicateFalse-599s");
		assertFalse(phaseSwitchFailed.getAsBoolean());

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|Charging");
		assertTrue(phaseSwitchFailed.getAsBoolean());
	}

	@Test
	void testTimeout() throws IllegalArgumentException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BooleanSupplier phaseSwitchFailed = () -> (boolean) ctrl
				.channel(ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED).getNextValue().get();

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE) //
				.build();

		chargePoint.withActivePower(1234);
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-0s");

		clock.leap(29, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-29s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-30s");

		clock.leap(569, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-599s");
		assertFalse(phaseSwitchFailed.getAsBoolean());

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		assertDebugLog(ctrl, "Mode:Minimum|Charging");
		assertTrue(phaseSwitchFailed.getAsBoolean());
	}

	private static void assertDebugLog(ControllerEvseSingleImpl ctrl, String string) {
		ctrl.channel(ControllerEvseSingle.ChannelId.ACTUAL_MODE).nextProcessImage();
		var actual = ctrl.debugLog();
		assertEquals(string, actual);
	}
}
