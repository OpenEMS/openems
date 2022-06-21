package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;

public interface AggregateTask {

	/**
	 * Aggregates the given instance.
	 *
	 * @param instance  the {@link AppConfiguration} of the instance
	 * @param oldConfig the old configuration of the instance
	 */
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig);

	/**
	 * e. g. creates components that were aggregated by the instances and my also
	 * delete unused components.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other existing {@link AppConfiguration}s
	 * @throws OpenemsNamedException on error
	 */
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException;

	/**
	 * e. g. deletes components that were aggregated.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other existing {@link AppConfiguration}s
	 * @throws OpenemsNamedException on error
	 */
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException;

	/**
	 * Resets the task.
	 */
	public void reset();

	public static interface ComponentAggregateTask extends AggregateTask {

		/**
		 * Gets the Components that were created.
		 *
		 * @return the created {@link EdgeConfig.Component}
		 */
		public List<EdgeConfig.Component> getCreatedComponents();

		/**
		 * Gets the Components that were deleted.
		 *
		 * @return the id's of the deleted {@link EdgeConfig.Component}
		 */
		public List<String> getDeletedComponents();

	}

	public static interface SchedulerAggregateTask extends AggregateTask {

	}

	public static interface StaticIpAggregateTask extends AggregateTask {

	}

}
