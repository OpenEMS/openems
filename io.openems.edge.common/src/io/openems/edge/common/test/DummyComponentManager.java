package io.openems.edge.common.test;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;

/**
 * Simulates a ComponentManager for the OpenEMS Component test framework.
 */
public class DummyComponentManager implements ComponentManager, ComponentJsonApi {

	private final List<OpenemsComponent> components = new ArrayList<>();
	private final Clock clock;
	private JsonObject edgeConfigJson;

	private ConfigurationAdmin configurationAdmin = null;

	public DummyComponentManager() {
		this(Clock.systemDefaultZone());
	}

	public DummyComponentManager(Clock clock) {
		this.clock = clock;
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return Collections.unmodifiableList(this.components);
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return Collections.unmodifiableList(this.components);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz) {
		List<T> result = new ArrayList<>();
		for (OpenemsComponent component : this.components) {
			if (clazz.isInstance(component)) {
				result.add((T) component);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (SINGLETON_COMPONENT_ID.equals(componentId)) {
			return (T) this;
		}
		for (var component : this.getEnabledComponents()) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getPossiblyDisabledComponent(String componentId)
			throws OpenemsNamedException {
		if (SINGLETON_COMPONENT_ID.equals(componentId)) {
			return (T) this;
		}
		for (var component : this.getAllComponents()) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

	/**
	 * Specific for this Dummy implementation.
	 *
	 * @param component the component that should be added
	 * @return this
	 */
	public DummyComponentManager addComponent(OpenemsComponent component) {
		if (component != this) {
			this.components.add(component);
		}
		return this;
	}

	/**
	 * Sets a {@link EdgeConfig} json.
	 *
	 * @param json the {@link EdgeConfig} json
	 */
	public void setConfigJson(JsonObject json) {
		this.edgeConfigJson = json;
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		if (this.edgeConfigJson == null) {
			return EdgeConfig.empty();
		}
		return EdgeConfig.fromJson(this.edgeConfigJson);
	}

	@Override
	public String id() {
		return ComponentManager.SINGLETON_COMPONENT_ID;
	}

	@Override
	public String alias() {
		return ComponentManager.SINGLETON_COMPONENT_ID;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public ComponentContext getComponentContext() {
		return null;
	}

	@Deprecated()
	@Override
	public Channel<?> _channel(String channelName) {
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		return new ArrayList<>();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetEdgeConfigRequest.METHOD, call -> {
			return new GetEdgeConfigResponse(call.getRequest().getId(), this.getEdgeConfig());
		});

		builder.handleRequest(CreateComponentConfigRequest.METHOD, call -> {
			final var request = CreateComponentConfigRequest.from(call.getRequest());
			this.handleCreateComponentConfigRequest(call.get(EdgeKeys.USER_KEY), request);

			return new GenericJsonrpcResponseSuccess(request.getId());
		});

		builder.handleRequest(UpdateComponentConfigRequest.METHOD, call -> {
			final var request = UpdateComponentConfigRequest.from(call.getRequest());
			this.handleUpdateComponentConfigRequest(call.get(EdgeKeys.USER_KEY), request);

			return new GenericJsonrpcResponseSuccess(request.getId());
		});
	}

	@Override
	public void handleCreateComponentConfigRequest(User user, CreateComponentConfigRequest request)
			throws OpenemsNamedException {
		if (this.configurationAdmin == null) {
			throw new OpenemsException("Can not create Component Config. ConfigurationAdmin is null!");
		}
		try {
			var config = this.configurationAdmin.createFactoryConfiguration(request.getFactoryPid(), null);

			// set properties
			for (Property property : request.getProperties()) {
				var value = JsonUtils.getAsBestType(property.getValue());
				if (value instanceof Object[] && ((Object[]) value).length == 0) {
					value = new String[0];
				}
				config.getProperties().put(property.getName(), value);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void handleUpdateComponentConfigRequest(User user, UpdateComponentConfigRequest request)
			throws OpenemsNamedException {
		if (this.configurationAdmin == null) {
			throw new OpenemsException("Can not update Component Config. ConfigurationAdmin is null!");
		}
		try {
			for (var configuration : this.configurationAdmin.listConfigurations(null)) {
				final var props = configuration.getProperties();
				if (props == null) {
					continue;
				}
				if (props.get("id") == null || !props.get("id").equals(request.getComponentId())) {
					continue;
				}
				var properties = new Hashtable<String, JsonElement>();
				for (var property : request.getProperties()) {
					properties.put(property.getName(), property.getValue());
				}
				configuration.update(properties);
			}
		} catch (IOException | InvalidSyntaxException e) {
			throw new OpenemsException("Can not update Component Config.");
		}
	}

	@Override
	public void handleDeleteComponentConfigRequest(User user, DeleteComponentConfigRequest request)
			throws OpenemsNamedException {
		if (this.configurationAdmin == null) {
			throw new OpenemsException("Can not delete Component Config. ConfigurationAdmin is null!");
		}

		try {
			for (var configuration : this.configurationAdmin.listConfigurations(null)) {
				final var props = configuration.getProperties();
				if (props == null) {
					continue;
				}
				if (props.get("id") == null || !props.get("id").equals(request.getComponentId())) {
					continue;
				}
				configuration.delete();
			}
		} catch (IOException | InvalidSyntaxException e) {
			throw new OpenemsException("Can not update Component Config.");
		}
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}

	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

}