package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;
import io.openems.edge.core.appmanager.validator.Validator;

@Component
public class AppManagerAppHelperImpl implements AppManagerAppHelper {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final ComponentManager componentManager;
	private final ComponentUtil componentUtil;

	private final Validator validator;

	// tasks
	private final AggregateTask.ComponentAggregateTask componentsTask;
	private final AggregateTask.SchedulerAggregateTask schedulerTask;
	private final AggregateTask.StaticIpAggregateTask staticIpTask;

	private final AggregateTask[] tasks;

	// TODO maybe add temporary fields with currently installing apps

	@Activate
	public AppManagerAppHelperImpl(@Reference ComponentManager componentManager, @Reference ComponentUtil componentUtil,
			@Reference Validator validator, @Reference AggregateTask.ComponentAggregateTask componentsTask,
			@Reference AggregateTask.SchedulerAggregateTask schedulerTask,
			@Reference AggregateTask.StaticIpAggregateTask staticIpTask) {
		this.componentManager = componentManager;
		this.componentUtil = componentUtil;
		this.validator = validator;
		this.componentsTask = componentsTask;
		this.schedulerTask = schedulerTask;
		this.staticIpTask = staticIpTask;
		this.tasks = new AggregateTask[] { componentsTask, schedulerTask, staticIpTask };
	}

	@Override
	public UpdateValues installApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException {
		return this.updateApp(user, null, properties, alias, app);
	}

