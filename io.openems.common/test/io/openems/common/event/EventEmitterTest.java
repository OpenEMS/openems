package io.openems.common.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class EventEmitterTest {
	@Test
	void testEventEmitter() {
		final var emitter = new EventEmitter<String>();
		final var tester = new EventEmitterTester<>(emitter);

		try {
			emitter.emit("Test1");
			tester.assertEmitted("Test1", true);
			assertThrows(RuntimeException.class, () -> tester.assertEmitted("Test1", false));

			emitter.emit("Test2");
			emitter.emit("Test3");
			tester.assertEmitted("Test2", false);
			tester.assertEmitted("Test3", false);
		} finally {
			tester.dispose();

			emitter.emit("Test5");
			assertThrows(RuntimeException.class, () -> tester.assertEmitted("Test5", false));
		}
	}

	@Test
	void testExceptions() {
		final var emitter = new EventEmitter<String>();

		final var calledSubscription = new AtomicBoolean(false);
		emitter.subscribe(value -> {
			calledSubscription.set(true);
			throw new RuntimeException("Some test exception occurred.");
		});

		assertDoesNotThrow(() -> emitter.emit("Test"));
		assertTrue(calledSubscription.get());
	}
}
