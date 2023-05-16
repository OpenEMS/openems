package io.openems.edge.core.appmanager;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelper;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.dependency.ComponentAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.SchedulerAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.StaticIpAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.TemporaryApps;
import io.openems.edge.core.appmanager.dependency.UpdateValues;
import io.openems.edge.core.appmanager.validator.Validator;

public class DummyAppManagerAppHelper implements AppManagerAppHelper {

	/**
	 * Creates a {@link ComponentServiceObjects} of a {@link AppManagerAppHelper}
	 * with the given parameter.
	 * 
	 * @param componentManager the {@link ComponentManager}
	 * @param componentUtil    the {@link ComponentUtil}
	 * @param validator        the {@link Validator}
	 * @param appManagerUtil   the {@link AppManagerUtil}
	 * @return the {@link ComponentServiceObjects}
	 * @throws IllegalAccessException    on reflection error
	 * @throws InvocationTargetException on reflection error
	 */
	public static ComponentServiceObjects<AppManagerAppHelper> cso(//
			ComponentManager componentManager, //
			ComponentUtil componentUtil, //
			Validator validator, //
			AppManagerUtil appManagerUtil //
	) throws IllegalAccessException, InvocationTargetException {
		return new ComponentServiceObjects<AppManagerAppHelper>() {

			private final AppManagerAppHelper impl = new DummyAppManagerAppHelper(componentManager, componentUtil,
					validator, appManagerUtil);

			@Override
			public AppManagerAppHelper getService() {
				return this.impl;
			}

			@Override
			public void ungetService(AppManagerAppHelper service) {
				// empty for test
			}

			@Override
			public ServiceReference<AppManagerAppHelper> getServiceReference() {
				// not needed for test
				return null;
			}

		};
	}

	private final AppManagerAppHelperImpl impl;

	public DummyAppManagerAppHelper(//
			ComponentManager componentManager, //
			ComponentUtil componentUtil, //
			Validator validator, //
			AppManagerUtil util //
	) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final var componentTask = new ComponentAggregateTaskImpl(componentManager);
		final var schedulerTask = new SchedulerAggregateTaskImpl(componentTask, componentUtil);
		final var staticIpTask = new StaticIpAggregateTaskImpl(componentUtil);
		this.impl = new AppManagerAppHelperImpl(componentManager, componentUtil, validator, componentTask,
				schedulerTask, staticIpTask);

		ReflectionUtils.setAttribute(AppManagerAppHelperImpl.class, this.impl, "appManagerUtil", util);
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
