package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class StateMachineTest {

	@Test
	public void test() throws OpenemsNamedException {
		final var clock = createDummyClock();
		final var ability = ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 32)) //
				.build();
		final var actions = new AtomicReference<ChargePointActions>(null);
		final Consumer<ChargePointActions> callback = a -> actions.set(a);

		var sm = new StateMachine(State.UNDEFINED);
		assertEquals(State.UNDEFINED, sm.getCurrentState());
		assertEquals("Undefined", sm.debugLog());

		sm.run(new Context(null, clock, ChargePointActions.from(ability).setApplySetPointInAmpere(0).build(), null,
				new History(), callback, setPhaseSwitchFailed -> FunctionUtils.doNothing()));
		assertEquals(State.EV_NOT_CONNECTED, sm.getCurrentState());
		assertEquals("EvNotConnected", sm.debugLog());
	}
}
