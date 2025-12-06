package io.openems.edge.batteryinverter.victron.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Tests for the Victron State Machine.
 */
public class StateMachineTest {

	@Test
	public void testStateMachineCreation() {
		var stateMachine = new StateMachine(StateMachine.State.UNDEFINED);
		assertNotNull(stateMachine);
		assertEquals(StateMachine.State.UNDEFINED, stateMachine.getCurrentState());
	}

	@Test
	public void testStateMachineStates() {
		// Test all states can be set as initial state
		for (var state : StateMachine.State.values()) {
			var stateMachine = new StateMachine(state);
			assertEquals(state, stateMachine.getCurrentState());
		}
	}

	@Test
	public void testStateValues() {
		var states = StateMachine.State.values();
		assertNotNull(states);

		// Verify expected states exist
		assertEquals(6, states.length);
		assertNotNull(StateMachine.State.UNDEFINED);
		assertNotNull(StateMachine.State.GO_RUNNING);
		assertNotNull(StateMachine.State.RUNNING);
		assertNotNull(StateMachine.State.GO_STOPPED);
		assertNotNull(StateMachine.State.STOPPED);
		assertNotNull(StateMachine.State.ERROR);
	}

	@Test
	public void testStateIntValues() {
		assertEquals(-1, StateMachine.State.UNDEFINED.getValue());
		assertEquals(10, StateMachine.State.GO_RUNNING.getValue());
		assertEquals(11, StateMachine.State.RUNNING.getValue());
		assertEquals(20, StateMachine.State.GO_STOPPED.getValue());
		assertEquals(21, StateMachine.State.STOPPED.getValue());
		assertEquals(30, StateMachine.State.ERROR.getValue());
	}

	@Test
	public void testStateHandlers() {
		// Verify handlers are instantiated
		assertNotNull(new UndefinedHandler());
		assertNotNull(new GoRunningHandler());
		assertNotNull(new RunningHandler());
		assertNotNull(new GoStoppedHandler());
		assertNotNull(new StoppedHandler());
		assertNotNull(new ErrorHandler());
	}

	@Test
	public void testGetStateHandler() {
		var stateMachine = new StateMachine(StateMachine.State.UNDEFINED);

		// Test that all states return a non-null handler
		for (var state : StateMachine.State.values()) {
			var handler = stateMachine.getStateHandler(state);
			assertNotNull("Handler for state " + state + " should not be null", handler);
		}
	}

}
