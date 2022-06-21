package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.base.Function;
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

@Component
public class AppManagerAppHelperImpl implements AppManagerAppHelper {

	private ComponentManager componentManager;
	private ComponentUtil componentUtil;

	// tasks
	private ComponentAggregateTask componentsTask;
	private SchedulerAggregateTask schedulerTask;

	@Activate
	public AppManagerAppHelperImpl(@Reference ComponentManager componentManager, @Reference ComponentUtil componentUtil,
			@Reference(target = "(component.name=AppManager.AggregateTask.CreateComponents)") AggregateTask componentsTask,
			@Reference(target = "(component.name=AppManager.AggregateTask.SchedulerAggregateTask)") AggregateTask schedulerTask) {
		this.componentManager = componentManager;
		this.componentUtil = componentUtil;
		this.componentsTask = (ComponentAggregateTask) componentsTask;
		this.schedulerTask = (SchedulerAggregateTask) schedulerTask;

	}

	@Override
	public List<OpenemsAppInstance> installApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException {
		final var language = user == null ? null : user.getLanguage();

		var createdInstances = new LinkedList<OpenemsAppInstance>();
		var dependencieInstances = new HashMap<DependencyConfig, OpenemsAppInstance>();
		this.foreachDependency(app, alias, properties, ConfigurationTarget.ADD, language, dc -> {

			var appId = dc.app.getAppId();
			var neededApp = this.findNeededApp(dc, appId);
			if (neededApp != null) {
				if (neededApp.isPresent()) {
					// TODO update app
					dependencieInstances.put(dc, neededApp.get());
					return true;
				}
			} else {
				return false;
			}

			// TODO what if the created app is a dependency needed from another app?

			// map dependencies if this is the parent
			var dependecies = new ArrayList<Dependency>(dependencieInstances.size());
			if (!dependencieInstances.isEmpty()) {
				var isParent = !dc.isDependency();
				for (var dependency : dependencieInstances.entrySet()) {
					if (!isParent
							&& !dc.config.dependencies.stream().anyMatch(t -> t.equals(dependency.getKey().sub))) {
						isParent = false;
						break;
					} else {
						isParent = true;
					}
					dependecies.add(new Dependency(dependency.getKey().sub.key, dependency.getValue().instanceId));
				}
				if (isParent) {
					dependencieInstances.clear();
				}
			}

			var instance = new OpenemsAppInstance(dc.app.getAppId(), dc.alias, UUID.randomUUID(), dc.properties,
					dependecies);
			createdInstances.add(instance);
			dependencieInstances.put(dc, instance);

			var otherAppComponents = this.getAppManagerImpl()
					.getOtherAppComponents(createdInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));
			try {
				var newAppConfig = this.getNewAppConfigWithReplacedIds(dc.app, null, instance, otherAppComponents,
						language);
				this.componentsTask.aggregate(newAppConfig, null);
				this.schedulerTask.aggregate(newAppConfig, null);
			} catch (OpenemsNamedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		});

		var otherAppComponents = this.getAppManagerImpl()
				.getOtherAppComponents(createdInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));

		this.componentsTask.create(user, otherAppComponents);
		
		this.schedulerTask.setCreatedComponents(this.componentsTask.getCreatedComponents());
		this.schedulerTask.create(user, otherAppComponents);

