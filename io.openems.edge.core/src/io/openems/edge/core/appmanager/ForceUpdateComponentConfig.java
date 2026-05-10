package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.ServiceUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.Dependency;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class ForceUpdateComponentConfig implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ForceUpdateComponentConfig.class);

	private final BundleContext bundleContext;

	public ForceUpdateComponentConfig(BundleContext bundleContext) {
		super();
		this.bundleContext = bundleContext;
	}

	@Override
	public void run() {
		try (var appManagerService = ServiceUtils.useService(this.bundleContext, AppManager.class); //
				var appManagerUtilService = ServiceUtils.useService(this.bundleContext, AppManagerUtil.class);
				var componentManagerService = ServiceUtils.useService(this.bundleContext, ComponentManager.class)) {

			final var appManager = appManagerService.getService();
			final var appManagerUtil = appManagerUtilService.getService();
			final var componentManager = componentManagerService.getService();

			if (appManager == null || appManagerUtil == null || componentManager == null) {
				LOG.warn("Unable to get references to AppManager and/or AppManagerUtil and/or ComponentManager!");
				return;
			}
			checkForceUpdating((AppManagerImpl) appManager, appManagerUtil, componentManager);

		} catch (Exception e) {
			LOG.error("Could not force to update componentConfigs!", e);
		}
	}

	/**
	 * Checks the all components and properties in the appConfig if they should be
	 * updated and differ form the component in the EdgeConfig and if yes, it sends
	 * a updateComponentConfig request.
	 *
	 * @param appManagerImpl   the appManagerImpl
	 * @param appManagerUtil   the appManagerUtil
	 * @param componentManager the componentManager
	 */
	public static void checkForceUpdating(AppManagerImpl appManagerImpl, AppManagerUtil appManagerUtil,
			ComponentManager componentManager) {
		final var systemUser = new User("system", "System", Language.DEFAULT, Role.ADMIN);
		for (var instance : appManagerImpl.getInstantiatedApps()) {
			try {
				final var appConfig = appManagerUtil.getAppConfiguration(ConfigurationTarget.UPDATE, instance,
						Language.DEFAULT);

				for (var appInstance : appManagerUtil.getAppsWithDependencyTo(instance)) {
					appInstance.dependencies.forEach(dependency -> {
						checkDependencyPropertyForceUpdate(appManagerImpl, appManagerUtil, instance, appInstance,
								dependency, systemUser);
					});
				}

				for (var component : appConfig.getComponents()) {
					checkConfigForceUpdate(componentManager, component, systemUser);
				}
			} catch (OpenemsError.OpenemsNamedException e) {
				LOG.error("Unable to get AppConfiguration", e);
			}
		}
	}

	private static void checkConfigForceUpdate(ComponentManager componentManager, ComponentDef component,
			User systemUser) {
		var edgeConfigComponent = componentManager.getEdgeConfig().getComponent(component.id());

		if (edgeConfigComponent.isPresent()
				&& !edgeConfigComponent.get().getFactoryId().equals(component.factoryId())) {
			return;
		}

		List<UpdateComponentConfigRequest.Property> properties = new ArrayList<>();

		if (component.config().forceUpdateOrCreate()) {

			properties = new ArrayList<>(component.properties().values().stream().map(prop -> {
				return new UpdateComponentConfigRequest.Property(prop.name(), prop.value());
			}) //
					.toList());

			if (edgeConfigComponent.isEmpty()) {
				// Component doesn't exist, so it will be created
				properties.addAll(List.of(//
						new UpdateComponentConfigRequest.Property("id", component.id()), //
						new UpdateComponentConfigRequest.Property("alias", component.alias())));
				sendCreateComponentRequest(null, component.id(), component.factoryId(), properties, componentManager);
				return;
			}

		} else if (edgeConfigComponent.isPresent()) {
			properties = component.properties().values().stream() //
					.flatMap(property -> {
						if (property.forceUpdate()) {
							return Stream
									.of(new UpdateComponentConfigRequest.Property(property.name(), property.value()));
						}
						return edgeConfigComponent.flatMap(edgeComponent -> edgeComponent.getProperty(property.name())) //
								.map(prop -> new UpdateComponentConfigRequest.Property(//
										property.name(), prop)) //
								.stream();
					}) //
					.toList();
		}

		var isSameConfig = edgeConfigComponent //
				.filter(value -> ComponentUtilImpl.isSameConfiguration(null, component, value)) //
				.isPresent();

		if (!isSameConfig) {
			sendUpdateComponentConfigRequest(systemUser, component.id(), properties, componentManager);
		}
	}

	private static void checkDependencyPropertyForceUpdate(AppManagerImpl appManagerImpl, AppManagerUtil appManagerUtil,
			OpenemsAppInstance instance, OpenemsAppInstance appInstance, Dependency dependency, User systemUser) {
		try {
			var dependencyAppConfig = appManagerUtil.getAppConfiguration(ConfigurationTarget.UPDATE, appInstance,
					Language.DEFAULT);

			final var dependencyDeclaration = dependencyAppConfig.dependencies().stream() //
					.filter(t -> t.key.equals(dependency.key)) //
					.findFirst().orElse(null);
			if (dependencyDeclaration == null) {
				return;
			}

			dependencyDeclaration.appConfigs.forEach(dependencyConfig -> {
				final var updatedProperties = instance.properties.deepCopy();

				var mustUpdate = false;
				for (var property : dependencyConfig.getProperties().values()) {
					if (property.forceUpdate()
							&& !Objects.equals(instance.properties.get(property.name()), property.value())) {
						updatedProperties.add(property.name(), property.value());
						mustUpdate = true;
					}
				}

				if (mustUpdate) {
					sendUpdateAppRequest(systemUser, instance.alias, instance.instanceId, updatedProperties,
							appManagerImpl);
				}
			});
		} catch (Exception e) {
			LOG.error("Unable to get AppConfiguration", e);
		}
	}

	private static void sendCreateComponentRequest(User user, //
			String componentId, //
			String factoryId, //
			List<UpdateComponentConfigRequest.Property> properties, //
			ComponentManager componentManager //
	) {
		try {
			componentManager.handleCreateComponentConfigRequest(user, new CreateComponentConfig.Request(//
					factoryId, //
					properties));
			LOG.info("Creating component {}", componentId);
		} catch (OpenemsError.OpenemsNamedException e) {
			LOG.error("Failed to create component {}", componentId, e);
		}
	}

	private static void sendUpdateComponentConfigRequest(//
			User user, //
			String componentId, //
			List<UpdateComponentConfigRequest.Property> properties, //
			ComponentManager componentManager //
	) {
		try {
			componentManager.handleUpdateComponentConfigRequest(user, new UpdateComponentConfig.Request(//
					componentId, //
					properties));
			LOG.info("Updating the config of {}", componentId);
		} catch (OpenemsError.OpenemsNamedException e) {
			LOG.error("Failed to update the config of {}", componentId);
		}
	}

	private static void sendUpdateAppRequest(//
			final User user, //
			final String alias, //
			final UUID instanceId, //
			final JsonObject properties, //
			final AppManagerImpl appManagerImpl //
	) {
		try {
			appManagerImpl.handleUpdateAppInstanceRequest(user,
					new UpdateAppInstance.Request(instanceId, alias, properties));
			LOG.info("Successfully updating the app {}", alias);
		} catch (OpenemsError.OpenemsNamedException e) {
			LOG.error("Failed to update the app {}", alias);
		}
	}
}
