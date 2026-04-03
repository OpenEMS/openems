package io.openems.common.bridge.http.time.periodic;

import java.util.function.Supplier;

import io.openems.common.bridge.http.time.DelayTimeProvider;

/**
 * Dummy factory for {@link PeriodicExecutor} for unit tests. This dummy
 * executor is directly executing the action without any delay mechanics.
 */
public class DummyPeriodicExecutorFactory extends PeriodicExecutorFactory {
	@Override
	public PeriodicExecutor execute(String name, Supplier<DelayTimeProvider.Delay> action,
			DelayTimeProvider.Delay firstExecutionDelay) {
		return new DummyPeriodicExecutor(name, action);
	}
}
