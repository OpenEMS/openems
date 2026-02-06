package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.ControllerEvseSingleImpl;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class PhaseSwitchHandlerTest {

	@Test
	public void testToThreePhase() throws IllegalArgumentException, OpenemsNamedException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BiConsumer<Integer, PhaseSwitch> test = (setPoint, phaseSwitch) -> {
			var cpa = chargePoint.getLastChargePointActions();
			if (setPoint == null && phaseSwitch == null) {
				assertNull(cpa);
			} else {
				assertEquals(setPoint.intValue(), cpa.applySetPoint().value());
				assertEquals(phaseSwitch, cpa.phaseSwitch());
			}
		};

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();
		ctrl.apply(mode, actions);

		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build();

		ctrl.apply(mode, actions);
		test.accept(null, null); // null because of Force-Next-State

		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase");
		ctrl.apply(mode, actions);
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
		chargePoint.withActivePower(100); // 100 is considered charging
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-31s");

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(99); // 99 is considered non-charging
		ctrl.apply(mode, actions);
		test.accept(0, null);
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateTrue-32s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE); // Apply Phase-Switch
		assertDebugLog(ctrl, "Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitch-DeadTime-1s");

		clock.leap(28, SECONDS);
		chargePoint.withActivePower(0); // actually Zero
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitch-DeadTime-29s");

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitch-PredicateFalse-30s");

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 32)) //
				.build());
		actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();

		clock.leap(1, SECONDS);
		ctrl.apply(mode, actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertDebugLog(ctrl, "Mode:Zero|PhaseSwitchToThreePhase-PhaseSwitch-PredicateTrue-31s");

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
	public void testTimeout() throws IllegalArgumentException, OpenemsNamedException {
		final var clock = createDummyClock();
		final var singleSut = generateSingleSut(clock, 0, config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final var mode = ctrl.getParams().mode();
		final var chargePoint = singleSut.chargePoint();
		final BooleanSupplier phaseSwitchFailed = () -> (boolean) ctrl
				.channel(ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED).getNextValue().get();

		chargePoint.withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(chargePoint.getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build();

		chargePoint.withActivePower(1234);
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
		assertEquals(string, ctrl.debugLog());
	}
}
