package io.openems.edge.core.appmanager;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.ServiceUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class ResolveDependencies implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ResolveDependencies.class);

	private final BundleContext bundleContext;

	public ResolveDependencies(//
			BundleContext bundleContext //
	) {
		super();
		this.bundleContext = bundleContext;
	}

	@Override
	public void run() {
		try (var appManagerService = ServiceUtils.useService(this.bundleContext, AppManager.class); //
				var appManagerUtilService = ServiceUtils.useService(this.bundleContext, AppManagerUtil.class)) {
			final var appManager = appManagerService.getService();
			final var appManagerUtil = appManagerUtilService.getService();

			if (appManager == null || appManagerUtil == null) {
				LOG.warn("Unable to get references to AppManager and/or AppManagerUtil!");
				return;
			}

			resolveDependencies(null, (AppManagerImpl) appManager, appManagerUtil);
			LOG.info("Resolved dependencies.");
		} catch (Exception e) {
			// unable to get references or resolve
			LOG.error("Could not resolve dependencies!", e);
		}
	}

	/**
	 * Resolves missing dependencies.
	 * 
	 * <p>
	 * protected so it can be used in a unit test
	 * 
	 * @param user           the executing {@link User}
	 * @param appManagerImpl the {@link AppManagerImpl}
	 * @param appManagerUtil the {@link AppManagerUtil}
	 */
	public static void resolveDependencies(User user, AppManagerImpl appManagerImpl, AppManagerUtil appManagerUtil) {
		final var instances = appManagerImpl.getInstantiatedApps();
		for (var instance : instances) {
			try {
				var configuration = appManagerUtil.getAppConfiguration(ConfigurationTarget.UPDATE, instance,
						Language.DEFAULT);

				// check if instance should have dependencies
				if (configuration.dependencies() == null || configuration.dependencies().isEmpty()) {
					if (instance.dependencies != null && !instance.dependencies.isEmpty()) {
						LOG.info(String.format("Instance %s has unnecessary dependencies!", instance.instanceId));
					}
					continue;
				}

				// remove satisfied dependencies
				for (var dependency : configuration.dependencies()) {
					// dependency exists
					if (instance.dependencies != null && instance.dependencies.stream() //
							.anyMatch(t -> t.key.equals(dependency.key))) {
						continue;
					}

					// dependency not found
					final var config = determineDependencyConfig(//
							appManagerImpl, appManagerUtil, dependency.appConfigs//
					);

					if (config == null || config.appId == null) {
						continue;
					}

					if (// checkstyle requires new line
					switch (dependency.createPolicy) {
					case NEVER -> {
						// can not resolve dependency automatically
						LOG.warn(String.format("Unable to automatically add dependency for %s and key %s.",
								instance.instanceId, dependency.key));
						yield false;
					}
					case IF_NOT_EXISTING -> instances.stream().anyMatch(t -> t.appId.equals(config.appId));
					case ALWAYS -> false;
					}) {
						continue;
					}

					try {
						LOG.info(String.format("Resolving dependency with installing %s!", config.appId));
						appManagerImpl.handleAddAppInstanceRequest(user, //
								new AddAppInstance.Request(//
										config.appId, "key", //
										config.alias, //
										config.initialProperties),
								true);
						resolveDependencies(user, appManagerImpl, appManagerUtil);
						return;
					} catch (OpenemsNamedException e) {
						e.printStackTrace();
					}
				}

			} catch (OpenemsNamedException e) {
				// unable to get configuration of instance
				LOG.error("Unable to get AppConfiguration", e);
			}
		}
	}

	private static AppDependencyConfig determineDependencyConfig(//
			final AppManagerImpl appManagerImpl, //
			final AppManagerUtil appManagerUtil, //
			final List<AppDependencyConfig> configs //
	) {
		if (configs == null || configs.isEmpty()) {
			return null;
		}
		// if there is already an instance which can be used for the dependency return
		// null so no new instance gets installed
		for (var config : configs) {
			var instances = appManagerImpl.getInstantiatedApps().stream().filter(i -> i.appId.equals(config.appId))
					.toList();
			for (var instance : instances) {
				var existingDependencies = appManagerUtil.getAppsWithDependencyTo(instance);
				if (existingDependencies.isEmpty()) {
					return null;
				}
			}
		}

		return configs.get(0);
	}

}
