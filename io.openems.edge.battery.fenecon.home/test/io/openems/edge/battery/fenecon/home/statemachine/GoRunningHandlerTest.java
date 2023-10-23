package io.openems.edge.battery.fenecon.home.statemachine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.BooleanConsumer;
import io.openems.edge.battery.fenecon.home.statemachine.GoRunningHandler.SubState;
import io.openems.common.test.TimeLeapClock;

public class GoRunningHandlerTest {

	private static TimeLeapClock CLOCK;
	private static DummyRunnable RETRY_MODBUS_COMMUNICATION;
	private static DummyBooleanConsumer SET_BATTERY_START_UP_RELAY;

	@Before
	public void before() {
		CLOCK = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		RETRY_MODBUS_COMMUNICATION = new DummyRunnable();
		SET_BATTERY_START_UP_RELAY = new DummyBooleanConsumer();
	}

	@Test
	public void testFromNull() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		var context = generateContext(null, null, true);
		sut.onEntry(context);

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		assertEquals("GoRunning-Undefined", sut.debugLog());
		sut.runAndGetNextState(generateContext(null, null, true));

		// INITIAL_WAIT_FOR_BMS_CONTROL
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false /* not started */, true));

		// START_UP_RELAY_ON (1)
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		assertFalse(SET_BATTERY_START_UP_RELAY.getLastValue()); // before OFF
		sut.runAndGetNextState(generateContext(false, false, true));

		// START_UP_RELAY_ON (2)
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		sut.runAndGetNextState(generateContext(true /* now ON */, false, true));
		assertTrue(SET_BATTERY_START_UP_RELAY.getLastValue()); // now ON

		// START_UP_RELAY_HOLD (1)
		assertEquals(SubState.START_UP_RELAY_HOLD, sut.grs.subState());
		((TimeLeapClock) context.clock).leap(6, ChronoUnit.SECONDS);
		sut.runAndGetNextState(generateContext(true, false, true));

		// START_UP_RELAY_HOLD (1)
		assertEquals(SubState.START_UP_RELAY_HOLD, sut.grs.subState());
		((TimeLeapClock) context.clock).leap(5, ChronoUnit.SECONDS);
		sut.runAndGetNextState(generateContext(true, false, true));

		// START_UP_RELAY_OFF (1)
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(true, false, true));

		// START_UP_RELAY_OFF (2)
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false /* now OFF */, false, true));

		// RETRY_MODBUS_COMMUNICATION
		RETRY_MODBUS_COMMUNICATION.reset();
		assertEquals(SubState.RETRY_MODBUS_COMMUNICATION, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, false, true));
		assertTrue(RETRY_MODBUS_COMMUNICATION.hasBeenCalled());

		// WAIT_FOR_BMS_CONTROL (1)
		assertEquals(SubState.WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, false, true));

		// WAIT_FOR_BMS_CONTROL (2)
		assertEquals(SubState.WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, true /* now ON */, true));

		// WAIT_FOR_MODBUS_COMMUNICATION (1)
		assertEquals(SubState.WAIT_FOR_MODBUS_COMMUNICATION, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, false, true));

		// WAIT_FOR_MODBUS_COMMUNICATION (2)
		assertEquals(SubState.WAIT_FOR_MODBUS_COMMUNICATION, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, false, false /* no failure */));

		// FINISHED
		assertEquals(SubState.FINISHED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, false, false));
	}

	@Test
	public void testFromBmsControlOff() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(false, null, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false, true));

		// START_UP_RELAY_ON (1)
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, null, true));
	}

	@Test
	public void testFromBmsControlOn() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(false, null, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, true, true));

		// START_UP_RELAY_ON (1)
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(false, true, true));
	}

	@Test
	public void testFromNullRelayIsOn() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(null, null, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		// INITIAL_WAIT_FOR_BMS_CONTROL (1)
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		CLOCK.leap(10, ChronoUnit.SECONDS);

		// INITIAL_WAIT_FOR_BMS_CONTROL (2)
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, true, true));

		// START_UP_RELAY_OFF
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, true, true));
	}

	@Test
	public void testBmsControlTimeout() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(null, null, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		// INITIAL_WAIT_FOR_BMS_CONTROL (1)
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		CLOCK.leap(10, ChronoUnit.SECONDS);

		// INITIAL_WAIT_FOR_BMS_CONTROL (2)
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		CLOCK.leap(60, ChronoUnit.SECONDS);

		// INITIAL_WAIT_FOR_BMS_CONTROL (3)
		assertEquals(SubState.INITIAL_WAIT_FOR_BMS_CONTROL, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));

		// START_UP_RELAY_OFF
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, null, true));
	}

	@Test
	public void testRelayTimeout() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(null, false, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false, true));

		// START_UP_RELAY_ON (1)
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false, true));

		CLOCK.leap(61, ChronoUnit.SECONDS);

		// START_UP_RELAY_ON (2)
		assertEquals(SubState.START_UP_RELAY_ON, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false, true));

		// RETRY_MODBUS_COMMUNICATION
		assertEquals(SubState.RETRY_MODBUS_COMMUNICATION, sut.grs.subState());
		sut.runAndGetNextState(generateContext(null, false, true));
	}

	@Test(expected = OpenemsException.class)
	public void testGlobalTimeout() throws OpenemsNamedException {
		var sut = new GoRunningHandler();
		sut.onEntry(generateContext(null, false, true));

		// UNDEFINED
		assertEquals(SubState.UNDEFINED, sut.grs.subState());
		sut.runAndGetNextState(generateContext(true, true, true));

		// START_UP_RELAY_OFF (1)
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(true, true, true));

		CLOCK.leap(121, ChronoUnit.SECONDS);

		// START_UP_RELAY_OFF (2) -> Timeout
		assertEquals(SubState.START_UP_RELAY_OFF, sut.grs.subState());
		sut.runAndGetNextState(generateContext(true, true, true));
	}

	private static Context generateContext(Boolean batteryStartUpRelay, Boolean bmsControl,
			Boolean modbusCommunicationFailed) {
		return new Context(null, CLOCK, batteryStartUpRelay, SET_BATTERY_START_UP_RELAY, bmsControl,
				modbusCommunicationFailed, RETRY_MODBUS_COMMUNICATION);
	}

	private static class DummyBooleanConsumer implements BooleanConsumer {

		private boolean lastValue = false;

		public boolean getLastValue() {
			return this.lastValue;
		}

		@Override
		public void accept(boolean value) {
			this.lastValue = value;
		}
	}

	private static class DummyRunnable implements Runnable {

		private boolean hasBeenCalled = false;

		public void reset() {
			this.hasBeenCalled = false;
		}

		public boolean hasBeenCalled() {
			return this.hasBeenCalled;
		}

		@Override
		public void run() {
			this.hasBeenCalled = true;
		}
	}

}
