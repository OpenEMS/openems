package io.openems.edge.core.appmanager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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

public class DummyAppManagerAppHelper implements AppManagerAppHelper {

	private final AppManagerAppHelperImpl impl;

	private final List<AggregateTask<?>> tasks;

	public DummyAppManagerAppHelper(//
			ComponentManager componentManager, //
			ComponentUtil componentUtil, //
			AppManagerUtil util //
	) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		this.tasks = new ArrayList<AggregateTask<?>>();
		this.impl = new AppManagerAppHelperImpl(componentManager, componentUtil);

		ReflectionUtils.setAttribute(AppManagerAppHelperImpl.class, this.impl, "tasks", this.tasks);
		ReflectionUtils.setAttribute(AppManagerAppHelperImpl.class, this.impl, "appManagerUtil", util);
	}

	/**
	 * Adds a {@link AggregateTask} to the current active ones.
	 * 
	 * @param task the {@link AggregateTask} to add
	 */
	public void addAggregateTask(AggregateTask<?> task) {
		this.tasks.add(task);
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
