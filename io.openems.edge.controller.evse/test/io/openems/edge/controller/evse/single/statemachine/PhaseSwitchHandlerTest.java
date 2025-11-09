package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
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
import io.openems.edge.controller.evse.single.ControllerEvseSingleImplTest;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class PhaseSwitchHandlerTest {

	@Test
	public void testToThreePhase() throws IllegalArgumentException, OpenemsNamedException {
		final var clock = createDummyClock();
		final var singleSut = ControllerEvseSingleImplTest.generateSingleSut(clock, 0,
				config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
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

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(singleSut.chargePoint().getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();
		ctrl.apply(actions);

		actions = ChargePointActions.from(singleSut.chargePoint().getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build();

		assertEquals("Mode:Minimum|EvNotConnected", ctrl.debugLog());
		ctrl.apply(actions);
		test.accept(null, null); // null because of Force-Next-State

		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase", ctrl.debugLog());
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-0s", ctrl.debugLog());
		ctrl.apply(actions);

		clock.leap(29, SECONDS);
		ctrl.apply(actions);
		test.accept(0, null);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-29s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(null);
		ctrl.apply(actions);
		test.accept(0, null);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-30s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(100); // 100 is considered charging
		ctrl.apply(actions);
		test.accept(0, null);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-31s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		chargePoint.withActivePower(99); // 99 is considered non-charging
		ctrl.apply(actions);
		test.accept(0, null);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateTrue-32s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE); // Apply Phase-Switch
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitch-DeadTime-1s", ctrl.debugLog());

		clock.leap(28, SECONDS);
		ctrl.apply(actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitch-DeadTime-29s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitch-PredicateFalse-30s", ctrl.debugLog());

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 32)) //
				.build());
		actions = ChargePointActions.from(singleSut.chargePoint().getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.build();

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		test.accept(0, PhaseSwitch.TO_THREE_PHASE);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-PhaseSwitch-PredicateTrue-31s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		test.accept(6, null); // Restart charging
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StartCharge-DeadTime-1s", ctrl.debugLog());

		clock.leap(29, SECONDS);
		ctrl.apply(actions);
		test.accept(6, null); // Restart charging
		assertEquals("Mode:Minimum|Charging", ctrl.debugLog());
	}

	@Test
	public void testTimeout() throws IllegalArgumentException, OpenemsNamedException {
		final var clock = createDummyClock();
		final var singleSut = ControllerEvseSingleImplTest.generateSingleSut(clock, 0,
				config -> config.setLogVerbosity(LogVerbosity.DEBUG_LOG));
		final var ctrl = singleSut.ctrlSingle();
		final BooleanSupplier phaseSwitchFailed = () -> (boolean) ctrl
				.channel(ControllerEvseSingle.ChannelId.PHASE_SWITCH_FAILED).getNextValue().get();

		singleSut.chargePoint().withChargePointAbilities(ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build());
		var actions = ChargePointActions.from(singleSut.chargePoint().getChargePointAbilities()) //
				.setApplySetPointInAmpere(25) //
				.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE) //
				.build();

		ctrl.apply(actions);
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-0s", ctrl.debugLog());

		clock.leap(29, SECONDS);
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-DeadTime-29s", ctrl.debugLog());

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-30s", ctrl.debugLog());

		clock.leap(569, SECONDS);
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|PhaseSwitchToThreePhase-StopCharge-PredicateFalse-599s", ctrl.debugLog());
		assertFalse(phaseSwitchFailed.getAsBoolean());

		clock.leap(1, SECONDS);
		ctrl.apply(actions);
		assertEquals("Mode:Minimum|Charging", ctrl.debugLog());
		assertTrue(phaseSwitchFailed.getAsBoolean());
	}
}
