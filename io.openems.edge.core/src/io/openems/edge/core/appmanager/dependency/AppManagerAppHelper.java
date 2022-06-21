package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl.UpdateValues;

public interface AppManagerAppHelper {

	/**
	 * Installs an {@link OpenemsApp} with all its {@link Dependency}s.
	 *
	 * @param user       the executing user
	 * @param properties the properties of the {@link OpenemsAppInstance}
	 * @param alias      the alias of the {@link OpenemsAppInstance}
	 * @param app        the {@link OpenemsApp}
	 * @return s a list of the created {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public List<OpenemsAppInstance> installApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException;

	/**
	 * Updates an existing {@link OpenemsAppInstance}.
	 *
	 * @param user        the executing user
	 * @param oldInstance the old {@link OpenemsAppInstance} with its
	 *                    configurations.
	 * @param properties  the properties of the new {@link OpenemsAppInstance}
	 * @param alias       the alias of the new {@link OpenemsAppInstance}
	 * @param app         the {@link OpenemsApp}
	 * @return s a list of the replaced {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues updateApp(User user, OpenemsAppInstance oldInstance, JsonObject properties, String alias,
			OpenemsApp app) throws OpenemsNamedException;

	/**
	 * Deletes an {@link OpenemsAppInstance}.
	 *
	 * @param user     the executing user
	 * @param instance the instance to delete
	 * @return s a list of the removed {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException;

}
