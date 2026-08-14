package io.openems.edge.timedata.api.utils;

import static io.openems.edge.timedata.api.utils.TimedataQueryRetryHandler.Decision.GIVE_UP;
import static io.openems.edge.timedata.api.utils.TimedataQueryRetryHandler.Decision.RETRY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

class TimedataQueryRetryHandlerTest {

	private static final String COMPONENT_ID = "component0";
	private static final String CHANNEL_ID = "TestChannel";
	private static final RuntimeException DUMMY_THROWABLE = new RuntimeException("test error");

	private Instant now = Instant.EPOCH;

	private Clock fixedClock() {
		final var instant = this.now;
		return Clock.fixed(instant, ZoneId.of("UTC"));
	}

	private TimedataQueryRetryHandler createHandler() {
		return new TimedataQueryRetryHandler(NOPLogger.NOP_LOGGER, this.fixedClock(), COMPONENT_ID, CHANNEL_ID);
	}

	@Test
	void shouldRetry_whenFirstFailureOccurs() {
		final var handler = this.createHandler();

		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(RETRY, decision);
	}

	@Test
	void shouldRetry_whenWithinRetryWindow() {
		final var clock = new Clock[1];
		clock[0] = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
		final var handler = new TimedataQueryRetryHandler(NOPLogger.NOP_LOGGER, new Clock() {
			@Override
			public ZoneId getZone() {
				return clock[0].getZone();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return clock[0].withZone(zone);
			}

			@Override
			public Instant instant() {
				return clock[0].instant();
			}
		}, COMPONENT_ID, CHANNEL_ID);

		// First failure starts the window
		handler.onFailure(DUMMY_THROWABLE);

		// Advance 14 seconds (still inside the 15 s retry window)
		clock[0] = Clock.fixed(Instant.EPOCH.plusSeconds(14), ZoneId.of("UTC"));
		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(RETRY, decision);
	}

	@Test
	void shouldGiveUp_whenRetryWindowExpired() {
		final var clock = new Clock[1];
		clock[0] = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
		final var handler = new TimedataQueryRetryHandler(NOPLogger.NOP_LOGGER, new Clock() {
			@Override
			public ZoneId getZone() {
				return clock[0].getZone();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return clock[0].withZone(zone);
			}

			@Override
			public Instant instant() {
				return clock[0].instant();
			}
		}, COMPONENT_ID, CHANNEL_ID);

		// First failure starts the window
		handler.onFailure(DUMMY_THROWABLE);

		// Advance 15 seconds (exactly at the boundary -> window is expired)
		clock[0] = Clock.fixed(Instant.EPOCH.plusSeconds(15), ZoneId.of("UTC"));
		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(GIVE_UP, decision);
	}

	@Test
	void shouldRetry_whenRetryWindowRestartsAfterGiveUp() {
		final var clock = new Clock[1];
		clock[0] = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
		final var handler = new TimedataQueryRetryHandler(NOPLogger.NOP_LOGGER, new Clock() {
			@Override
			public ZoneId getZone() {
				return clock[0].getZone();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return clock[0].withZone(zone);
			}

			@Override
			public Instant instant() {
				return clock[0].instant();
			}
		}, COMPONENT_ID, CHANNEL_ID);

		// First window: start -> expire
		handler.onFailure(DUMMY_THROWABLE);
		clock[0] = Clock.fixed(Instant.EPOCH.plusSeconds(15), ZoneId.of("UTC"));
		handler.onFailure(DUMMY_THROWABLE); // GIVE_UP, resets firstFailedAt

		// Next failure must open a fresh window -> RETRY
		clock[0] = Clock.fixed(Instant.EPOCH.plusSeconds(20), ZoneId.of("UTC"));
		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(RETRY, decision);
	}

	@Test
	void shouldRetry_whenFailureOccursAfterReset() {
		final var handler = this.createHandler();

		handler.onFailure(DUMMY_THROWABLE); // starts window
		handler.reset(); // explicit reset

		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(RETRY, decision);
	}

	@Test
	void shouldRetry_whenOnlyOneFailureBeforeBoundary() {
		final var clock = new Clock[1];
		clock[0] = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
		final var handler = new TimedataQueryRetryHandler(NOPLogger.NOP_LOGGER, new Clock() {
			@Override
			public ZoneId getZone() {
				return clock[0].getZone();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return clock[0].withZone(zone);
			}

			@Override
			public Instant instant() {
				return clock[0].instant();
			}
		}, COMPONENT_ID, CHANNEL_ID);

		handler.onFailure(DUMMY_THROWABLE); // starts window

		// Advance to exactly 14999 ms (still inside)
		clock[0] = Clock.fixed(Instant.EPOCH.plusMillis(14_999), ZoneId.of("UTC"));
		final var decision = handler.onFailure(DUMMY_THROWABLE);

		assertEquals(RETRY, decision);
	}
}
