package io.openems.common.bridge.http.time.periodic;

import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.bridge.http.time.DelayTimeProvider;

@Component(service = PeriodicExecutorFactory.class, scope = ServiceScope.SINGLETON)
public class PeriodicExecutorFactory {

	/**
	 * Creates a new periodic executor that calls the given action is a periodic
	 * matter. The first run is delayed by the given firstExecutionDelay. All next
	 * runs are delayed by the return value of the action. The run can be stopped by
	 * using .dispose().
	 *
	 * @param name                Name that should be used for thread creation and
	 *                            log messages
	 * @param action              Action to be executed every delay
	 * @param firstExecutionDelay This delay is awaited before the action is
	 *                            executed the first time
	 * @return A new instance of {@link PeriodicExecutor}
	 */
	public PeriodicExecutor execute(String name, Supplier<DelayTimeProvider.Delay> action,
			DelayTimeProvider.Delay firstExecutionDelay) {
		return new PeriodicExecutorImpl(name, action, firstExecutionDelay);
	}

}
