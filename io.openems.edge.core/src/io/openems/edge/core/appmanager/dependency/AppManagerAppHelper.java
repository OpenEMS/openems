package io.openems.edge.core.appmanager.dependency;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

public interface AppManagerAppHelper {

	/**
	 * Installs an {@link OpenemsApp} with all its {@link Dependency}s.
	 *
	 * @param user     the executing user
	 * @param instance the settings of the new {@link OpenemsAppInstance}
	 * @param app      the {@link OpenemsApp}
	 * @return a list of the created {@link OpenemsAppInstance OpenemsAppInstances}
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues installApp(User user, OpenemsAppInstance instance, OpenemsApp app) throws OpenemsNamedException;

	/**
	 * Updates an existing {@link OpenemsAppInstance}.
	 *
	 * @param user        the executing user
	 * @param oldInstance the old {@link OpenemsAppInstance} with its
	 *                    configurations.
	 * @param instance    the settings of the new {@link OpenemsAppInstance}
	 * @param app         the {@link OpenemsApp}
	 * @return a list of the replaced {@link OpenemsAppInstance OpenemsAppInstances}
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues updateApp(User user, OpenemsAppInstance oldInstance, OpenemsAppInstance instance,
			OpenemsApp app) throws OpenemsNamedException;

	/**
	 * Deletes an {@link OpenemsAppInstance}.
	 *
	 * @param user     the executing user
	 * @param instance the instance to delete
	 * @return a list of the removed {@link OpenemsAppInstance OpenemsAppInstances}
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException;

	/**
	 * Only available during a call of one of the other methods.
	 *
	 * @return null if none of the other methods is currently running else the
	 *         {@link TemporaryApps}
	 */
	public TemporaryApps getTemporaryApps();

}