	@Override
	public UpdateValues updateApp(User user, OpenemsAppInstance oldInstance, JsonObject properties, String alias,
			OpenemsApp app) throws OpenemsNamedException {
		this.resetTasks();
		// TODO maybe check for all apps
		// if also checking dependencies these may be inconsistent
		// e. g. install HOME is requested it may have a dependency on a SOCOMEC Meter
		// but the meter has a checkable that there has to be a HOME installed
		// maybe add temporary apps in this component
		final var warnings = new LinkedList<String>();
		if (oldInstance == null) {
			this.checkStatus(app);
		} else {
			// determine if properties are allowed to be updated
			var references = this.getAppsWithReferenceTo(oldInstance.instanceId);
			for (var entry : this.getAppManagerImpl().appConfigs(references, null)) {
				for (var dependencieDeclaration : entry.getValue().dependencies) {

					var dd = entry.getKey().dependencies.stream()
							.filter(d -> d.instanceId.equals(oldInstance.instanceId))
							.filter(d -> d.key.equals(dependencieDeclaration.key)).findAny();

					if (dd.isEmpty()) {
						continue;
					}

					var dependencyApp = this.getAppManagerImpl().findInstaceById(dd.get().instanceId);
					var appConfig = dependencieDeclaration.appConfigs.stream()
							.filter(d -> d.appId.equals(dependencyApp.appId)).findAny();

					if (appConfig.isEmpty()) {
						// TODO can not get app config
						continue;
					}

					switch (dependencieDeclaration.dependencyUpdatePolicy) {
					case ALLOW_ALL:
						// everything can be changed
						break;
					case ALLOW_NONE:
						// reset to old config
						properties = oldInstance.properties;
						break;
					case ALLOW_ONLY_UNCONFIGURED_PROPERTIES:
						// override properties
						for (var propEntry : appConfig.get().properties.entrySet()) {
							if (!properties.has(propEntry.getKey())
									|| !properties.get(propEntry.getKey()).equals(propEntry.getValue())) {
								// TODO translation
								warnings.add("Can not change Property[" + propEntry.getKey()
										+ "] because of a Dependency constraint!");
								properties.add(propEntry.getKey(), propEntry.getValue());
							}

						}
						break;
					}
				}
			}
		}

		final var language = user == null ? null : user.getLanguage();

		var errors = new LinkedList<String>();

		var oldInstances = new TreeMap<AppIdKey, ExistingDependencyConfig>();
		var dependencieInstances = new HashMap<DependencyConfig, OpenemsAppInstance>();
		// all existing app dependencies
		if (oldInstance != null) {
			this.foreachExistingDependecy(oldInstance, ConfigurationTarget.UPDATE, language, dc -> {
				if (!dc.isDependency()) {
					return true;
				}
				oldInstances.put(new AppIdKey(dc.parentInstance.appId, dc.sub.key), dc);
				return true;
			});
		}

		var modifiedOrCreatedApps = new ArrayList<OpenemsAppInstance>();
		var deletedApps = new ArrayList<OpenemsAppInstance>();
		final var lastCreatedOrModifiedApp = new MutableValue<OpenemsAppInstance>();
		// update app and its dependencies
		this.foreachDependency(app, alias, properties, ConfigurationTarget.UPDATE, language,
				this::determineDependencyConfig, dc -> {
					// get old instance if existing
					ExistingDependencyConfig oldAppConfig = null;
					if (oldInstance != null) {
						if (dc.isDependency()) {
							oldAppConfig = oldInstances.remove(new AppIdKey(dc.parent.getAppId(), dc.sub.key));
							if (oldAppConfig != null) {
								for (var entry : oldAppConfig.appDependencyConfig.properties.entrySet()) {
									// add old values which are not set by the DependecyDeclaration
									if (!dc.appDependencyConfig.properties.has(entry.getKey())) {
										dc.appDependencyConfig.properties.add(entry.getKey(), entry.getValue());
									}
								}

							}
						} else {
							AppConfiguration oldAppConfiguration = null;
							try {
								oldAppConfiguration = dc.app.getAppConfiguration(ConfigurationTarget.UPDATE,
										oldInstance.properties, language);

							} catch (OpenemsNamedException e) {
								errors.add(e.getMessage());
							}

							var appDependencyConfig = DependencyDeclaration.AppDependencyConfig.create() //
									.setAppId(app.getAppId()) //
									.setAlias(oldInstance.alias) //
									.setProperties(oldInstance.properties) //
									.build();
							oldAppConfig = new ExistingDependencyConfig(app, null, null, oldAppConfiguration,
									appDependencyConfig, null, null, oldInstance);
						}
					}

					// map dependencies if this is the parent
					var dependecies = new ArrayList<Dependency>(dependencieInstances.size());
					if (!dependencieInstances.isEmpty()) {
						var isParent = !dc.isDependency();
						for (var dependency : dependencieInstances.entrySet()) {
							if (!isParent && !dc.config.dependencies.stream()
									.anyMatch(t -> t.equals(dependency.getKey().sub))) {
								isParent = false;
								break;
							}
							isParent = true;
							dependecies
									.add(new Dependency(dependency.getKey().sub.key, dependency.getValue().instanceId));
						}
						if (isParent) {
							dependencieInstances.clear();
						}
					}

					var otherAppConfigs = this.getAppManagerImpl().getOtherAppConfigurations(
							modifiedOrCreatedApps.stream().map(t -> t.instanceId).toArray(UUID[]::new));

					// create app or get as dependency
					if (oldAppConfig == null) {
						var neededApp = this.findNeededApp(dc);
						if (neededApp == null) {
							return false;
						}
						AppConfiguration oldConfig = null;
						UUID instanceId;
						OpenemsAppInstance oldInstanceOfCurrentApp = null;
						String aliasOfNewInstance = dc.appDependencyConfig.alias;
						if (neededApp.isPresent()) {
							instanceId = neededApp.get().instanceId;
							oldInstanceOfCurrentApp = neededApp.get();
							if (dc.sub.updatePolicy.isAllowedToUpdate(this.getAppManagerImpl().getInstantiatedApps(),
									null, neededApp.get())) {
								try {
									// update app
									oldConfig = dc.app.getAppConfiguration(ConfigurationTarget.UPDATE,
											neededApp.get().properties, language);
									for (var entry : neededApp.get().properties.entrySet()) {
										// add old values which are not set by the DependecyDeclaration
										if (!dc.appDependencyConfig.properties.has(entry.getKey())) {
											dc.appDependencyConfig.properties.add(entry.getKey(), entry.getValue());
										}
									}

									if (aliasOfNewInstance == null) {
										aliasOfNewInstance = oldInstanceOfCurrentApp.alias;
									}

								} catch (OpenemsNamedException e) {
									errors.add(e.getMessage());
								}
							}
						} else {
							// create app
							instanceId = UUID.randomUUID();

							// use app name as default alias if not given
							if (aliasOfNewInstance == null) {
								aliasOfNewInstance = dc.app.getName(language);
							}

							// check if the created app can satisfy another app dependency
							for (var instance : this.getAppManagerImpl().getInstantiatedApps()) {
								var neededDependency = this.getNeededDependencyTo(instance, dc.app.getAppId());
								if (neededDependency == null) {
									continue;
								}
								if (neededDependency.createPolicy == DependencyDeclaration.CreatePolicy.ALWAYS) {
									continue;
								}
								// override properties if set by dependency
								var config = determineDependencyConfig(neededDependency.appConfigs);
								for (var entry : config.properties.entrySet()) {
									if (!dc.appDependencyConfig.properties.has(entry.getKey())
											|| !dc.appDependencyConfig.properties.get(entry.getKey())
													.equals(entry.getValue())) {
										warnings.add("Override Property[" + entry.getKey()
												+ "] because of a Dependency constraint!");
									}
									dc.appDependencyConfig.properties.add(entry.getKey(), entry.getValue());
								}

								// update dependencies
								var alreadyModifiedAppIndex = modifiedOrCreatedApps.indexOf(instance);
								var replaceApp = instance;
								if (alreadyModifiedAppIndex != -1) {
									replaceApp = modifiedOrCreatedApps.get(alreadyModifiedAppIndex);
								}
								var newDependencies = new ArrayList<Dependency>();
								if (replaceApp.dependencies != null) {
									newDependencies.addAll(replaceApp.dependencies);
								}
								newDependencies.add(new Dependency(neededDependency.key, instanceId));
								modifiedOrCreatedApps.remove(replaceApp);
								modifiedOrCreatedApps.add(new OpenemsAppInstance(replaceApp.appId, replaceApp.alias,
										replaceApp.instanceId, replaceApp.properties, newDependencies));
							}
						}

						var newAppInstance = new OpenemsAppInstance(dc.app.getAppId(), aliasOfNewInstance, instanceId,
								dc.appDependencyConfig.properties, dependecies);
						lastCreatedOrModifiedApp.setValue(newAppInstance);
						modifiedOrCreatedApps.add(newAppInstance);
						dependencieInstances.put(dc, newAppInstance);
						try {
							var newConfig = this.getNewAppConfigWithReplacedIds(dc.app, oldInstanceOfCurrentApp,
									newAppInstance, AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigs),
									language);

							this.aggregateAllTasks(newConfig, oldConfig);
						} catch (OpenemsNamedException e) {
							errors.add(e.getMessage());
						}
						return true;
					}

					// find parent
					OpenemsAppInstance parent = null;
					if (dc.isDependency()) {
						if (dc.parent.getAppId().equals(oldInstance.appId)) {
							parent = oldInstance;
						} else {
							for (var entry : oldInstances.entrySet()) {
								if (entry.getValue().app.equals(dc.parent)) {
									parent = entry.getValue().instance;
								}
							}
						}
					}

					// update existing app
					if (dc.isDependency()
							&& !dc.sub.updatePolicy.isAllowedToUpdate(this.getAppManagerImpl().getInstantiatedApps(),
									parent, oldAppConfig.instance)) {
						// not allowed to update but still a dependency
						return true;
					}

					var newInstanceAlias = dc.appDependencyConfig.alias;
					if (newInstanceAlias == null) {
						newInstanceAlias = oldAppConfig.instance.alias;
					}

					var newAppInstance = new OpenemsAppInstance(dc.app.getAppId(), newInstanceAlias,
							oldAppConfig.instance.instanceId, dc.appDependencyConfig.properties, dependecies);
					lastCreatedOrModifiedApp.setValue(newAppInstance);
					modifiedOrCreatedApps.add(newAppInstance);
					dependencieInstances.put(dc, newAppInstance);

					try {
						var newAppConfig = this.getNewAppConfigWithReplacedIds(dc.app, oldAppConfig.instance,
								newAppInstance, AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigs),
								language);

						this.aggregateAllTasks(newAppConfig, oldAppConfig.config);

					} catch (OpenemsNamedException e) {
						errors.add(e.getMessage());
					}

					return true;
				});

		// add removed apps for deletion
		for (var entry : oldInstances.entrySet()) {
			var dc = entry.getValue();
			if (!dc.sub.deletePolicy.isAllowedToDelete(this.getAppManagerImpl().getInstantiatedApps(),
					dc.parentInstance, dc.instance)) {
				continue;
			}
			this.aggregateAllTasks(null, dc.config);
			deletedApps.add(dc.instance);
		}

		var ignoreInstances = new ArrayList<>(modifiedOrCreatedApps);
		ignoreInstances
				.addAll(oldInstances.entrySet().stream().map(t -> t.getValue().instance).collect(Collectors.toList()));

		var otherAppConfigs = this.getAppManagerImpl()
				.getOtherAppConfigurations(ignoreInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));

		try {
			// create or delete unused components
			this.componentsTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			log.error(e.getMessage());
			// TODO translation
			errors.add("Can not update Components!");
		}

		try {
			// update scheduler execute order
			this.schedulerTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			log.error(e.getMessage());
			// TODO translation
			errors.add("Can not update Scheduler!");
		}

		try {
			// update static ips
			this.staticIpTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			log.error(e.getMessage());
			// TODO translation
			errors.add("Can not update static ips!");
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}

		return new UpdateValues(lastCreatedOrModifiedApp.getValue(), modifiedOrCreatedApps, deletedApps, warnings);
	}

	private static final class MutableValue<T> {

		private T value;

		public MutableValue() {
			this(null);
		}

		public MutableValue(T value) {
			this.setValue(value);
		}

		public void setValue(T value) {
			this.value = value;
		}

		public T getValue() {
			return value;
		}

	}

	private final DependencyDeclaration getNeededDependencyTo(OpenemsAppInstance instance, String appId) {
		var app = this.getAppManagerImpl().findAppById(instance.appId);
		try {
			var neededDependencies = app.getAppConfiguration(ConfigurationTarget.UPDATE, instance.properties,
					null).dependencies;
			if (neededDependencies == null || neededDependencies.isEmpty()) {
				return null;
			}
			for (var neededDependency : neededDependencies) {
				// remove already satisfied dependencies
				if (instance.dependencies != null
						&& instance.dependencies.stream().anyMatch(d -> d.key.equals(neededDependency.key))) {
					continue;
				}
				// TODO when adding an app the current app can't be referenced
//				if (neededDependency.appConfigs.stream().filter(c -> c.specificInstanceId != null)
//						.anyMatch(c -> c.specificInstanceId.equals(instanceId))) {
//					return neededDependency;
//				}
				if (neededDependency.appConfigs.stream().filter(c -> c.appId != null)
						.anyMatch(c -> c.appId.equals(appId))) {
					return neededDependency;
				}

			}
		} catch (OpenemsNamedException e) {
			// can not get app configuration
		}
		return null;
	}

	public static final class UpdateValues {
		public final OpenemsAppInstance rootInstance;
		public final List<OpenemsAppInstance> modifiedOrCreatedApps;
		public final List<OpenemsAppInstance> deletedApps;

		public final List<String> warnings;

		public UpdateValues(OpenemsAppInstance rootInstance, List<OpenemsAppInstance> modifiedOrCreatedApps,
				List<OpenemsAppInstance> deletedApps) {
			this(rootInstance, modifiedOrCreatedApps, deletedApps, null);
		}

		public UpdateValues(OpenemsAppInstance rootInstance, List<OpenemsAppInstance> modifiedOrCreatedApps,
				List<OpenemsAppInstance> deletedApps, List<String> warnings) {
			this.rootInstance = rootInstance;
			this.modifiedOrCreatedApps = modifiedOrCreatedApps;
			this.deletedApps = deletedApps;
			this.warnings = warnings;
		}

	}

	private static class AppIdKey implements Comparable<AppIdKey> {
		public final String appId;
		public final String key;

		public AppIdKey(String appId, String key) {
			this.appId = appId;
			this.key = key;
		}

		@Override
		public int compareTo(AppIdKey o) {
			return this.toString().compareTo(o.toString());
		}

		@Override
		public String toString() {
			return this.appId + ":" + this.key;
		}
	}

	@Override
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException {
		this.resetTasks();

		// check if the app is allowed to be delete
		if (!this.isAllowedToDelete(instance)) {
			throw new OpenemsException("App is not allowed to be deleted because of a dependency constraint!");
		}

		var deletedInstances = new LinkedList<OpenemsAppInstance>();
		final var language = user == null ? null : user.getLanguage();
		final var errors = new LinkedList<String>();

		this.foreachExistingDependecy(instance, ConfigurationTarget.DELETE, language, dc -> {

			// check if dependency is allowed to be deleted by its parent
			if (dc.isDependency()) {
				switch (dc.sub.deletePolicy) {
				case NEVER:
					return false;
				case IF_MINE:
					if (this.getAppManagerImpl().getInstantiatedApps().stream()
							.anyMatch(a -> !a.equals(dc.parentInstance) && a.dependencies != null && a.dependencies
									.stream().anyMatch(d -> d.instanceId.equals(dc.instance.instanceId)))) {
						return false;
					}
					break;
				case ALWAYS:
					break;
				}
			}

			deletedInstances.add(dc.instance);

			this.aggregateAllTasks(null, dc.config);

			return true;
		});

		var unmodifiedApps = this
				.getAppsWithReferenceTo(deletedInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new)).stream()
				.filter(a -> !deletedInstances.stream().anyMatch(t -> t.equals(a))).collect(Collectors.toList());

		var modifiedApps = new ArrayList<OpenemsAppInstance>(unmodifiedApps.size());
		for (var app : unmodifiedApps) {
			var dependencies = new ArrayList<>(app.dependencies);
			dependencies.removeIf(d -> deletedInstances.stream().anyMatch(i -> i.instanceId.equals(d.instanceId)));
			modifiedApps.add(new OpenemsAppInstance(app.appId, //
					app.alias, app.instanceId, app.properties, dependencies));
		}

		var otherAppConfigs = this.getAppManagerImpl()
				.getOtherAppConfigurations(deletedInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));

		try {
			// delete components
			this.componentsTask.delete(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
		}

		try {
			// remove ids in scheduler
			this.schedulerTask.delete(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
		}

		try {
			// remove static ips
			this.staticIpTask.delete(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}

		return new UpdateValues(instance, modifiedApps, deletedInstances);
	}

	// private void getModifiedAppAssistant(User user, OpenemsApp app) {
	// TODO does not work when having multiple instances of the same app and only
	// one has properties that cant be edited

	private List<OpenemsAppInstance> getAppsWithReferenceTo(UUID... instanceIds) {
		return this.getAppManagerImpl().getInstantiatedApps() //
				.stream() //
				.filter(i -> i.dependencies != null && !i.dependencies.isEmpty()) //
				.filter(i -> i.dependencies.stream().anyMatch(//
						d -> Arrays.stream(instanceIds).anyMatch(id -> id.equals(d.instanceId)))) //
				.collect(Collectors.toList());
	}

	private final void aggregateAllTasks(AppConfiguration instance, AppConfiguration oldInstance) {
		for (var task : this.tasks) {
			task.aggregate(instance, oldInstance);
		}
	}

	private void resetTasks() {
		for (var task : this.tasks) {
			task.reset();
		}
	}

	/**
	 * Checks if the instance is allowed to be deleted depending on other apps
	 * dependencies to this instance.
	 *
	 * @param instance  the app to delete
	 * @param ignoreIds the instance id's that should be ignored
	 * @return true if it is allowed to delete the app
	 */
	private final boolean isAllowedToDelete(OpenemsAppInstance instance, UUID... ignoreIds) {
		// check if a parent does not allow deletion of this instance
		for (var entry : this.getAppManagerImpl().appConfigs(this.getAppsWithReferenceTo(instance.instanceId),
				AppManagerImpl.exludingInstanceIds(ignoreIds))) {
			for (var dependency : entry.getKey().dependencies) {
				if (!dependency.instanceId.equals(instance.instanceId)) {
					continue;
				}
				var declaration = entry.getValue().dependencies.stream().filter(dd -> dd.key.equals(dependency.key))
						.findAny();

				// declaration not found for dependency
				if (declaration.isEmpty()) {
					continue;
				}

				switch (declaration.get().dependencyDeletePolicy) {
				case ALLOWED:
					break;
				case NOT_ALLOWED:
					return false;
				}
			}
		}
		return true;
	}

	protected void checkStatus(OpenemsApp openemsApp) throws OpenemsNamedException {
		var validatorConfig = openemsApp.getValidatorConfig();
		var status = this.validator.getStatus(validatorConfig);
		switch (status) {
		case INCOMPATIBLE:
			throw new OpenemsException("App is not compatible! " + this.validator
					.getErrorCompatibleMessages(validatorConfig).stream().collect(Collectors.joining(";")));
		case COMPATIBLE:
			throw new OpenemsException("App can not be installed! " + this.validator
					.getErrorInstallableMessages(validatorConfig).stream().collect(Collectors.joining(";")));
		case INSTALLABLE:
			// app can be installed
			return;
		}
		throw new OpenemsException("Status '" + status.name() + "' is not implemented.");
	}

	protected static List<EdgeConfig.Component> getComponentsFromConfigs(List<AppConfiguration> configs) {
		var components = new LinkedList<EdgeConfig.Component>();
		for (var config : configs) {
			components.addAll(config.components);
		}
		return components;
	}

	protected static List<String> getSchedulerIdsFromConfigs(List<AppConfiguration> configs) {
		var ids = new LinkedList<String>();
		for (var config : configs) {
			ids.addAll(config.schedulerExecutionOrder);
		}
		return ids;
	}

	protected static List<String> getStaticIpsFromConfigs(List<AppConfiguration> configs) {
		var ips = new LinkedList<String>();
		for (var config : configs) {
			ips.addAll(config.ips);
		}
		return ips;
	}

	/**
	 * Finds the needed app for a {@link DependencyDeclaration}.
	 *
	 * @param dc    the current {@link DependencyConfig}
	 * @param appId the appId of the needed {@link Dependency}
	 * @return s null if the app can not be added; {@link Optional#absent()} if the
	 *         app needs to be created; the {@link OpenemsAppInstance} if an
	 *         existing app can be used
	 */
	private Optional<OpenemsAppInstance> findNeededApp(DependencyConfig dc) {
		if (!dc.isDependency()) {
			return Optional.absent();
		}
		if (dc.appDependencyConfig.specificInstanceId != null) {
			try {
				var appById = this.getAppManagerImpl().findInstaceById(dc.appDependencyConfig.specificInstanceId);
				return Optional.of(appById);
			} catch (NoSuchElementException e) {
				return null;
			}
		}
		var appId = dc.appDependencyConfig.appId;
		if (dc.sub.createPolicy == DependencyDeclaration.CreatePolicy.ALWAYS) {
			var neededApps = this.getAppManagerImpl().getInstantiatedApps().stream().filter(t -> t.appId.equals(appId))
					.collect(Collectors.toList());
			OpenemsAppInstance availableApp = null;
			for (var neededApp : neededApps) {
//				if (!this.getAppManagerImpl().getInstantiatedApps().stream()
//						.filter(t -> t.dependencies != null && !t.dependencies.isEmpty()).anyMatch(t -> t.dependencies
//								.stream().anyMatch(d -> d.instanceId.equals(neededApp.instanceId)))) {
				if (this.getAppsWithDependencyTo(neededApp).isEmpty()) {
					availableApp = neededApp;
					break;
				}
			}
			return Optional.fromNullable(availableApp);
		}
		var neededApp = this.getAppManagerImpl().getInstantiatedApps().stream().filter(t -> t.appId.equals(appId))
				.collect(Collectors.toList());
		if (!neededApp.isEmpty()) {
			return Optional.of(neededApp.get(0));
		}
		if (dc.sub.createPolicy == DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING) {
			return Optional.absent();
		}
		return null;
	}

	private List<OpenemsAppInstance> getAppsWithDependencyTo(OpenemsAppInstance instance) {
		return this.getAppManagerImpl().getInstantiatedApps().stream()
				.filter(t -> t.dependencies != null && !t.dependencies.isEmpty())
				.filter(t -> t.dependencies.stream().anyMatch(d -> d.instanceId.equals(instance.instanceId)))
				.collect(Collectors.toList());
	}

	/**
	 * Recursively iterates over all dependencies and the given app.
	 *
	 * <p>
	 * Order bottom -> top.
	 *
	 * @param app                 the app to be installed
	 * @param alias               the alias of the current instance
	 * @param defaultProperties   the properties of the current instane
	 * @param target              the {@link ConfigurationTarget}
	 * @param function            returns true if the instance gets created or
	 *                            already exists
	 * @param sub                 the {@link DependencyDeclaration}
	 * @param l                   the {@link Language}
	 * @param parent              the parent app
	 * @param alreadyIteratedApps the apps that already got iterated thru to avoid
	 *                            endless loop. e. g. if two apps have each other as
	 *                            a dependency
	 * @return s the last {@link DependencyConfig}
	 * @throws OpenemsNamedException on error
	 */
	private DependencyConfig foreachDependency(OpenemsApp app, AppDependencyConfig appConfig,
			ConfigurationTarget target, Function<DependencyConfig, Boolean> function, DependencyDeclaration sub,
			Language l, OpenemsApp parent, Set<OpenemsApp> alreadyIteratedApps,
			Function<List<AppDependencyConfig>, AppDependencyConfig> determineDependencyConfig)
			throws OpenemsNamedException {
		if (alreadyIteratedApps == null) {
			alreadyIteratedApps = new HashSet<>();
		}
		alreadyIteratedApps.add(app);
		if (appConfig.alias != null) {
			appConfig.properties.addProperty("ALIAS", appConfig.alias);
		}
		var config = app.getAppConfiguration(target, appConfig.properties, l);
		if (appConfig.alias != null) {
			appConfig.properties.remove("ALIAS");
		}
		var dependencies = new LinkedList<DependencyConfig>();
		for (var dependency : config.dependencies) {
			var nextAppConfig = determineDependencyConfig.apply(dependency.appConfigs);
			if (nextAppConfig == null) {
				// can not determine one out of many configs
				continue;
			}
			try {
				OpenemsApp dependencyApp;
				if (nextAppConfig.appId != null) {
					dependencyApp = this.getAppManagerImpl().findAppById(nextAppConfig.appId);
				} else {
					var specificApp = this.getAppManagerImpl().findInstaceById(nextAppConfig.specificInstanceId);
					dependencyApp = this.getAppManagerImpl().findAppById(specificApp.appId);
				}
				if (alreadyIteratedApps.contains(dependencyApp)) {
					continue;
				}
				var addingConfig = this.foreachDependency(dependencyApp, nextAppConfig, target, function, dependency, l,
						app, alreadyIteratedApps, determineDependencyConfig);
				if (addingConfig != null) {
					dependencies.add(addingConfig);
				}
			} catch (NoSuchElementException e) {
				// TODO can not find app
			}
		}

		var newConfig = new DependencyConfig(app, parent, sub, config, appConfig, dependencies);
		if (function.apply(newConfig)) {
			return newConfig;
		}
		return null;
	}

	private DependencyDeclaration.AppDependencyConfig determineDependencyConfig(List<AppDependencyConfig> configs) {
		if (configs == null || configs.isEmpty()) {
			return null;
		}
		if (configs.size() == 1) {
			return configs.get(0);
		}
		// TODO multiple dependency configs
		for (var config : configs) {
			var instances = this.getAppManagerImpl().getInstantiatedApps().stream()
					.filter(i -> i.appId.equals(config.appId)).collect(Collectors.toList());
			for (var instance : instances) {
				var existingDependencies = this.getAppsWithDependencyTo(instance);
				if (existingDependencies.isEmpty()) {
					return config;
				}
			}
		}

		return null;
	}

	private void foreachDependency(OpenemsApp app, String alias, JsonObject defaultProperties,
			ConfigurationTarget target, Language l,
			Function<List<AppDependencyConfig>, AppDependencyConfig> determineDependencyConfig,
			Function<DependencyConfig, Boolean> consumer) throws OpenemsNamedException {
		var appConfig = DependencyDeclaration.AppDependencyConfig.create() //
				.setAppId(app.getAppId()) //
				.setAlias(alias) //
				.setProperties(defaultProperties) //
				.build();
		this.foreachDependency(app, appConfig, target, consumer, null, l, null, null, determineDependencyConfig);
	}

	private void foreachExistingDependecy(OpenemsAppInstance instance, ConfigurationTarget target, Language l,
			Function<ExistingDependencyConfig, Boolean> consumer) throws OpenemsNamedException {
		this.foreachExistingDependency(instance, target, consumer, null, null, l, null);
	}

	/**
	 * Recursively iterates over all existing dependencies and the given app.
	 *
	 * <p>
	 * Order bottom -> top.
	 *
	 * @param instance            the existing {@link OpenemsAppInstance}
	 * @param target              the {@link ConfigurationTarget}
	 * @param consumer            the consumer that gets executed for every instance
	 * @param parent              the parent instance of the current dependency
	 * @param sub                 the {@link DependencyDeclaration}
	 * @param l                   the {@link Language}
	 * @param alreadyIteratedApps the already iterated app to avoid an endless loop
	 * @return s the last {@link DependencyConfig}
	 * @throws OpenemsNamedException on error
	 */
	private DependencyConfig foreachExistingDependency(OpenemsAppInstance instance, ConfigurationTarget target,
			Function<ExistingDependencyConfig, Boolean> consumer, OpenemsAppInstance parent, DependencyDeclaration sub,
			Language l, Set<OpenemsAppInstance> alreadyIteratedApps) throws OpenemsNamedException {
		if (alreadyIteratedApps == null) {
			alreadyIteratedApps = new HashSet<>();
		}
		alreadyIteratedApps.add(instance);
		var app = this.getAppManagerImpl().findAppById(instance.appId);
		instance.properties.addProperty("ALIAS", instance.alias);
		var config = app.getAppConfiguration(target, instance.properties, l);
		instance.properties.remove("ALIAS");

		var dependecies = new ArrayList<DependencyConfig>();
		if (instance.dependencies != null) {
			dependecies = new ArrayList<>(instance.dependencies.size());
			for (var dependency : instance.dependencies) {
				try {
					var dependencyApp = this.getAppManagerImpl().findInstaceById(dependency.instanceId);
					if (alreadyIteratedApps.contains(dependencyApp)) {
						continue;
					}
					var subApp = config.dependencies.stream().filter(t -> t.key.equals(dependency.key)).findFirst()
							.get();
					var dependecy = this.foreachExistingDependency(dependencyApp, target, consumer, instance, subApp, l,
							alreadyIteratedApps);
					dependecies.add(dependecy);
				} catch (NoSuchElementException e) {
					// can not find app
				}
			}
		}
		OpenemsApp parentApp = null;
		if (parent != null) {
			parentApp = this.getAppManagerImpl().findAppById(parent.appId);
		}

		DependencyDeclaration.AppDependencyConfig dependencyAppConfig;
		if (sub == null) {
			dependencyAppConfig = DependencyDeclaration.AppDependencyConfig.create() //
					.setAppId(instance.appId) //
					.setProperties(instance.properties) //
					.setAlias(instance.alias) //
					.build();
		} else {
			dependencyAppConfig = sub.appConfigs.stream().filter(c -> c.appId.equals(instance.appId)).findAny().get();
		}

		var newConfig = new ExistingDependencyConfig(app, parentApp, sub, config, dependencyAppConfig, dependecies,
				parent, instance);
		if (consumer.apply(newConfig)) {
			return newConfig;
		}
		return null;
	}

	/**
	 * Gets the component id s that can be replaced.
	 *
	 * @param app        the components of which app
	 * @param properties the default properties to create an app instance of this
	 *                   app
	 * @return a map of the component id s that can be replaced mapped from id to
	 *         key to put the next id
	 * @throws OpenemsNamedException on error
	 */
	protected final Map<String, String> getReplaceableComponentIds(OpenemsApp app, JsonObject properties)
			throws OpenemsNamedException {
		final var prefix = "?_?_";
		var config = app.getAppConfiguration(ConfigurationTarget.TEST, properties, null);
		var copyBuilder = JsonUtils.buildJsonObject();
		for (var entry : properties.entrySet()) {
			copyBuilder.add(entry.getKey(), entry.getValue());
		}
		for (var comp : config.components) {
			copyBuilder.addProperty(comp.getId(), prefix);
		}
		var copy = copyBuilder.build();
		var configWithNewIds = app.getAppConfiguration(ConfigurationTarget.TEST, copy, null);
		Map<String, String> replaceableComponentIds = new HashMap<>();
		for (var comp : configWithNewIds.components) {
			if (comp.getId().startsWith(prefix)) {
				// "METER_ID:meter0"
				var raw = comp.getId().substring(prefix.length());
				// ["METER_ID", "meter0"]
				var pieces = raw.split(":");
				// "METER_ID"
				var property = pieces[0];
				// "meter0"
				var defaultId = pieces[1];
				replaceableComponentIds.put(defaultId, property);
			}
		}
		return replaceableComponentIds;
	}

	/**
	 * Gets an App Configuration with component id s, which can be used to create or
	 * rewrite the settings of the component.
	 *
	 * @param app                the {@link OpenemsApp}
	 * @param oldAppInstance     the old {@link OpenemsAppInstance}
	 * @param newAppInstance     the new {@link OpenemsAppInstance}
	 * @param otherAppComponents the components that are used from the other
	 *                           {@link OpenemsAppInstance}
	 * @param language           the language of the new config
	 * @return the AppConfiguration with the replaced ID s of the components
	 * @throws OpenemsNamedException on error
	 */
	private AppConfiguration getNewAppConfigWithReplacedIds(OpenemsApp app, OpenemsAppInstance oldAppInstance,
			OpenemsAppInstance newAppInstance, List<EdgeConfig.Component> otherAppComponents, Language language)
			throws OpenemsNamedException {

		var target = oldAppInstance == null ? ConfigurationTarget.ADD : ConfigurationTarget.UPDATE;
		var newAppConfig = app.getAppConfiguration(target, newAppInstance.properties, language);

		final var replacableIds = this.getReplaceableComponentIds(app, newAppInstance.properties);

		for (var comp : ComponentUtilImpl.order(newAppConfig.components)) {
			// replace old id s with new ones
			for (var entry : comp.getProperties().entrySet()) {
				for (var replaceableId : replacableIds.entrySet()) {
					if (entry.getValue().toString().contains(replaceableId.getKey())) {
						var newId = entry.getValue().toString().replace(replaceableId.getKey(),
								newAppInstance.properties.get(replaceableId.getValue()).getAsString());
						newId = newId.replace("\"", "");
						var newValue = JsonUtils.getAsJsonElement(newId);
						comp.getProperties().put(entry.getKey(), newValue);
					}
				}
			}

			var isNewComponent = true;
			var id = comp.getId();
			var canBeReplaced = replacableIds.containsKey(id);
			EdgeConfig.Component foundComponent = null;

			// try to find a component with the necessary settings
			if (canBeReplaced) {
				foundComponent = this.componentUtil.getComponentByConfig(comp);
				if (foundComponent != null) {
					id = foundComponent.getId();
				}
			}
			if (foundComponent == null && oldAppInstance != null && oldAppInstance.properties.has(id.toUpperCase())) {
				id = oldAppInstance.properties.get(id.toUpperCase()).getAsString();
				foundComponent = this.componentManager.getEdgeConfig().getComponent(id).orElse(null);
				final var tempId = id;
				// other app uses the same component because they had the same configuration
				// now this app needs the component with a different configuration so now create
				// a new component
				if (foundComponent != null && otherAppComponents.stream().anyMatch(t -> t.getId().equals(tempId))) {
					foundComponent = null;
				}
			}
			isNewComponent = isNewComponent && foundComponent == null;
			if (isNewComponent) {
				// if the id is not already set and there is no component with the default id
				// then use the default id
				foundComponent = this.componentManager.getEdgeConfig().getComponent(comp.getId()).orElse(null);
				if (foundComponent == null) {
					id = comp.getId();
				} else {
					// replace number at the end and get the next available id
					var nextAvailableId = this.componentUtil.getNextAvailableId(id.replaceAll("\\d+", ""),
							otherAppComponents);
					if (!nextAvailableId.equals(id) && !canBeReplaced) {
						// component can not be created because the id is already used
						// and the id can not be set in the configuration
						continue;
					}
					if (canBeReplaced) {
						id = nextAvailableId;
					}
				}
			}

			if (canBeReplaced) {
				newAppInstance.properties.addProperty(replacableIds.get(comp.getId()), id);
			}
		}
		return app.getAppConfiguration(target, newAppInstance.properties, language);
	}

	private final AppManagerImpl getAppManagerImpl() {
		return (AppManagerImpl) this.componentManager.getEnabledComponentsOfType(AppManager.class).get(0);
	}

}
