package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;

@Component
public class SchedulerAggregateTaskImpl implements AggregateTask, AggregateTask.SchedulerAggregateTask {

	private final AggregateTask.ComponentAggregateTask aggregateTask;
	private final ComponentUtil componentUtil;

	private List<String> order;
	private List<String> removeIds;

	@Activate
	public SchedulerAggregateTaskImpl(@Reference AggregateTask.ComponentAggregateTask aggregateTask,
			@Reference ComponentUtil componentUtil) {
		this.aggregateTask = aggregateTask;
		this.componentUtil = componentUtil;
	}

	@Override
	public void reset() {
		this.order = new LinkedList<>();
		this.removeIds = new LinkedList<>();
	}

	@Override
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig) {
		if (instance != null) {
			this.order = this.componentUtil.insertSchedulerOrder(this.order, instance.schedulerExecutionOrder);
		}
		if (oldConfig != null) {
			var schedulerIdDiff = new ArrayList<>(oldConfig.schedulerExecutionOrder);
			if (instance != null) {
				schedulerIdDiff.removeAll(instance.schedulerExecutionOrder);
			}
			this.removeIds.addAll(schedulerIdDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.order = this.componentUtil.insertSchedulerOrder(this.componentUtil.getSchedulerIds(), this.order);
		this.componentUtil.updateScheduler(user, this.order, this.aggregateTask.getCreatedComponents());

		this.delete(user, otherAppConfigurations);
	}

	/**
	 * removes id's from the scheduler that were aggregated.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other existing {@link AppConfiguration}s
	 * @throws OpenemsNamedException on error
	 */
	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.removeIds.removeAll(AppManagerAppHelperImpl.getSchedulerIdsFromConfigs(otherAppConfigurations));
		this.removeIds.addAll(this.aggregateTask.getDeletedComponents());

		this.componentUtil.removeIdsInSchedulerIfExisting(user, this.removeIds);
	}

}
