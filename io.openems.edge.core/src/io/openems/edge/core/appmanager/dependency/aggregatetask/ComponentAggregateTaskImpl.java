package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StreamUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.jsonrpc.GetEstimatedConfiguration;

@Component(//
		service = { //
				AggregateTask.class, //
				ComponentAggregateTask.class, //
				ComponentAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class ComponentAggregateTaskImpl implements ComponentAggregateTask {

	private record ComponentAggregatedExecutionConfiguration(//
			List<ComponentDef> components //
	) implements AggregateTask.AggregateTaskExecutionConfiguration {

		private ComponentAggregatedExecutionConfiguration {
			Objects.requireNonNull(components);
		}

		@Override
		public String identifier() {
			return "Component";
		}

		@Override
		public JsonElement toJson() {
			if (this.components.isEmpty()) {
				return JsonNull.INSTANCE;
			}
			return JsonUtils.buildJsonObject() //
					.add("components", this.components.stream() //
							.map(t -> new GetEstimatedConfiguration.Component(t.factoryId(), t.id(), t.alias(),
									t.properties().values().stream() //
											.collect(JsonUtils.toJsonObject(ComponentProperties.Property::name,
													ComponentProperties.Property::value)))) //
							.map(GetEstimatedConfiguration.Component.serializer()::serialize) //
							.collect(JsonUtils.toJsonArray())) //
					.build();
		}

	}

	private final ComponentManager componentManager;

	private List<ComponentDef> components;
	private List<ComponentDef> components2Delete;

	private List<ComponentDef> createdComponents;
	private List<String> deletedComponents;

	@Activate
	public ComponentAggregateTaskImpl(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public void reset() {
		this.components = new LinkedList<>();
		this.components2Delete = new LinkedList<>();
		this.createdComponents = new LinkedList<>();
		this.deletedComponents = new LinkedList<>();
	}

	@Override
	public void aggregate(ComponentConfiguration config, ComponentConfiguration oldConfig) {
		if (config != null) {
			// remove duplicated components
			// TODO maybe error
			this.components.removeIf(t -> config.components().stream().anyMatch(o -> t.id().equals(o.id())));
			this.components.addAll(config.components());
		}
		if (oldConfig != null) {
			var componentDiff = new ArrayList<>(oldConfig.components());
			if (config != null) {
				componentDiff.removeIf(t -> config.components().stream().anyMatch(c -> {
					return c.id().equals(t.id()) //
							&& c.factoryId().equals(t.factoryId());
				}));
			}
			this.components2Delete.addAll(componentDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}
		var errors = new LinkedList<String>();
		var otherAppComponents = AppConfiguration.getComponentsFromConfigs(otherAppConfigurations);
		// create components
		for (var comp : ComponentUtilImpl.order(this.components)) {
			/**
			 * if comp already exists with same config as needed => use it. if comp exist
			 * with different config and no other app needs it => rewrite settings. if comp
			 * exist with different config and other app needs it => create new comp
			 */
			var foundComponentWithSameId = this.componentManager.getEdgeConfig().getComponent(comp.id()).orElse(null);
			if (foundComponentWithSameId != null) {
				// check if the found component has the same factory id
				if (!foundComponentWithSameId.getFactoryId().equals(comp.factoryId())) {
					if (this.components2Delete.stream().anyMatch(t -> t.id().equals(comp.id()))) {
						// if the component was intended to be deleted anyway delete it directly and
						// create the new component directly afterwards
						try {
							this.deleteComponent(user, comp);
							this.deletedComponents.add(comp.id());
							this.components2Delete.removeIf(t -> t.id().equals(comp.id()));
							this.createComponent(user, comp);
							this.createdComponents.add(comp);
						} catch (OpenemsNamedException e) {
							final var error = "Component[" + comp.factoryId() + "] cant be created!";
							errors.add(error);
							errors.add(e.getMessage());
						}
					} else {
						errors.add("Configuration of component with id '" + foundComponentWithSameId.getId()
								+ "' can not be rewritten. Because the component has a different factoryId.");
					}
					continue;
				}

				var isSameConfigWithoutAlias = ComponentUtilImpl.isSameConfigurationWithoutAlias(null, comp,
						foundComponentWithSameId);
				if (isSameConfigWithoutAlias && comp.alias() == null) {
					// alias == null => no update
					continue;
				}
				var isSameConfig = isSameConfigWithoutAlias && comp.alias().equals(foundComponentWithSameId.getAlias());

				if (isSameConfig) {
					// same configuration so no reconfiguration needed
					continue;
				}

				// check if it is my component
				final var componentFromOtherApp = otherAppComponents.stream() //
						.filter(t -> t.id().equals(comp.id())) //
						.reduce(ComponentAggregateTaskImpl::reduceBasedOnPriority) //
						.orElse(null);

				var componentToUpdate = comp;
				if (componentFromOtherApp != null) {
					try {
						componentToUpdate = reduceBasedOnPriorityThrowing(componentFromOtherApp, comp);
					} catch (OpenemsRuntimeException e) {
						errors.add("Configuration of component with id '" + foundComponentWithSameId.getId()
								+ "' can not be rewritten. Because the component belongs to another app. "
								+ e.getMessage());
						continue;
					}
				}
				try {
					this.reconfigure(user, componentToUpdate, foundComponentWithSameId);
				} catch (OpenemsNamedException e) {
					errors.add(e.getMessage());
				}
				continue;
			}

			// create new component
			try {
				this.createComponent(user, comp);
				this.createdComponents.add(comp);
			} catch (OpenemsNamedException e) {
				var error = "Component[" + comp.factoryId() + "] cant be created!";
				errors.add(error);
				errors.add(e.getMessage());
			}

		}
		try {
			// delete components that were used from the old configurations
			this.delete(user, otherAppConfigurations);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	/**
	 * deletes the given components only if they are not in notMyComponents.
	 *
	 * @param user                   the executing user
	 * @param otherAppConfigurations the other {@link AppConfiguration}s
	 */
	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}
		List<String> errors = new ArrayList<>();
		var notMyComponents = AppConfiguration.getComponentsFromConfigs(otherAppConfigurations);
		for (var comp : this.components2Delete) {
			final var componentFromOtherApp = notMyComponents.stream() //
					.filter(t -> t.id().equals(comp.id())) //
					.reduce(ComponentAggregateTaskImpl::reduceBasedOnPriority) //
					.orElse(null);
			if (componentFromOtherApp != null) {
				// TODO
				this.reconfigure(user, componentFromOtherApp, comp.id());
				continue;
			}
			var component = this.componentManager.getEdgeConfig().getComponent(comp.id()).orElse(null);
			if (component == null) {
				// component does not exist
				continue;
			}

			try {
				this.deleteComponent(user, comp);
				this.deletedComponents.add(comp.id());
			} catch (OpenemsNamedException e) {
				errors.add(e.toString());
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	private static ComponentDef reduceBasedOnPriority(//
			ComponentDef componentDef, //
			ComponentDef componentDef2, //
			Consumer<String> errorResolver //
	) {
		final var properties = Stream
				.concat(componentDef.properties().values().stream(), componentDef2.properties().values().stream())
				.collect(StreamUtils.distinctByKey(ComponentProperties.Property::name, (property1, property2) -> {
					if (property1.priority().isSame(property2.priority())) {
						if (!property1.value().equals(property2.value())) {
							errorResolver.accept("Unable to get value based on priority");
						}
						return property1;
					}
					if (property1.priority().isGreaterThan(property2.priority())) {
						return property1;
					}
					return property2;
				})) //
				.toList();

		return componentDef.withProperties(new ComponentProperties(properties));
	}

	private static ComponentDef reduceBasedOnPriority(ComponentDef componentDef, ComponentDef componentDef2) {
		return reduceBasedOnPriority(componentDef, componentDef2, FunctionUtils::doNothing);
	}

	private static ComponentDef reduceBasedOnPriorityThrowing(ComponentDef componentDef, ComponentDef componentDef2) {
		return reduceBasedOnPriority(componentDef, componentDef2, s -> {
			throw new OpenemsRuntimeException(s);
		});
	}

	@Override
	public AggregateTaskExecutionConfiguration getExecutionConfiguration() {
		return new ComponentAggregatedExecutionConfiguration(this.components);
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateComponents");
	}

	@Override
	public void validate(//
			final List<String> errors, //
			final AppConfiguration appConfiguration, //
			final ComponentConfiguration config, //
			final Map<OpenemsAppInstance, AppConfiguration> allConfigurations //
	) {
		var actualEdgeConfig = this.componentManager.getEdgeConfig();

		final var componentsById = allConfigurations.values().stream() //
				.flatMap(t -> t.getComponents().stream()) //
				.collect(Collectors.groupingBy(ComponentDef::id));

		var missingComponents = new ArrayList<String>();
		for (var expectedComponent : config.components()) {
			var componentId = expectedComponent.id();

			// Get Actual Component Configuration
			EdgeConfig.Component actualComponent;
			try {
				actualComponent = actualEdgeConfig.getComponentOrError(componentId);
			} catch (InvalidValueException e) {
				missingComponents.add(componentId);
				continue;
			}

			final var componentFromOtherInstances = Optional.ofNullable(componentsById.get(componentId)) //
					.flatMap(t -> t.stream().reduce(ComponentAggregateTaskImpl::reduceBasedOnPriority)) //
					.orElse(null);

			if (componentFromOtherInstances != null) {
				ComponentUtilImpl.isSameConfigurationWithoutAlias(errors,
						reduceBasedOnPriority(expectedComponent, componentFromOtherInstances), actualComponent);
				continue;
			}

			// ALIAS should not be validated because it can be different depending on the
			// language
			ComponentUtilImpl.isSameConfigurationWithoutAlias(errors, expectedComponent, actualComponent);
		}

		if (!missingComponents.isEmpty()) {
			errors.add("Missing Component" //
					+ (missingComponents.size() > 1 ? "s" : "") + ":" //
					+ missingComponents.stream().collect(Collectors.joining(",")));
		}
	}

	private final boolean anyChanges() {
		return !this.components.isEmpty() //
				|| !this.components2Delete.isEmpty();
	}

	private void deleteComponent(User user, ComponentDef comp) throws OpenemsNamedException {
		this.componentManager.handleDeleteComponentConfigRequest(user, new DeleteComponentConfig.Request(comp.id()));
	}

	private void createComponent(User user, ComponentDef comp) throws OpenemsNamedException {
		List<Property> properties = comp.properties().values().stream() //
				.map(t -> new Property(t.name(), t.value())) //
				.collect(Collectors.toList());
		properties.add(new Property("id", comp.id()));
		properties.add(new Property("alias", comp.alias()));

		this.componentManager.handleCreateComponentConfigRequest(user,
				new CreateComponentConfig.Request(comp.factoryId(), properties));
	}

	/**
	 * checks if the settings of the component changed if there is a change it
	 * rewrites the settings of the given component.
	 *
	 * @param user       the executing user
	 * @param myComp     the component that configuration should be rewritten
	 * @param actualComp the actual component that exists
	 * @throws OpenemsNamedException when the configuration can not be rewritten
	 */
	private void reconfigure(User user, ComponentDef myComp, EdgeConfig.Component actualComp)
			throws OpenemsNamedException {
		if (ComponentUtilImpl.isSameConfiguration(null, myComp, actualComp)) {
			return;
		}

		this.reconfigure(user, myComp, actualComp.getId());
	}

	private void reconfigure(User user, ComponentDef myComp, String componentId) throws OpenemsNamedException {
		// send update request
		List<Property> properties = myComp.properties().values().stream()//
				.map(t -> new Property(t.name(), t.value())) //
				.collect(Collectors.toList());
		properties.add(new Property("alias", myComp.alias()));

		this.componentManager.handleUpdateComponentConfigRequest(user,
				new UpdateComponentConfig.Request(componentId, properties));
	}

	@Override
	public List<ComponentDef> getCreatedComponents() {
		return Collections.unmodifiableList(this.createdComponents);
	}

	@Override
	public List<String> getDeletedComponents() {
		return Collections.unmodifiableList(this.deletedComponents);
	}

}
