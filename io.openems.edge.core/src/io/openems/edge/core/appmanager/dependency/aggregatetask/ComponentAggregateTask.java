package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.List;

import io.openems.common.types.EdgeConfig;

public interface ComponentAggregateTask extends AggregateTask<ComponentConfiguration> {

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