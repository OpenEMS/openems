package io.openems.edge.controller.ess.sohcycle.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl;
import io.openems.edge.controller.ess.sohcycle.MyConfig;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

class AbstractAllowedPowerHandlerTest {

	private final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2026-01-01T00:00:00Z"));
	private final DummyManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
	private final ControllerEssSohCycleImpl controller = new ControllerEssSohCycleImpl();

	@Test
	void stateExitAndReentryClearTimingWhileAllowingSubclassCleanup() throws Exception {
		var machine = new TestStateMachine();
		final var handler = machine.prepareHandler();
		TestUtils.withValue(this.ess, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 0);
		machine.run(this.context());

		this.clock.leap(299, ChronoUnit.SECONDS);
		machine.forceNextState(StateMachine.State.IDLE);
		machine.run(this.context());
		assertTrue(handler.cleanupCalled);

		this.clock.leap(1, ChronoUnit.SECONDS);
		machine.forceNextState(StateMachine.State.PREPARE);
		machine.run(this.context());
		machine.run(this.context());
		assertEquals(StateMachine.State.PREPARE, machine.getCurrentState());

		this.clock.leap(299, ChronoUnit.SECONDS);
		machine.run(this.context());
		assertEquals(StateMachine.State.PREPARE, machine.getCurrentState());

		this.clock.leap(1, ChronoUnit.SECONDS);
		machine.run(this.context());
		assertEquals(StateMachine.State.DONE, machine.getCurrentState());
	}

	private Context context() {
		return new Context(this.controller, MyConfig.create().setId("ctrl0").build(), this.clock, this.ess);
	}

	private static class TestStateMachine extends StateMachine {

		TestStateMachine() {
			super(State.PREPARE);
		}

		@Override
		public StateHandler<State, Context> getStateHandler(State state) {
			return state == State.PREPARE ? new TestHandler() : super.getStateHandler(state);
		}

		private TestHandler prepareHandler() {
			return (TestHandler) this.getCurrentStateHandler();
		}
	}

	private static class TestHandler extends AbstractAllowedPowerHandler {

		private boolean cleanupCalled;

		@Override
		protected StateMachine.State runAndGetNextState(Context context) {
			return this.isBlockedForFiveMinutes(context.getClock(), context.isDischargeBlocked())
					? StateMachine.State.DONE
					: StateMachine.State.PREPARE;
		}

		@Override
		protected void onExit(Context context) throws OpenemsNamedException {
			super.onExit(context);
			this.cleanupCalled = true;
		}
	}
}
