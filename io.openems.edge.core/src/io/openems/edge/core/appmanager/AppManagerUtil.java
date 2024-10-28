package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;

public interface AppManagerUtil {

	/**
	 * Gets a {@link List} of the current installed {@link OpenemsAppInstance}.
	 * 
	 * @return the list of installed apps
	 */
	public List<OpenemsAppInstance> getInstantiatedApps();

	/**
	 * Gets a {@link List} of the current installed {@link OpenemsAppInstance} which
	 * match the given appId.
	 * 
	 * @param appId the appId which should match with
	 *              {@link OpenemsAppInstance#appId}
	 * @return a {@link List} of {@link OpenemsAppInstance}
	 */
	public default List<OpenemsAppInstance> getInstantiatedAppsOfApp(String appId) {
		Objects.requireNonNull(appId);
		return this.getInstantiatedApps().stream() //
				.filter(instance -> appId.equals(instance.appId)) //
				.toList();
	}

	/**
	 * Gets a {@link List} of the current installed {@link OpenemsAppInstance} which
	 * match the given appIds.
	 * 
	 * @param appIds the appIds which should match with
	 *               {@link OpenemsAppInstance#appId}
	 * @return a {@link List} of {@link OpenemsAppInstance}
	 */
	public default List<OpenemsAppInstance> getInstantiatedAppsOf(String... appIds) {
		return this.getInstantiatedApps().stream() //
				.filter(instance -> Stream.of(appIds) //
						.anyMatch(appId -> appId.equals(instance.appId))) //
				.toList();
	}

	/**
	 * Gets the installed apps which match any of the provided
	 * {@link OpenemsAppCategory OpenemsAppCategories}.
	 * 
	 * @param categories the {@link OpenemsAppCategory} to be contained by the app
	 * @return the found {@link OpenemsAppInstance OpenemsAppInstances}
	 */
	public List<OpenemsAppInstance> getInstantiatedAppsByCategories(OpenemsAppCategory... categories);

	/**
	 * Finds the {@link OpenemsApp} with the given id.
	 * 
	 * @param id the {@link OpenemsApp#getAppId()} of the app.
	 * @return a {@link Optional} of the app
	 */
	public Optional<OpenemsApp> findAppById(String id);

	/**
	 * Finds the {@link OpenemsApp} with the given id.
	 * 
	 * @param id the {@link OpenemsApp#getAppId()} of the app.
	 * @return the app
	 * @throws OpenemsNamedException if the app was not found
	 */
	public default OpenemsApp findAppByIdOrError(String id) throws OpenemsNamedException {
		return this.findAppById(id).orElseThrow(() -> new OpenemsException("Unable to find app with id '" + id + "'"));
	}

	/**
	 * Finds the {@link OpenemsAppInstance} with the given {@link UUID}.
	 *
	 * @param id the id of the instance
	 * @return a {@link Optional} of the instance
	 */
	public Optional<OpenemsAppInstance> findInstanceById(UUID id);

	/**
	 * Finds the {@link OpenemsAppInstance} with the given {@link UUID}.
	 * 
	 * @param id the {@link UUID} of the instance
	 * @return the instance
	 * @throws OpenemsNamedException if not found
	 */
	public default OpenemsAppInstance findInstanceByIdOrError(UUID id) throws OpenemsNamedException {
		return this.findInstanceById(id)
				.orElseThrow(() -> new OpenemsException("Unable to find instance with id '" + id + "'"));
	}

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
		return this.getAppConfiguration(target, this.findAppByIdOrError(appId), alias, properties, language);
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
		return this.getAppConfiguration(target, this.findAppByIdOrError(instance.appId), instance, language);
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
