package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;

public interface AppManagerUtil {

	/**
	 * Gets the {@link OpenemsApp} for the given appId.
	 *
	 * @param appId the appId of the {@link OpenemsApp}
	 * @return the {@link OpenemsApp}
	 * @throws NoSuchElementException if there is no {@link OpenemsApp} with the
	 *                                given appId
	 */
	public OpenemsApp getAppById(String appId) throws NoSuchElementException;

	/**
	 * Gets the {@link OpenemsAppInstance} for the given instanceId.
	 *
	 * @param instanceId the instanceId of the {@link OpenemsAppInstance}
	 * @return the {@link OpenemsAppInstance}
	 * @throws NoSuchElementException if there is not {@link OpenemsAppInstance}
	 *                                with the given instanceId
	 */
	public OpenemsAppInstance getInstanceById(UUID instanceId) throws NoSuchElementException;

	/**
	 * Gets the {@link AppConfiguration} with the given parameter.
	 *
	 * @param target     the {@link ConfigurationTarget} of the configuration
	 * @param app        the {@link OpenemsApp}
	 * @param alias      the alias of the instance
	 * @param properties the {@link JsonObject} properties of the instance
	 * @param language   the {@link Language} of the configuration
	 * @return the {@link AppConfiguration}
	 * @throws OpenemsException on error
	 */
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, OpenemsApp app, String alias,
			JsonObject properties, Language language) throws OpenemsNamedException;

	/**
	 * Gets the {@link AppConfiguration} with the given parameter.
	 * 
	 * @param target     the {@link ConfigurationTarget} of the configuration
	 * @param appId      the {@link OpenemsApp#getAppId()} of the {@link OpenemsApp}
	 * @param alias      the alias of the instance
	 * @param properties the {@link JsonObject} properties of the instance
	 * @param language   the {@link Language} of the configuration
	 * @return the {@link AppConfiguration}
	 * @throws OpenemsException on error
	 */
	public default AppConfiguration getAppConfiguration(ConfigurationTarget target, String appId, String alias,
			JsonObject properties, Language language) throws OpenemsNamedException {
		return this.getAppConfiguration(target, this.getAppById(appId), alias, properties, language);
	}

	/**
	 * Gets the {@link AppConfiguration} with the given parameter.
	 * 
	 * @param target   the {@link ConfigurationTarget} of the configuration
	 * @param app      the {@link OpenemsApp#getAppId()} of the {@link OpenemsApp}
	 * @param instance the {@link OpenemsAppInstance}
	 * @param language the {@link Language} of the configuration
	 * @return the {@link AppConfiguration}
	 * @throws OpenemsException on error
	 */
	public default AppConfiguration getAppConfiguration(ConfigurationTarget target, OpenemsApp app,
			OpenemsAppInstance instance, Language language) throws OpenemsNamedException {
		return this.getAppConfiguration(target, app, instance.alias, instance.properties, language);
	}

	/**
	 * Gets the {@link AppConfiguration} with the given parameter.
	 * 
	 * @param target   the {@link ConfigurationTarget} of the configuration
	 * @param instance the {@link OpenemsAppInstance}
	 * @param language the {@link Language} of the configuration
	 * @return the {@link AppConfiguration}
	 * @throws OpenemsException on error
	 */
	public default AppConfiguration getAppConfiguration(ConfigurationTarget target, OpenemsAppInstance instance,
			Language language) throws OpenemsNamedException {
		return this.getAppConfiguration(target, this.getAppById(instance.appId), instance, language);
	}

	/**
	 * Gets all {@link OpenemsAppInstance Instances} which have a dependency to the
	 * given {@link OpenemsAppInstance instance}.
	 * 
	 * @param instance the instance which is referenced
	 * @return the referencing instances
	 */
	public List<OpenemsAppInstance> getAppsWithDependencyTo(OpenemsAppInstance instance);

}
