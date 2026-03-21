package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.user.User;

public interface SchedulerByCentralOrderAggregateTask extends AggregateTask<SchedulerByCentralOrderConfiguration> {

	/**
	 * Fixes the order of the schedulers according to the central order.
	 * 
	 * @param user the user to perform the action
	 * @throws OpenemsError.OpenemsNamedException if the order cannot be fixed due
	 *                                            to an error
	 */
	void fixSchedulerOrder(User user) throws OpenemsError.OpenemsNamedException;

	/**
	 * Fixes the order of the schedulers according to the central order.
	 *
	 * @param user                          the user to perform the action
	 * @param additionalSchedulerComponents a list of additional scheduler
	 *                                      components to consider for fixing the
	 *                                      order
	 * @throws OpenemsError.OpenemsNamedException if the order cannot be fixed due
	 *                                            to an error
	 */
	void fixSchedulerOrder(User user,
			List<SchedulerByCentralOrderConfiguration.SchedulerComponent> additionalSchedulerComponents)
			throws OpenemsError.OpenemsNamedException;

}