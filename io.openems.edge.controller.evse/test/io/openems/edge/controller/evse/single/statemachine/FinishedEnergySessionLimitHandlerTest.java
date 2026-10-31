package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TestUtils;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.LogVerbosity;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.MyConfig;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplySetPoint;

class FinishedEnergySessionLimitHandlerTest {

	@Test
	void testContinuesChargingAfterIncreasingReachedEnergySessionLimit() throws Exception {
		final var clock = TestUtils.createDummyClock();
		final var sut = generateSingleSut(clock, 0, c -> c //
				.setMode(Mode.FORCE) //
				.setManualEnergySessionLimit(1500));
		final var ctrl = sut.ctrlSingle();
		final var abilities = Profile.ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
				.setIsEvConnected(true) //
				.setIsReadyForCharging(true) //
				.build();
		final var actions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(6000) //
				.build();
		sut.chargePoint().withActivePower(4123);

		ctrl.apply(Mode.FORCE, actions);
		clock.leap(16, ChronoUnit.SECONDS);
		ctrl.apply(Mode.FORCE, actions);
		assertEquals(EvseSingleState.CHARGING, ctrl.getParams().state());

		sut.test().next(new AbstractComponentTest.TestCase() //
				.input(ctrl.id(), ControllerEvseSingle.ChannelId.SESSION_ENERGY, 1500));
		ctrl.apply(Mode.FORCE, actions);
		ctrl.apply(Mode.FORCE, actions);
		assertEquals(EvseSingleState.FINISHED_ENERGY_SESSION_LIMIT, ctrl.getParams().state());
		assertEquals(0, sut.chargePoint().getLastChargePointActions().applySetPoint().value());

		sut.test().modified(MyConfig.create() //
				.setId("ctrlEvseSingle0") //
				.setMode(Mode.FORCE) //
				.setChargePointId("chargePoint0") //
				.setElectricVehicleId("electricVehicle0") //
				.setPhaseSwitching(PhaseSwitching.DISABLE) //
				.setOneShot("") //
				.setJsCalendar("[]") //
				.setManualEnergySessionLimit(2000) //
				.setLogVerbosity(LogVerbosity.NONE) //
				.build());
		ctrl.apply(Mode.FORCE, actions);
		assertEquals(EvseSingleState.CHARGE_RESUMING, ctrl.getParams().state());
		ctrl.apply(Mode.FORCE, actions);
		assertNotEquals(0, sut.chargePoint().getLastChargePointActions().applySetPoint().value());

		sut.test().deactivate();
	}
}
