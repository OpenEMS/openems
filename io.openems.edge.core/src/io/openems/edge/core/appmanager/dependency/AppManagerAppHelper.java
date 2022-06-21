package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

public interface AppManagerAppHelper {

	/**
	 * 
	 * @param user
	 * @param properties
	 * @param alias
	 * @param app
	 * @returns a list of the created {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException
	 */
	public List<OpenemsAppInstance> installApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException;

	/**
	 * 
	 * @param user
	 * @param properties
	 * @param alias
	 * @param app
	 * @returns a list of the replaced {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException
	 */
	public List<OpenemsAppInstance> updateApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException;

	/**
	 * 
	 * @param user
	 * @param instance
	 * @returns a list of the removed {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException
	 */
	public List<OpenemsAppInstance> deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException;

}
