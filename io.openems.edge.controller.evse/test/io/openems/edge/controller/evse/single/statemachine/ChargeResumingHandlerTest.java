package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TestUtils;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplySetPoint;

class ChargeResumingHandlerTest {

	@Test
	void testChargeResuming() {
		final var clock = TestUtils.createDummyClock();
		final var sut = generateSingleSut(clock, 0, FunctionUtils::doNothing);
		final var ctrl = sut.ctrlSingle();
		final var abilities = Profile.ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
				.setIsEvConnected(true) //
				.setIsReadyForCharging(true) //
				.build();
		final var chargingActions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(6000) //
				.build();
		final var pausedActions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(0) //
				.build();

		sut.chargePoint().withActivePower(4123);
		ctrl.apply(Mode.FORCE, chargingActions);
		clock.leap(16, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		ctrl.apply(Mode.FORCE, pausedActions);

		sut.chargePoint().withActivePower(0);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.CHARGE_RESUMING, ctrl.getParams().state());

		clock.leap(30, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.CHARGE_RESUMING, ctrl.getParams().state());

		sut.chargePoint().withActivePower(4123);
		clock.leap(1, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.CHARGING, ctrl.getParams().state());
	}

	@Test
	void testChargeResumingTimeout() {
		final var clock = TestUtils.createDummyClock();
		final var sut = generateSingleSut(clock, 0, FunctionUtils::doNothing);
		final var ctrl = sut.ctrlSingle();
		final var abilities = Profile.ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
				.setIsEvConnected(true) //
				.setIsReadyForCharging(true) //
				.build();
		final var chargingActions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(6000) //
				.build();
		final var pausedActions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(0) //
				.build();

		sut.chargePoint().withActivePower(4123);
		ctrl.apply(Mode.FORCE, chargingActions);
		clock.leap(16, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		ctrl.apply(Mode.FORCE, pausedActions);

		sut.chargePoint().withActivePower(0);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.CHARGE_RESUMING, ctrl.getParams().state());

		clock.leap(60, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.CHARGE_RESUMING, ctrl.getParams().state());

		clock.leap(61, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, chargingActions);
		assertEquals(EvseSingleState.FINISHED_EV_STOP, ctrl.getParams().state());
	}
}
