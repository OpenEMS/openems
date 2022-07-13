package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;
import io.openems.edge.core.appmanager.validator.Validator;

@Component
public class AppManagerAppHelperImpl implements AppManagerAppHelper {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile AppManager appManager;

	private final ComponentManager componentManager;
	private final ComponentUtil componentUtil;

	private final Validator validator;

	// tasks
	private final AggregateTask.ComponentAggregateTask componentsTask;
	private final AggregateTask.SchedulerAggregateTask schedulerTask;
	private final AggregateTask.StaticIpAggregateTask staticIpTask;

	private final AggregateTask[] tasks;

	private TemporaryApps temporaryApps;

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
		return this.usingTemporaryApps(user, () -> this.updateAppInternal(user, oldInstance, properties, alias, app));
	}

	@Override
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException {
		return this.usingTemporaryApps(user, () -> this.deleteAppInternal(user, instance));
	}

	private UpdateValues usingTemporaryApps(User user, ThrowingSupplier<UpdateValues, OpenemsNamedException> supplier)
			throws OpenemsNamedException {
		// to make sure the temporaryApps get set to null
		this.resetTasks();
		this.temporaryApps = new TemporaryApps();
		OpenemsNamedException exception = null;
		RuntimeException runtimeException = null;
		UpdateValues result = null;
		try {
			result = supplier.get();
		} catch (OpenemsNamedException e) {
			exception = e;
		} catch (RuntimeException e) {
			runtimeException = e;
		}
		final var tempTemporarayApps = this.temporaryApps;
		this.temporaryApps = null;
		if (exception != null) {
			throw exception;
		}
		if (runtimeException != null) {
			throw runtimeException;
		}

		var ignoreInstances = new ArrayList<OpenemsAppInstance>(tempTemporarayApps.currentlyModifiedApps.size() //
				+ tempTemporarayApps.currentlyDeletingApps.size());
		ignoreInstances.addAll(tempTemporarayApps.currentlyModifiedApps);
		ignoreInstances.addAll(tempTemporarayApps.currentlyDeletingApps);

		var otherAppConfigs = this.getAppManagerImpl()
				.getOtherAppConfigurations(ignoreInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));

		var errors = new LinkedList<String>();
		final var language = user == null ? null : user.getLanguage();
		final var bundle = getTranslationBundle(language);

		try {
			// create or delete unused components
			this.componentsTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
			e.printStackTrace();
			errors.add(TranslationUtil.getTranslation(bundle, "canNotUpdateComponents"));
		}

		try {
			// update scheduler execute order
			this.schedulerTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
			errors.add(TranslationUtil.getTranslation(bundle, "canNotUpdateScheduler"));
		}

		try {
			// update static ips
			this.staticIpTask.create(user, otherAppConfigs);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
			errors.add(TranslationUtil.getTranslation(bundle, "canNotUpdateStaticIps"));
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}

		return result;
	}

	private UpdateValues updateAppInternal(User user, OpenemsAppInstance oldInstance, JsonObject properties,
			String alias, OpenemsApp app) throws OpenemsNamedException {
		if (properties == null) {
			properties = new JsonObject();
		}
		// TODO maybe check for all apps
		// if also checking dependencies these may be inconsistent
		// e. g. install HOME is requested it may have a dependency on a SOCOMEC Meter
		// but the meter has a checkable that there has to be a HOME installed
		// maybe add temporary apps in this component
		final var warnings = new LinkedList<String>();
		final var language = user == null ? null : user.getLanguage();
		final var bundle = getTranslationBundle(language);
		if (oldInstance == null) {
			this.checkStatus(app, language);
			this.temporaryApps.currentlyCreatingApps
					.add(new OpenemsAppInstance(app.getAppId(), null, UUID.randomUUID(), null, null));
		} else {
			// determine if properties are allowed to be updated
			var references = this.getAppsWithReferenceTo(oldInstance.instanceId);
			references.removeAll(this.temporaryApps.currentlyDeletingApps);
			for (var entry : this.getAppManagerImpl().appConfigs(references, null)) {
				for (var dependencieDeclaration : entry.getValue().dependencies) {

					var dd = entry.getKey().dependencies.stream()
							.filter(d -> d.instanceId.equals(oldInstance.instanceId))
							.filter(d -> d.key.equals(dependencieDeclaration.key)).findAny();

					if (dd.isEmpty()) {
						continue;
					}

					var dependencyApp = this.getAppManagerImpl().findInstanceById(dd.get().instanceId);

					var appConfig = this.getAppDependencyConfig(dependencyApp, dependencieDeclaration.appConfigs);

					if (appConfig == null) {
						continue;
					}

					switch (dependencieDeclaration.dependencyUpdatePolicy) {
					case ALLOW_ALL:
						// everything can be changed
						break;
					case ALLOW_NONE:
						throw new OpenemsException(TranslationUtil.getTranslation(bundle, "appNotAllowedToBeUpdated"));
					case ALLOW_ONLY_UNCONFIGURED_PROPERTIES:
						// override properties
						for (var propEntry : appConfig.properties.entrySet()) {
							if (!properties.has(propEntry.getKey())
									|| !properties.get(propEntry.getKey()).equals(propEntry.getValue())) {

								warnings.add(TranslationUtil.getTranslation(bundle, "canNotChangeProperty",
										propEntry.getKey()));

								properties.add(propEntry.getKey(), propEntry.getValue());
							}

						}
						// override alias if set
						if (appConfig.alias != null && !alias.equals(appConfig.alias)) {
							warnings.add(TranslationUtil.getTranslation(bundle, "canNotChangeAlias"));
							alias = appConfig.alias;
						}
						break;
					}
				}
			}
		}

		var errors = new LinkedList<String>();

		var oldInstances = new TreeMap<AppIdKey, ExistingDependencyConfig>();
		var dependencieInstances = new HashMap<DependencyConfig, OpenemsAppInstance>();
		// get all existing app dependencies
		if (oldInstance != null) {
			this.foreachExistingDependency(oldInstance, ConfigurationTarget.UPDATE, language, null, dc -> {
				if (!dc.isDependency()) {
					return true;
				}
				oldInstances.put(new AppIdKey(dc.parentInstance.appId, dc.sub.key), dc);
				return true;
			});
		}

		BiFunction<OpenemsApp, DependencyDeclaration, Boolean> includeDependency = (a, d) -> {
			var oldAppConfig = oldInstances.get(new AppIdKey(a.getAppId(), d.key));

			var isCreating = false;
			var possibleInstance = this.findNeededApp(d, this.determineDependencyConfig(d.appConfigs));
			if (oldAppConfig == null) {
				switch (d.createPolicy) {
				case ALWAYS:
					isCreating = true;
					break;
				case IF_NOT_EXISTING:
					if (!possibleInstance.isPresent()) {
						isCreating = true;
					}
					break;
				case NEVER:
					// isCreating is false
					break;
				}
			}
			if (isCreating) {
				if (d != null || oldInstance == null) {
					var config = this.determineDependencyConfig(d.appConfigs);
					String appId;
					UUID id = null;
					if (config.appId != null) {
						appId = config.appId;
						id = UUID.randomUUID();
					} else {
						var instance = this.getAppManagerImpl().findInstanceById(config.specificInstanceId);
						appId = instance.appId;
						id = instance.instanceId;
					}
					try {
						// check if an instance can be created
						var tempApp = this.getAppManagerImpl().findAppById(config.appId);
						tempApp.getAppConfiguration(ConfigurationTarget.ADD, config.initialProperties, language);
						this.temporaryApps.currentlyCreatingApps
								.add(new OpenemsAppInstance(appId, null, id, null, null));
					} catch (NoSuchElementException | OpenemsNamedException ex) {
						// app not found or config cant be get
					}
				}
			}
			return true;
		};
		final var lastCreatedOrModifiedApp = new MutableValue<OpenemsAppInstance>();
		// update app and its dependencies
		this.foreachDependency(errors, app, alias, properties, ConfigurationTarget.UPDATE, language,
				this::determineDependencyConfig, includeDependency, dc -> {
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
								this.log.error(e.getMessage());
								errors.add(TranslationUtil.getTranslation(bundle, "canNotGetAppConfiguration"));
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

					// create app or get as dependency
					if (oldAppConfig == null) {
						var neededApp = this.findNeededApp(dc.sub, dc.appDependencyConfig);
						if (neededApp == null) {
							return false;
						}
						AppConfiguration oldConfig = null;
						UUID instanceId;
						OpenemsAppInstance oldInstanceOfCurrentApp = null;
						JsonObject propertiesOfNewInstance;
						var aliasOfNewInstance = dc.appDependencyConfig.alias;
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
									this.log.error(e.getMessage());
									errors.add(TranslationUtil.getTranslation(bundle, "canNotGetAppConfiguration"));
								}
							}
							propertiesOfNewInstance = dc.appDependencyConfig.properties;
						} else {
							var existing = this.temporaryApps.currentlyCreatingApps.stream()
									.filter(t -> t.appId.equals(dc.app.getAppId())) //
									.filter(t -> t.properties == null).findFirst();
							instanceId = existing.get().instanceId;
							propertiesOfNewInstance = dc.appDependencyConfig.initialProperties;
							// use app name as default alias if not given
							if (aliasOfNewInstance == null) {
								aliasOfNewInstance = dc.app.getName(language);
							}

							// check if the created app can satisfy another app dependency
							final var fallBackAlwaysCreateApp = new MutableValue<OpenemsAppInstance>();

							var apps2UpdateDependency = this.getAppManagerImpl().getInstantiatedApps().stream() //
									.filter(i -> {
										var neededDependency = this.getNeededDependencyTo(i, dc.app.getAppId());
										if (neededDependency == null) {
											return false;
										}
										if (neededDependency.createPolicy == DependencyDeclaration.CreatePolicy.ALWAYS) {
											// only set the dependency to one app which has the always create policy
											fallBackAlwaysCreateApp.setValue(i);
											return false;
										}
										return true;
									}) //
									.collect(Collectors.toList());

							if (apps2UpdateDependency.isEmpty() && fallBackAlwaysCreateApp.getValue() != null) {
								apps2UpdateDependency.add(fallBackAlwaysCreateApp.getValue());
							}

							for (var instance : apps2UpdateDependency) {
								var neededDependency = this.getNeededDependencyTo(instance, dc.app.getAppId());
								// override properties if set by dependency
								if (neededDependency.dependencyUpdatePolicy != DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL) {
									var config = this.determineDependencyConfig(neededDependency.appConfigs);
									for (var entry : config.properties.entrySet()) {
										if (!dc.appDependencyConfig.properties.has(entry.getKey())
												|| !dc.appDependencyConfig.properties.get(entry.getKey())
														.equals(entry.getValue())) {
											warnings.add(TranslationUtil.getTranslation(bundle, "overrideProperty",
													entry.getKey()));
										}
										dc.appDependencyConfig.properties.add(entry.getKey(), entry.getValue());
									}
								}

								// update dependencies
								var modifiedOrCreatedApps = this.temporaryApps.currentlyCreatingApps;
								var alreadyModifiedAppIndex = modifiedOrCreatedApps.indexOf(instance);
								var replaceApp = instance;
								if (alreadyModifiedAppIndex != -1) {
									replaceApp = modifiedOrCreatedApps.get(alreadyModifiedAppIndex);
								} else {
									modifiedOrCreatedApps = this.temporaryApps.currentlyModifiedApps;
									alreadyModifiedAppIndex = modifiedOrCreatedApps.indexOf(instance);
									replaceApp = instance;
									if (alreadyModifiedAppIndex != -1) {
										replaceApp = modifiedOrCreatedApps.get(alreadyModifiedAppIndex);
									}
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
								propertiesOfNewInstance, dependecies);
						lastCreatedOrModifiedApp.setValue(newAppInstance);
						if (neededApp.isPresent()) {
							this.temporaryApps.currentlyModifiedApps.removeIf(t -> t.equals(newAppInstance));
							this.temporaryApps.currentlyModifiedApps.add(newAppInstance);
						} else {
							this.temporaryApps.currentlyCreatingApps.removeIf(t -> t.equals(newAppInstance));
							this.temporaryApps.currentlyCreatingApps.add(newAppInstance);
						}

						dependencieInstances.put(dc, newAppInstance);
						try {
							var otherAppConfigs = this.getAppManagerImpl()
									.getOtherAppConfigurations(newAppInstance.instanceId);

							// add configurations from currently creating apps
							for (var config : this.getAppManagerImpl().appConfigs(
									this.temporaryApps.currentlyCreatingModifiedApps(),
									AppManagerImpl.exludingInstanceIds(newAppInstance.instanceId))) {
								otherAppConfigs.add(config.getValue());
							}

							var newConfig = this.getNewAppConfigWithReplacedIds(dc.app, oldInstanceOfCurrentApp,
									newAppInstance, AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigs),
									language);

							this.aggregateAllTasks(newConfig, oldConfig);
						} catch (OpenemsNamedException e) {
							this.log.error(e.getMessage());
							errors.add(TranslationUtil.getTranslation(bundle, "canNotGetAppConfiguration"));
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
									break;
								}
							}
						}
					}

					// update existing app
					var isNotAllowedToUpdate = dc.isDependency()
							&& !dc.sub.updatePolicy.isAllowedToUpdate(this.getAppManagerImpl().getInstantiatedApps(),
									parent, oldAppConfig.instance);

					var newInstanceAlias = dc.appDependencyConfig.alias;
					if (newInstanceAlias == null) {
						newInstanceAlias = oldAppConfig.instance.alias;
					}

					OpenemsAppInstance newAppInstance;

					if (isNotAllowedToUpdate) {
						newAppInstance = oldAppConfig.instance;
					} else {
						newAppInstance = new OpenemsAppInstance(dc.app.getAppId(), newInstanceAlias,
								oldAppConfig.instance.instanceId, dc.appDependencyConfig.properties, dependecies);
					}

					lastCreatedOrModifiedApp.setValue(newAppInstance);
					dependencieInstances.put(dc, newAppInstance);

					if (isNotAllowedToUpdate) {
						// not allowed to update but still a dependency
						return true;
					}
					this.temporaryApps.currentlyModifiedApps.removeIf(t -> t.equals(newAppInstance));
					this.temporaryApps.currentlyModifiedApps.add(newAppInstance);

					try {
						var otherAppConfigs = this.getAppManagerImpl()
								.getOtherAppConfigurations(newAppInstance.instanceId);

						// add configurations from currently creating apps
						for (var config : this.getAppManagerImpl().appConfigs(
								this.temporaryApps.currentlyCreatingModifiedApps(),
								AppManagerImpl.exludingInstanceIds(newAppInstance.instanceId))) {
							otherAppConfigs.add(config.getValue());
						}

						var newAppConfig = this.getNewAppConfigWithReplacedIds(dc.app, oldAppConfig.instance,
								newAppInstance, AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigs),
								language);

						this.aggregateAllTasks(newAppConfig, oldAppConfig.config);

					} catch (OpenemsNamedException e) {
						this.log.error(e.getMessage());
						errors.add(TranslationUtil.getTranslation(bundle, "canNotGetAppConfiguration"));
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
			this.temporaryApps.currentlyDeletingApps.add(dc.instance);
		}

		// remove temporary instances that couldn't be created
		this.temporaryApps.currentlyCreatingApps.removeIf(t -> t.properties == null);
		this.temporaryApps.currentlyModifiedApps.removeIf(t -> t.properties == null);

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}

		return new UpdateValues(lastCreatedOrModifiedApp.getValue(), this.temporaryApps.currentlyCreatingModifiedApps(),
				this.temporaryApps.currentlyDeletingApps, warnings);
	}

	private DependencyDeclaration.AppDependencyConfig getAppDependencyConfig(OpenemsAppInstance instance,
			List<DependencyDeclaration.AppDependencyConfig> appDependencyConfigs) {
		for (var config : appDependencyConfigs) {
			if (config.appId != null && config.appId.equals(instance.appId)
					|| config.specificInstanceId.equals(instance.instanceId)) {
				return config;
			}
		}
		return null;
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
			return this.value;
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
				if (neededDependency.appConfigs.stream().filter(c -> c.appId != null)
						.anyMatch(c -> c.appId.equals(appId))
						|| neededDependency.appConfigs.stream().filter(c -> c.specificInstanceId != null)
								.anyMatch(c -> c.specificInstanceId.equals(instance.instanceId))) {
					return neededDependency;
				}

			}
		} catch (OpenemsNamedException e) {
			// can not get app configuration
		}
		return null;
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

	private UpdateValues deleteAppInternal(User user, OpenemsAppInstance instance) throws OpenemsNamedException {

		final var language = user == null ? null : user.getLanguage();
		final var bundle = getTranslationBundle(language);
		// check if the app is allowed to be delete
		if (!this.isAllowedToDelete(instance)) {
			throw new OpenemsException(TranslationUtil.getTranslation(bundle, "appNotAllowedToBeDeleted"));
		}

		BiFunction<OpenemsAppInstance, OpenemsAppInstance, Boolean> includeInstance = (p, i) -> {
			if (p != null) {
				// check if the parent should delete it
				try {
					var app = this.getAppManagerImpl().findAppById(p.appId);
					var config = app.getAppConfiguration(ConfigurationTarget.DELETE, p.properties, null);

					var dependency = p.dependencies.stream().filter(de -> de.instanceId.equals(i.instanceId))
							.findFirst();

					if (dependency.isEmpty()) {
						return false;
					}

					var dependencyDeclaration = config.dependencies.stream()
							.filter(dd -> dd.key.equals(dependency.get().key)).findFirst();

					if (dependencyDeclaration.isEmpty()) {
						return false;
					}

					switch (dependencyDeclaration.get().deletePolicy) {
					case NEVER:
						return false;
					case IF_MINE:
						var referencedApps = this.getAppsWithReferenceTo(i.instanceId);
						for (var referencedInstance : referencedApps) {
							if (!referencedInstance.equals(p)) {
								return false;
							}
						}
						break;
					case ALWAYS:
						break;
					}

				} catch (OpenemsNamedException | NoSuchElementException e) {
					// TODO: handle exception
					return false;
				}
			}
			this.temporaryApps.currentlyDeletingApps.add(i);
			return true;
		};

		this.foreachExistingDependency(instance, ConfigurationTarget.DELETE, language, includeInstance, dc -> {
			// check if dependency is allowed to be deleted by its parent
			if (dc.isDependency()) {
				var deleteApp = true;
				switch (dc.sub.deletePolicy) {
				case IF_MINE:
					if (!this.getAppManagerImpl().getInstantiatedApps().stream()
							.anyMatch(a -> !a.equals(dc.parentInstance) && a.dependencies != null && a.dependencies
									.stream().anyMatch(d -> d.instanceId.equals(dc.instance.instanceId)))) {
						break;
					}
					deleteApp = false;
					break;
				case NEVER:
					deleteApp = false;
					break;
				case ALWAYS:
					break;
				}
				if (!deleteApp) {
					// update for enabling ReadOnly after deleting ReadWrite
					if (dc.sub.updatePolicy != DependencyDeclaration.UpdatePolicy.ALWAYS) {
						return false;
					}

					var copy = dc.instance.properties.deepCopy();
					// override properties
					for (var entry : dc.appDependencyConfig.properties.entrySet()) {
						copy.add(entry.getKey(), entry.getValue());
					}

					try {
						this.updateAppInternal(user, dc.instance, copy,
								dc.appDependencyConfig.alias != null ? dc.appDependencyConfig.alias : dc.instance.alias,
								dc.app);

					} catch (OpenemsNamedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return false;
				}
			}

			this.temporaryApps.currentlyDeletingApps.add(dc.instance);

			this.aggregateAllTasks(null, dc.config);

			return true;
		});

		var unmodifiedApps = this
				.getAppsWithReferenceTo(
						this.temporaryApps.currentlyDeletingApps.stream().map(t -> t.instanceId).toArray(UUID[]::new))
				.stream().filter(a -> !this.temporaryApps.currentlyDeletingApps.stream().anyMatch(t -> t.equals(a)))
				.collect(Collectors.toList());

		for (var app : unmodifiedApps) {
			var dependencies = new ArrayList<>(app.dependencies);
			dependencies.removeIf(d -> this.temporaryApps.currentlyDeletingApps.stream()
					.anyMatch(i -> i.instanceId.equals(d.instanceId)));
			this.temporaryApps.currentlyModifiedApps.removeIf(t -> t.instanceId.equals(app.instanceId));
			this.temporaryApps.currentlyModifiedApps.add(new OpenemsAppInstance(app.appId, //
					app.alias, app.instanceId, app.properties, dependencies));
		}

		return new UpdateValues(instance, this.temporaryApps.currentlyCreatingModifiedApps(),
				this.temporaryApps.currentlyDeletingApps);
	}

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

	protected void checkStatus(OpenemsApp openemsApp, Language language) throws OpenemsNamedException {
		var validatorConfig = openemsApp.getValidatorConfig();
		var status = this.validator.getStatus(validatorConfig);
		switch (status) {
		case INCOMPATIBLE:
			throw new OpenemsException("App is not compatible! " + this.validator
					.getErrorCompatibleMessages(validatorConfig, language).stream().collect(Collectors.joining(";")));
		case COMPATIBLE:
			throw new OpenemsException("App can not be installed! " + this.validator
					.getErrorInstallableMessages(validatorConfig, language).stream().collect(Collectors.joining(";")));
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
	 * @param declaration the current {@link DependencyConfig}
	 * @param config      the current
	 *                    {@link DependencyDeclaration.AppDependencyConfig}
	 * @return s null if the app can not be added; {@link Optional#absent()} if the
	 *         app needs to be created; the {@link OpenemsAppInstance} if an
	 *         existing app can be used
	 */
	private Optional<OpenemsAppInstance> findNeededApp(DependencyDeclaration declaration,
			DependencyDeclaration.AppDependencyConfig config) {
		if (declaration == null) {
			return Optional.absent();
		}
		if (config.specificInstanceId != null) {
			try {
				var appById = this.getAppManagerImpl().findInstanceById(config.specificInstanceId);
				return Optional.of(appById);
			} catch (NoSuchElementException e) {
				return null;
			}
		}
		var appId = config.appId;
		if (declaration.createPolicy == DependencyDeclaration.CreatePolicy.ALWAYS) {
			var neededApps = this.getAppManagerImpl().getInstantiatedApps().stream().filter(t -> t.appId.equals(appId))
					.collect(Collectors.toList());
			OpenemsAppInstance availableApp = null;
			for (var neededApp : neededApps) {
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
		if (declaration.createPolicy == DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING) {
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
	 * @param errors                    the errors that occur during the call
	 * @param app                       the app to be installed
	 * @param appConfig                 the {@link AppDependencyConfig} of the
	 *                                  current app
	 * @param target                    the {@link ConfigurationTarget}
	 * @param function                  returns true if the instance gets created or
	 *                                  already exists
	 * @param sub                       the {@link DependencyDeclaration}
	 * @param l                         the {@link Language}
	 * @param parent                    the parent app
	 * @param alreadyIteratedApps       the apps that already got iterated thru to
	 *                                  avoid endless loop. e. g. if two apps have
	 *                                  each other as a dependency
	 * @param determineDependencyConfig the function to determine the
	 *                                  {@link AppDependencyConfig}
	 * @param includeDependency         a {@link BiFunction} to determine if a
	 *                                  dependency should get included
	 * @return s the last {@link DependencyConfig}
	 * @throws OpenemsNamedException on error
	 */
	private DependencyConfig foreachDependency(List<String> errors, OpenemsApp app, AppDependencyConfig appConfig,
			ConfigurationTarget target, Function<DependencyConfig, Boolean> function, DependencyDeclaration sub,
			Language l, OpenemsApp parent, Set<OpenemsApp> alreadyIteratedApps,
			Function<List<AppDependencyConfig>, AppDependencyConfig> determineDependencyConfig,
			BiFunction<OpenemsApp, DependencyDeclaration, Boolean> includeDependency) throws OpenemsNamedException {
		if (alreadyIteratedApps == null) {
			alreadyIteratedApps = new HashSet<>();
		}
		alreadyIteratedApps.add(app);
		if (appConfig.alias != null) {
			appConfig.properties.addProperty("ALIAS", appConfig.alias);
		}
		AppConfiguration config = null;
		try {
			config = app.getAppConfiguration(target, appConfig.initialProperties, l);
		} catch (OpenemsNamedException e) {
			// can not get config of app
			this.log.error(e.getMessage());
			errors.add(TranslationUtil.getTranslation(getTranslationBundle(l), "canNotGetAppConfigurationOfApp",
					app.getName(l)));
		}
		if (appConfig.alias != null) {
			appConfig.properties.remove("ALIAS");
		}
		if (config == null) {
			return null;
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
					var specificApp = this.getAppManagerImpl().findInstanceById(nextAppConfig.specificInstanceId);
					dependencyApp = this.getAppManagerImpl().findAppById(specificApp.appId);
					// fill up properties of existing app to make sure the appConfig can be get
					specificApp.properties.entrySet().forEach(entry -> {
						if (nextAppConfig.properties.has(entry.getKey())) {
							return;
						}
						nextAppConfig.properties.add(entry.getKey(), entry.getValue());
					});
				}

				if (/* TODO alreadyIteratedApps.contains(dependencyApp) || */!includeDependency.apply(app,
						dependency)) {
					continue;
				}

				var addingConfig = this.foreachDependency(errors, dependencyApp, nextAppConfig, target, function,
						dependency, l, app, alreadyIteratedApps, determineDependencyConfig, includeDependency);
				if (addingConfig != null) {
					dependencies.add(addingConfig);
				}
			} catch (NoSuchElementException e) {
				// can not find app
				e.printStackTrace();
			}
		}

		var newConfig = new DependencyConfig(app, parent, sub, config, appConfig, dependencies);
		if (function.apply(newConfig)) {
			return newConfig;
		}
		return null;
	}

	private void foreachDependency(List<String> errors, OpenemsApp app, String alias, JsonObject defaultProperties,
			ConfigurationTarget target, Language l,
			Function<List<AppDependencyConfig>, AppDependencyConfig> determineDependencyConfig,
			BiFunction<OpenemsApp, DependencyDeclaration, Boolean> includeDependency,
			Function<DependencyConfig, Boolean> consumer) throws OpenemsNamedException {
		var appConfig = DependencyDeclaration.AppDependencyConfig.create() //
				.setAppId(app.getAppId()) //
				.setAlias(alias) //
				.setProperties(defaultProperties) //
				.build();
		this.foreachDependency(errors, app, appConfig, target, consumer, null, l, null, null, determineDependencyConfig,
				includeDependency);
	}

	private DependencyDeclaration.AppDependencyConfig determineDependencyConfig(List<AppDependencyConfig> configs) {
		if (configs == null || configs.isEmpty()) {
			return null;
		}
		if (configs.size() == 1) {
			return configs.get(0);
		}

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

		return configs.get(0);
	}

	private void foreachExistingDependency(OpenemsAppInstance instance, ConfigurationTarget target, Language l,
			BiFunction<OpenemsAppInstance, OpenemsAppInstance, Boolean> includeInstance,
			Function<ExistingDependencyConfig, Boolean> consumer) throws OpenemsNamedException {
		this.foreachExistingDependency(instance, target, consumer, null, null, l, null, includeInstance);
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
	 * @param includeInstance     parent, instance, if the instance should get
	 *                            included
	 * @return s the last {@link DependencyConfig}
	 * @throws OpenemsNamedException on error
	 */
	private DependencyConfig foreachExistingDependency(OpenemsAppInstance instance, ConfigurationTarget target,
			Function<ExistingDependencyConfig, Boolean> consumer, OpenemsAppInstance parent, DependencyDeclaration sub,
			Language l, Set<OpenemsAppInstance> alreadyIteratedApps,
			BiFunction<OpenemsAppInstance, OpenemsAppInstance, Boolean> includeInstance) throws OpenemsNamedException {
		if (alreadyIteratedApps == null) {
			alreadyIteratedApps = new HashSet<>();
		}
		alreadyIteratedApps.add(instance);
		var app = this.getAppManagerImpl().findAppById(instance.appId);
		instance.properties.addProperty("ALIAS", instance.alias);
		var config = app.getAppConfiguration(target, instance.properties, l);
		instance.properties.remove("ALIAS");

		var dependecies = new ArrayList<DependencyConfig>();
		if (includeInstance == null || includeInstance.apply(parent, instance)) {

			if (instance.dependencies != null) {
				dependecies = new ArrayList<>(instance.dependencies.size());
				for (var dependency : instance.dependencies) {
					try {
						var dependencyApp = this.getAppManagerImpl().findInstanceById(dependency.instanceId);
						if (alreadyIteratedApps.contains(dependencyApp)) {
							continue;
						}
						var subApp = config.dependencies.stream().filter(t -> t.key.equals(dependency.key)).findFirst()
								.get();
						var dependencyConfig = this.foreachExistingDependency(dependencyApp, target, consumer, instance,
								subApp, l, alreadyIteratedApps, includeInstance);
						if (dependencyConfig != null) {
							dependecies.add(dependencyConfig);
						}
					} catch (NoSuchElementException e) {
						// can not find app
					}
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
			dependencyAppConfig = this.getAppDependencyConfig(instance, sub.appConfigs);
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
		var copy = properties.deepCopy();

		// remove already set ids
		for (var component : config.components) {
			String removeKey = null;
			for (var entry : copy.entrySet()) {
				var id = JsonUtils.getAsOptionalString(entry.getValue()).orElse(null);
				if (id != null && component.getId().startsWith(id)) {
					removeKey = entry.getKey();
					break;
				}
			}
			if (removeKey != null) {
				copy.remove(removeKey);
			}
		}

		config = app.getAppConfiguration(ConfigurationTarget.TEST, copy, null);

		for (var comp : config.components) {
			copy.addProperty(comp.getId(), prefix);
		}
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
			// has to be at first place to make sure no unnecessary components are created
			if (canBeReplaced) {
				// TODO include currently creating components
				foundComponent = this.componentUtil.getComponentByConfig(comp);
				if (foundComponent != null) {
					id = foundComponent.getId();
				}
			}

			// use component based on the last configuration
			if (foundComponent == null && oldAppInstance != null && canBeReplaced
					&& oldAppInstance.properties.has(replacableIds.get(id))) {
				id = oldAppInstance.properties.get(replacableIds.get(id)).getAsString();
				foundComponent = this.componentManager.getEdgeConfig().getComponent(id).orElse(null);
				final var tempId = id;
				// other app uses the same component because they had the same configuration
				// now this app needs the component with a different configuration so now create
				// a new component
				if (foundComponent != null && (!foundComponent.getFactoryId().equals(comp.getFactoryId())
						|| otherAppComponents.stream().anyMatch(t -> t.getId().equals(tempId)))) {
					foundComponent = null;
				}
			}

			isNewComponent = isNewComponent && foundComponent == null;
			if (isNewComponent) {
				// if the id is not already set and there is no component with the default id
				// then use the default id
				foundComponent = this.componentManager.getEdgeConfig().getComponent(comp.getId()).orElse(null);
				if (foundComponent == null) {
					// find component for currently creating apps
					for (var entry : this.getAppManagerImpl().appConfigs(
							this.temporaryApps.currentlyCreatingModifiedApps(),
							AppManagerImpl.exludingInstanceIds(newAppInstance.instanceId))) {
						foundComponent = entry.getValue().components.stream()
								.filter(t -> t.getId().equals(comp.getId())).findFirst().orElse(null);
						if (foundComponent != null) {
							break;
						}

					}
				}
				if (foundComponent == null) {
					id = comp.getId();
				} else {
					// replace number at the end and get the next available id
					var baseName = id.replaceAll("\\d+", "");
					var startingNumber = Integer.parseInt(id.replace(baseName, ""));
					var nextAvailableId = this.componentUtil.getNextAvailableId(baseName, startingNumber,
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

		if (newAppInstance.alias != null) {
			newAppInstance.properties.addProperty("ALIAS", newAppInstance.alias);
		}
		var result = app.getAppConfiguration(target, newAppInstance.properties, language);
		if (newAppInstance.alias != null) {
			newAppInstance.properties.remove("ALIAS");
		}
		return result;
	}

	private final AppManagerImpl getAppManagerImpl() {
		var appManagerImpl = this.appManager;
		if (appManagerImpl == null) {
			appManagerImpl = this.componentManager.getEnabledComponentsOfType(AppManager.class).get(0);
		}
		return (AppManagerImpl) appManagerImpl;
	}

	private static ResourceBundle getTranslationBundle(Language language) {
		if (language == null) {
			language = Language.DEFAULT;
		}
		// TODO translation
		switch (language) {
		case CZ:
		case ES:
		case FR:
		case NL:
			language = Language.EN;
			break;
		case DE:
		case EN:
			break;
		}

		return ResourceBundle.getBundle("io.openems.edge.core.appmanager.dependency.translation", language.getLocal());
	}

	@Override
	public TemporaryApps getTemporaryApps() {
		return this.temporaryApps;
	}

}
