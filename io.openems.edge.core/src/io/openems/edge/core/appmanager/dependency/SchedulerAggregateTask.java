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

	@Activate
	public SchedulerAggregateTask(@Reference ComponentUtil componentUtil) {
		this.componentUtil = componentUtil;
		order = new LinkedList<>();
		removeIds = new LinkedList<>();
	}

	@Override
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig) throws OpenemsNamedException {
		order = componentUtil.insertSchedulerOrder(order, instance.schedulerExecutionOrder);

		if (oldConfig == null) {
			return;
		}
		var schedulerIdDiff = new ArrayList<>(oldConfig.schedulerExecutionOrder);
		schedulerIdDiff.removeAll(instance.schedulerExecutionOrder);
		removeIds.addAll(schedulerIdDiff);
	}

	@Override
	public void create(User user, List<EdgeConfig.Component> otherAppComponents) throws OpenemsNamedException {
		order = componentUtil.insertSchedulerOrder(componentUtil.getSchedulerIds(), order);
		componentUtil.updateScheduler(user, order, createdComponents);
		

		order = new LinkedList<>();
		removeIds = new LinkedList<>();
	}

	@Override
	public void delete(User user, List<EdgeConfig.Component> otherAppComponents) throws OpenemsNamedException {

	}
	
	public final void setCreatedComponents(List<EdgeConfig.Component> createdComponents) {
		this.createdComponents = createdComponents;
	}

}
