package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.utils.ServiceUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;

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
		var instances = appManagerImpl.getInstantiatedApps();
		instances.forEach(instance -> {
			try {
				var appConfig = appManagerUtil.getAppConfiguration(ConfigurationTarget.UPDATE, instance,
						Language.DEFAULT);

				appConfig.getComponents().forEach(component -> {

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
							sendCreateComponentRequest(null, component.id(), component.factoryId(), properties,
									componentManager);
							return;
						}

					} else {

						if (edgeConfigComponent.isPresent()) {
							properties = component.properties().values().stream().flatMap(property -> {
								if (property.forceUpdate()) {
									return Stream.of(new UpdateComponentConfigRequest.Property(property.name(),
											property.value()));
								}
								return edgeConfigComponent
										.flatMap(edgeComponent -> edgeComponent.getProperty(property.name())) //
										.map(prop -> new UpdateComponentConfigRequest.Property(//
												property.name(), prop)) //
										.stream();
							}) //
									.toList();
						}
					}

					var isSameConfig = edgeConfigComponent //
							.filter(value -> ComponentUtilImpl.isSameConfiguration(null, component, value)) //
							.isPresent();

					if (!isSameConfig) {
						sendUpdateComponentConfigRequest(null, component.id(), properties, componentManager);
					}

				});
			} catch (OpenemsError.OpenemsNamedException e) {
				LOG.error("Unable to get AppConfiguration", e);
			}
		});
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
}
