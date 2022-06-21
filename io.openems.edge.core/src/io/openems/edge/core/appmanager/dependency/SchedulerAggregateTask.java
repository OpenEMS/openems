package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;

@Component(name = "AppManager.AggregateTask.SchedulerAggregateTask")
public class SchedulerAggregateTask implements AggregateTask {

	private ComponentUtil componentUtil;
	private List<String> order;
	private List<String> removeIds;

	private List<EdgeConfig.Component> createdComponents;
	private List<String> deletedComponents;

	@Activate
	public SchedulerAggregateTask(@Reference ComponentUtil componentUtil) {
		this.componentUtil = componentUtil;
		order = new LinkedList<>();
		removeIds = new LinkedList<>();
	}

	@Override
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig) throws OpenemsNamedException {
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
		this.order = componentUtil.insertSchedulerOrder(componentUtil.getSchedulerIds(), this.order);
		this.componentUtil.updateScheduler(user, this.order, createdComponents);

		this.order = new LinkedList<>();
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.removeIds.addAll(deletedComponents);
		this.removeIds.removeAll(AppManagerAppHelperImpl.getSchedulerIdsFromConfigs(otherAppConfigurations));

		this.componentUtil.removeIdsInSchedulerIfExisting(user, this.removeIds);

		this.removeIds = new LinkedList<>();
	}

	public final void setCreatedComponents(List<EdgeConfig.Component> createdComponents) {
		this.createdComponents = createdComponents;
	}

	public final void setDeletedComponents(List<String> deletedComponents) {
		this.deletedComponents = deletedComponents;
	}

}
