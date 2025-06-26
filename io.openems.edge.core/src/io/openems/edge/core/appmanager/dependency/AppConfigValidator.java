package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;

@Component(service = AppConfigValidator.class)
public class AppConfigValidator {

	@Reference
	private AppManagerUtil appManagerUtil;

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile List<AggregateTask<?>> tasks;

	public AppConfigValidator() {
	}

	@Activate
	private void activate() {

	}

	/**
	 * Validates the expected configuration of an app the actual configuration on
	 * the system.
	 * 
	 * @param instance the instance to validate
	 * @throws OpenemsNamedException on error
	 */
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException {
		final var configuration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE, instance,
				Language.DEFAULT);

		final var errors = new ArrayList<String>();
		for (var task : configuration.tasks()) {
			final var aggregateTask = this.findTaskByClass(task.aggregateTaskClass());
			if (aggregateTask == null) {
				errors.add("Missing AggregateTask to validate " + task.aggregateTaskClass().getCanonicalName());
				continue;
			}

			validate(aggregateTask, errors, configuration, task.configuration());
		}

		this.validateDependecies(errors, instance.dependencies, configuration.dependencies());

		if (!errors.isEmpty()) {
			throw new OpenemsException(String.join("|", errors));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> void validate(AggregateTask<T> aggregateTask, List<String> errors,
			AppConfiguration appConfiguration, Object configuration) {
		aggregateTask.validate(errors, appConfiguration, (T) configuration);
	}

	private AggregateTask<?> findTaskByClass(Class<? extends AggregateTask<?>> clazz) {
		return this.tasks.stream() //
				.filter(t -> clazz.isAssignableFrom(t.getClass())) //
				.findAny() //
				.orElse(null);
	}

	private void validateDependecies(//
			List<String> errors, //
			List<Dependency> configDependencies, //
			List<DependencyDeclaration> neededDependencies //
	) {
		// find dependencies that are not in config
		var notRegisteredDependencies = neededDependencies.stream().filter(
				t -> configDependencies == null || !configDependencies.stream().anyMatch(o -> o.key.equals(t.key)))
				.toList();

		// check if exactly one app is available of the needed appId
		for (var dependency : notRegisteredDependencies) {
			List<String> minErrors = null;
			for (var appConfig : dependency.appConfigs) {
				var appConfigErrors = new LinkedList<String>();
				if (appConfig.specificInstanceId != null) {
					try {
						final var instance = this.appManagerUtil.findInstanceByIdOrError(appConfig.specificInstanceId);
						final var app = this.appManagerUtil.findAppById(instance.appId);
						final var props = app.map(a -> {
							try {
								return AbstractOpenemsApp.fillUpProperties(a, instance.properties);
							} catch (UnsupportedOperationException e) {
								return instance.properties;
							}
						}).orElse(instance.properties);
						checkProperties(errors, props, appConfig, dependency.key);
					} catch (OpenemsNamedException e) {
						appConfigErrors.add(e.getMessage());
					}
				} else {
					var list = this.appManagerUtil.getInstantiatedAppsOfApp(appConfig.appId);
					if (list.size() != 1) {
						errors.add("Missing dependency with Key[" + dependency.key + "] needed App[" + appConfig.appId
								+ "]");
					} else {
						checkProperties(errors, list.get(0).properties, appConfig, dependency.key);
					}
				}

				if (minErrors == null || minErrors.size() > appConfigErrors.size()) {
					minErrors = appConfigErrors;
				}
			}

			errors.addAll(minErrors);
		}

		if (configDependencies == null) {
			return;
		}
		// check if dependency apps are available
		for (var dependency : configDependencies) {
			final OpenemsAppInstance appInstance;
			try {
				appInstance = this.appManagerUtil.findInstanceByIdOrError(dependency.instanceId);
			} catch (OpenemsNamedException e) {
				errors.add(e.getMessage());
				continue;
			}
			final var dependencyDeclaration = neededDependencies.stream() //
					.filter(d -> d.key.equals(dependency.key)) //
					.findAny().orElse(null);
			if (dependencyDeclaration == null) {
				errors.add("Can not get DependencyDeclaration of Dependency[" + dependency.key + "]");
				continue;
			}

			// get app config
			var appConfig = dependencyDeclaration.appConfigs.stream() //
					.filter(c -> c.specificInstanceId != null) //
					.filter(c -> c.specificInstanceId.equals(appInstance.instanceId)) //
					.findAny();

			if (appConfig.isEmpty()) {
				appConfig = dependencyDeclaration.appConfigs.stream() //
						.filter(c -> c.appId != null) //
						.filter(c -> c.appId.equals(appInstance.appId)) //
						.findAny();

				if (appConfig.isEmpty()) {
					errors.add("Can not get DependencyAppConfig of Dependency[" + dependency.key + "]");
					continue;
				}
			}

			var copy = appInstance.properties.deepCopy();
			try {
				final var app = this.appManagerUtil.findAppByIdOrError(appInstance.appId);
				copy = AbstractOpenemsApp.fillUpProperties(app, appInstance.properties);
			} catch (OpenemsNamedException e) {
				errors.add(e.getMessage());
			} catch (UnsupportedOperationException e) {
				// get props not supported
			}
			// when available check properties
			checkProperties(errors, copy, appConfig.get(), dependency.key);
		}
	}

	private static final void checkProperties(List<String> errors, JsonObject actualAppProperties,
			DependencyDeclaration.AppDependencyConfig appDependencyConfig, String dependecyKey) {
		if (appDependencyConfig == null) {
			errors.add("SubApp with Key[" + dependecyKey + "] not found!");
			return;
		}

		for (var property : appDependencyConfig.properties.entrySet()) {
			var actualValue = actualAppProperties.get(property.getKey());
			if (actualValue == null) {
				errors.add("Value for Key[" + property.getKey() + "] not found!");
				continue;
			}
			var actual = actualValue.toString().replace("\"", "");
			var needed = property.getValue().toString().replace("\"", "");
			if (!actual.equals(needed)) {
				errors.add("Value for Key[" + property.getKey() + "] does not match: expected[" + needed + "] actual["
						+ actual + "]  !");
			}
		}
	}

}
