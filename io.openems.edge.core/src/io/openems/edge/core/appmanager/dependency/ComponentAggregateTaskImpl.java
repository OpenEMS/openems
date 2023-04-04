package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;

@Component
public class ComponentAggregateTaskImpl implements AggregateTask, AggregateTask.ComponentAggregateTask {

	private final ComponentManager componentManager;

	private List<EdgeConfig.Component> components;
	private List<EdgeConfig.Component> components2Delete;

	private List<EdgeConfig.Component> createdComponents;
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
	public void aggregate(AppConfiguration config, AppConfiguration oldConfig) {
		if (config != null) {
			// remove duplicated components
			// TODO maybe error
			this.components.removeIf(t -> config.components.stream().anyMatch(o -> t.getId().equals(o.getId())));
			this.components.addAll(config.components);
		}
		if (oldConfig != null) {
			var componentDiff = new ArrayList<>(oldConfig.components);
			if (config != null) {
				componentDiff.removeIf(t -> config.components.stream().anyMatch(c -> c.getId().equals(t.getId())));
			}
			this.components2Delete.addAll(componentDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		var errors = new LinkedList<String>();
		var otherAppComponents = AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigurations);
		// create components
		for (var comp : ComponentUtilImpl.order(this.components)) {
			/**
			 * if comp already exists with same config as needed => use it. if comp exist
			 * with different config and no other app needs it => rewrite settings. if comp
			 * exist with different config and other app needs it => create new comp
			 */
			var foundComponentWithSameId = this.componentManager.getEdgeConfig().getComponent(comp.getId())
					.orElse(null);
			if (foundComponentWithSameId != null) {
				// check if the found component has the same factory id
				if (!foundComponentWithSameId.getFactoryId().equals(comp.getFactoryId())) {
					errors.add("Configuration of component with id '" + foundComponentWithSameId.getId()
							+ "' can not be rewritten. Because the component has a different factoryId.");
					continue;
				}

				var isSameConfigWithoutAlias = ComponentUtilImpl.isSameConfigurationWithoutAlias(null, comp,
						foundComponentWithSameId);
				if (isSameConfigWithoutAlias && comp.getAlias() == null) {
					// alias == null => no update
					continue;
				}
				var isSameConfig = isSameConfigWithoutAlias
						&& comp.getAlias().equals(foundComponentWithSameId.getAlias());

				if (isSameConfig) {
					// same configuration so no reconfiguration needed
					continue;
				}

				// check if it is my component
				if (otherAppComponents.stream().anyMatch(t -> t.getId().equals(foundComponentWithSameId.getId()))) {
					// not my component but only the alias changed
					if (isSameConfigWithoutAlias) {
						// TODO maybe warning if the alias can't be set
						continue;
					}
					errors.add("Configuration of component with id '" + foundComponentWithSameId.getId()
							+ "' can not be rewritten. Because the component belongs to another app.");
					continue;
				}
				try {
					this.reconfigure(user, comp, foundComponentWithSameId);
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
				var error = "Component[" + comp.getFactoryId() + "] cant be created!";
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
		List<String> errors = new ArrayList<>();
		var notMyComponents = AppManagerAppHelperImpl.getComponentsFromConfigs(otherAppConfigurations);
		for (var comp : this.components2Delete) {
			if (notMyComponents.stream().anyMatch(t -> t.getId().equals(comp.getId()))) {
				continue;
			}
			var component = this.componentManager.getEdgeConfig().getComponent(comp.getId()).orElse(null);
			if (component == null) {
				// component does not exist
				continue;
			}

			try {
				// user can be null using internal method
				((ComponentManagerImpl) this.componentManager).handleDeleteComponentConfigRequest(user,
						new DeleteComponentConfigRequest(comp.getId()));
				this.deletedComponents.add(comp.getId());
			} catch (OpenemsNamedException e) {
				errors.add(e.toString());
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	private void createComponent(User user, EdgeConfig.Component comp) throws OpenemsNamedException {
		List<Property> properties = comp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())).collect(Collectors.toList());
		properties.add(new Property("id", comp.getId()));
		properties.add(new Property("alias", comp.getAlias()));

		var request = new CreateComponentConfigRequest(comp.getFactoryId(), properties);
		if (user != null) {
			this.componentManager.handleJsonrpcRequest(user, request);
			return;
		}
		// user can be null using internal method
		((ComponentManagerImpl) this.componentManager).handleCreateComponentConfigRequest(user, request);
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
	private void reconfigure(User user, EdgeConfig.Component myComp, EdgeConfig.Component actualComp)
			throws OpenemsNamedException {
		if (ComponentUtilImpl.isSameConfiguration(null, myComp, actualComp)) {
			return;
		}

		// send update request
		List<Property> properties = myComp.getProperties().entrySet().stream()
				.map(t -> new Property(t.getKey(), t.getValue())) //
				.collect(Collectors.toList());
		properties.add(new Property("alias", myComp.getAlias()));
		var updateRequest = new UpdateComponentConfigRequest(actualComp.getId(), properties);

		if (user != null) {
			this.componentManager.handleJsonrpcRequest(user, updateRequest);
			return;
		}
		// user can be null using internal method
		((ComponentManagerImpl) this.componentManager).handleUpdateComponentConfigRequest(user, updateRequest);
	}

	@Override
	public List<EdgeConfig.Component> getCreatedComponents() {
		return Collections.unmodifiableList(this.createdComponents);
	}

	@Override
	public List<String> getDeletedComponents() {
		return Collections.unmodifiableList(this.deletedComponents);
	}

}