		return createdInstances;
	}

	@Override
	public List<OpenemsAppInstance> updateApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OpenemsAppInstance> deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException {
		var deletedInstances = new LinkedList<OpenemsAppInstance>();
		final var language = user == null ? null : user.getLanguage();

		this.foreachExistingDependecy(instance, ConfigurationTarget.DELETE, language, dc -> {

			if (dc.isDependency()) {
				switch (dc.sub.deletePolicy) {
				case NEVER:
					return false;
				case IF_MINE:
					if (this.getAppManagerImpl().getInstantiatedApps().stream().anyMatch(a -> !a.equals(dc.parent)
							&& a.dependencies != null
							&& a.dependencies.stream().anyMatch(d -> d.instanceId.equals(dc.instance.instanceId)))) {
						return false;
					}
					break;
				case ALWAYS:
					break;
				}
			}

			deletedInstances.add(dc.instance);

			try {
				componentsTask.aggregate(dc.config, null);
			} catch (OpenemsNamedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		});

		var otherAppComponents = this.getAppManagerImpl()
				.getOtherAppComponents(deletedInstances.stream().map(t -> t.instanceId).toArray(UUID[]::new));

		componentsTask.delete(user, otherAppComponents);

		return deletedInstances;
	}

	/**
	 * 
	 * @param dc
	 * @param appId
	 * @returns null if the app can not be added; {@link Optional#absent()} if the
	 *          app needs to be created; the {@link OpenemsAppInstance} if an
	 *          existing app can be used
	 */
	private Optional<OpenemsAppInstance> findNeededApp(DependencyConfig dc, String appId) {
		if (!dc.isDependency()) {
			return Optional.absent();
		}
		if (dc.sub.createPolicy == DependencyDeclaration.CreatePolicy.ALWAYS) {
			var neededApps = this.getAppManagerImpl().getInstantiatedApps().stream().filter(t -> t.appId.equals(appId))
					.toList();
			OpenemsAppInstance availableApp = null;
			for (var neededApp : neededApps) {
				if (!this.getAppManagerImpl().getInstantiatedApps().stream().anyMatch(
						t -> t.dependencies.stream().anyMatch(d -> d.instanceId.equals(neededApp.instanceId)))) {
					availableApp = neededApp;
					break;
				}
			}
			return Optional.fromNullable(availableApp);
		} else {
			var neededApp = this.getAppManagerImpl().getInstantiatedApps().stream().filter(t -> t.appId.equals(appId))
					.toList();
			if (!neededApp.isEmpty()) {
				return Optional.of(neededApp.get(0));
			}
			if (dc.sub.createPolicy == DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING) {
				return Optional.absent();
			}
		}
		return null;
	}

	/**
	 * Iterates over all dependencies and the given app.
	 * 
	 * <p>
	 * Order bottom -> top.
	 * 
	 * @param app
	 * @param defaultProperties
	 * @param target
	 * @param function          returns true if the instance gets created or already
	 *                          exists
	 * @throws OpenemsNamedException
	 */
	private DependencyConfig foreachDependency(OpenemsApp app, String alias, JsonObject defaultProperties,
			ConfigurationTarget target, Function<DependencyConfig, Boolean> function, DependencyDeclaration sub,
			Language l) throws OpenemsNamedException {
		var config = app.getAppConfiguration(target, defaultProperties, l);
		var dependencies = new LinkedList<DependencyConfig>();
		for (var dependency : config.dependencies) {
			try {
				var dependencyApp = this.getAppManagerImpl().findAppById(dependency.appId);
				var addingConfig = this.foreachDependency(dependencyApp, dependency.alias, dependency.properties,
						target, function, dependency, l);
				if (addingConfig != null) {
					dependencies.add(addingConfig);
				}
			} catch (NoSuchElementException e) {
				// TODO can not find app
			}
		}

		var newConfig = new DependencyConfig(app, sub, config, alias, defaultProperties, dependencies);
		if (function.apply(newConfig)) {
			return newConfig;
		}
		return null;
	}

	private void foreachDependency(OpenemsApp app, String alias, JsonObject defaultProperties,
			ConfigurationTarget target, Language l, Function<DependencyConfig, Boolean> consumer)
			throws OpenemsNamedException {
		this.foreachDependency(app, alias, defaultProperties, target, consumer, null, l);
	}

	private void foreachExistingDependecy(OpenemsAppInstance instance, ConfigurationTarget target, Language l,
			Function<ExistingDependencyConfig, Boolean> consumer) throws OpenemsNamedException {
		this.foreachExistingDependency(instance, target, consumer, null, null, l);
	}

	/**
	 * 
	 * <p>
	 * Order bottom -> top.
	 * 
	 * @param instance
	 * @param target
	 * @param consumer
	 * @param parent
	 * @param sub
	 * @param l
	 * @throws OpenemsNamedException
	 */
	private DependencyConfig foreachExistingDependency(OpenemsAppInstance instance, ConfigurationTarget target,
			Function<ExistingDependencyConfig, Boolean> consumer, OpenemsAppInstance parent, DependencyDeclaration sub,
			Language l) throws OpenemsNamedException {
		var app = this.getAppManagerImpl().findAppById(instance.appId);
		var config = app.getAppConfiguration(target, instance.properties, l);

		var dependecies = new ArrayList<DependencyConfig>();
		if (instance.dependencies != null) {
			dependecies = new ArrayList<DependencyConfig>(instance.dependencies.size());
			for (var dependency : instance.dependencies) {
				try {
					var dependecyApp = this.getAppManagerImpl().findInstaceById(dependency.instanceId);
					var subApp = config.dependencies.stream().filter(t -> t.key.equals(dependency.key)).findFirst()
							.get();
					var dependecy = this.foreachExistingDependency(dependecyApp, target, consumer, instance, subApp, l);
					dependecies.add(dependecy);
				} catch (NoSuchElementException e) {
					// can not find app
				}
			}
		}
		var newConfig = new ExistingDependencyConfig(app, sub, config, instance.alias, instance.properties, dependecies,
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
//		return (AppManagerImpl) appManager;
		return (AppManagerImpl) componentManager.getEnabledComponentsOfType(AppManager.class).get(0);
	}

}
