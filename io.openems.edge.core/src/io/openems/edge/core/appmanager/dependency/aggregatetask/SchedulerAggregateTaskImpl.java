package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;

@Component(//
		service = { //
				AggregateTask.class, //
				SchedulerAggregateTask.class, //
				SchedulerAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class SchedulerAggregateTaskImpl implements SchedulerAggregateTask {

	private final ComponentAggregateTask aggregateTask;
	private final ComponentUtil componentUtil;

	private List<String> order;
	private List<String> removeIds;

	@Activate
	public SchedulerAggregateTaskImpl(//
			@Reference ComponentAggregateTask aggregateTask, //
			@Reference ComponentUtil componentUtil //
	) {
		this.aggregateTask = aggregateTask;
		this.componentUtil = componentUtil;
	}

	@Override
	public void reset() {
		this.order = new LinkedList<>();
		this.removeIds = new LinkedList<>();
	}

	@Override
	public void aggregate(//
			final SchedulerConfiguration currentConfiguration, //
			final SchedulerConfiguration lastConfiguration //
	) {
		if (currentConfiguration != null) {
			this.order = this.componentUtil.insertSchedulerOrder(this.order, currentConfiguration.componentOrder());
		}
		if (lastConfiguration != null) {
			var schedulerIdDiff = new ArrayList<>(lastConfiguration.componentOrder());
			if (currentConfiguration != null) {
				schedulerIdDiff.removeAll(currentConfiguration.componentOrder());
			}
			this.removeIds.addAll(schedulerIdDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}
		this.order = this.componentUtil.insertSchedulerOrder(this.componentUtil.getSchedulerIds(), this.order);
		this.componentUtil.updateScheduler(user, this.order, this.aggregateTask.getCreatedComponents());

		this.delete(user, otherAppConfigurations);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}
		var otherIds = AppConfiguration
				.flatMap(otherAppConfigurations, SchedulerAggregateTask.class, SchedulerConfiguration::componentOrder)
				.toList();
		this.removeIds.removeAll(otherIds);
		this.removeIds.addAll(this.aggregateTask.getDeletedComponents());

		this.componentUtil.removeIdsInSchedulerIfExisting(user, this.removeIds);
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateScheduler");
	}

	@Override
	public AggregateTaskExecuteConstraints getExecuteConstraints() {
		return new AggregateTaskExecuteConstraints(Set.of(//
				// Needs to run after the AggregateTask.ComponentAggregateTask to also remove
				// ids in the scheduler of components which got deleted
				ComponentAggregateTask.class //
		));
	}

	@Override
	public void validate(List<String> errors, AppConfiguration appConfiguration, SchedulerConfiguration configuration) {
		if (configuration.componentOrder().isEmpty()) {
			return;
		}

		// Prepare Queue
		var controllers = new LinkedList<>(this.componentUtil.removeIdsWhichNotExist(configuration.componentOrder(),
				appConfiguration.getComponents()));

		if (controllers.isEmpty()) {
			return;
		}

		List<String> schedulerIds;
		try {
			schedulerIds = this.componentUtil.getSchedulerIds();
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return;
		}

		var nextControllerId = controllers.poll();

		// Remove found Controllers from Queue in order
		for (var controllerId : schedulerIds) {
			if (controllerId.equals(nextControllerId)) {
				nextControllerId = controllers.poll();
			}
		}
		if (nextControllerId != null) {
			errors.add("Controller [" + nextControllerId + "] is not/wrongly configured in Scheduler");
		}
	}

	private boolean anyChanges() {
		return !this.order.isEmpty() //
				|| !this.removeIds.isEmpty() //
				|| !this.aggregateTask.getDeletedComponents().isEmpty();
	}

}
