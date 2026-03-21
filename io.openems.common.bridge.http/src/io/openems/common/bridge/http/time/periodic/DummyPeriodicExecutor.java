package io.openems.common.bridge.http.time.periodic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.openems.common.bridge.http.time.DelayTimeProvider;

public class DummyPeriodicExecutor implements PeriodicExecutor {
	public DummyPeriodicExecutor(String name, Supplier<DelayTimeProvider.Delay> action) {
		// Start the action in a new thread to mock the behavior of the real
		// PeriodicExecutor
		var resultingException = new AtomicReference<Exception>();
		var thread = Thread.ofVirtual().name(name).start(() -> {
			try {
				action.get();
			} catch (Exception ex) {
				resultingException.set(ex);
			}
		});

		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("An error occurred while executing task '%s'".formatted(name), e);
		}

		if (resultingException.get() != null) {
			throw new RuntimeException("An error occurred while executing task '%s'".formatted(name),
					resultingException.get());
		}
	}

	@Override
	public long getRemainingDelayUntilNextRun(TimeUnit unit) {
		return 0;
	}

	@Override
	public void dispose() {
	}
}
