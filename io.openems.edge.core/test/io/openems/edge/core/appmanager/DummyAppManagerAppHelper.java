package io.openems.edge.core.appmanager;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelper;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.dependency.TemporaryApps;
import io.openems.edge.core.appmanager.dependency.UpdateValues;
import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.StaticIpAggregateTaskImpl;
import io.openems.edge.core.appmanager.validator.Validator;

public class DummyAppManagerAppHelper implements AppManagerAppHelper {

	private final AppManagerAppHelperImpl impl;

	private final List<AggregateTask<?>> tasks;

	public DummyAppManagerAppHelper(//
			ComponentManager componentManager, //
			ComponentUtil componentUtil, //
			Validator validator, //
			AppManagerUtil util //
	) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final var componentTask = new ComponentAggregateTaskImpl(componentManager);
		final var schedulerTask = new SchedulerAggregateTaskImpl(componentTask, componentUtil);
		final var staticIpTask = new StaticIpAggregateTaskImpl(componentUtil);
		this.tasks = List.of(staticIpTask, componentTask, schedulerTask);
		this.impl = new AppManagerAppHelperImpl(componentManager, componentUtil, validator);

		ReflectionUtils.setAttribute(AppManagerAppHelperImpl.class, this.impl, "tasks", this.tasks);
		ReflectionUtils.setAttribute(AppManagerAppHelperImpl.class, this.impl, "appManagerUtil", util);
	}

	public List<AggregateTask<?>> getTasks() {
		return this.tasks;
	}

	@Override
	public UpdateValues installApp(User user, OpenemsAppInstance instance, OpenemsApp app)
			throws OpenemsNamedException {
		return this.impl.installApp(user, instance, app);
	}

	@Override
	public UpdateValues updateApp(User user, OpenemsAppInstance oldInstance, OpenemsAppInstance instance,
			OpenemsApp app) throws OpenemsNamedException {
		return this.impl.updateApp(user, oldInstance, instance, app);
	}

	@Override
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException {
		return this.impl.deleteApp(user, instance);
	}

	@Override
	public TemporaryApps getTemporaryApps() {
		return this.impl.getTemporaryApps();
	}

}
