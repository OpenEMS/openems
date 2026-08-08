package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.TestUtils.generateSingleSut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplySetPoint;

class ControllerEvseSingleImplTest {

	@Test
	void test() throws Exception {
		var sut = generateSingleSut(c -> c //
				.setLogVerbosity(LogVerbosity.DEBUG_LOG) //
				.setJsCalendar("""
						[{
						  "@type": "Task",
						  "updated": "2020-01-01T00:00:00Z",
						  "start": "2024-06-17T00:00:00",
						  "recurrenceRules": [
						    {
						      "frequency": "weekly",
						      "byDay": [
						        "sa",
						        "su"
						      ]
						    }
						  ],
						  "openems.io:payload": {
						    "class": "Manual",
						    "mode": "FORCE"
						  }
						}]"""));

		sut.test() //
				.next(new TestCase()) //
				.deactivate();

		final var ctrl = sut.ctrlSingle();
		assertEquals("Mode:Zero|Undefined", ctrl.debugLog());

		var params = sut.ctrlSingle().getParams();
		assertEquals("ctrlEvseSingle0", params.ctrlSingleId());
		assertEquals(Mode.MINIMUM, params.mode());
		assertNull(params.activePower());
		assertEquals(0, params.sessionEnergy());
		assertEquals(10000, params.sessionEnergyLimit().intValue());
		assertEquals(0, params.history().streamAll().count());
		assertEquals(Hysteresis.INACTIVE, params.hysteresis());
		assertEquals(PhaseSwitching.DISABLE, params.phaseSwitching());
		assertFalse(params.appearsToBeFullyCharged());
	}

	@Test
	void testDoesNotApplyActionsWhenChargePointIsReadOnly() {
		final var sut = generateSingleSut(FunctionUtils::doNothing);
		final var abilities = Profile.ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
				.setIsEvConnected(false) //
				.build();
		final var actions = Profile.ChargePointActions.from(abilities) //
				.setApplySetPointInMilliAmpere(6000) //
				.build();

		sut.ctrlSingle().apply(Mode.ZERO, actions);
		sut.chargePoint().withIsReadOnly(true);
		sut.ctrlSingle().apply(Mode.FORCE, actions);

		assertEquals(Mode.FORCE.getValue(),
				sut.ctrlSingle().channel(ControllerEvseSingle.ChannelId.ACTUAL_MODE).getNextValue().get());
		assertEquals(State.EV_NOT_CONNECTED.getValue(),
				sut.ctrlSingle().channel(ControllerEvseSingle.ChannelId.STATE_MACHINE).getNextValue().get());

		assertNull(sut.chargePoint().getLastChargePointActions());
	}
}
