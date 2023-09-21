package io.openems.edge.core.appmanager;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.ActualEdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;

public class DummyPseudoComponentManager implements ComponentManager {

	private final Clock clock;
	private ConfigurationAdmin configurationAdmin;
	private final List<OpenemsComponent> components = new Vector<>();

	public DummyPseudoComponentManager() {
		this(Clock.systemDefaultZone());
	}

	public DummyPseudoComponentManager(Clock clock) {
		this.clock = clock;
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

	@Override
	public Channel<?> _channel(String channelName) {
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		return Collections.emptyList();
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return this.components.stream() //
				.filter(OpenemsComponent::isEnabled) //
				.collect(Collectors.toList());
	}

	@Override
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz) {
		return this.getEnabledComponents().stream() //
				.filter(clazz::isInstance) //
				.map(clazz::cast) //
				.collect(Collectors.toList());
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return Collections.unmodifiableList(this.components);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponentOrNull(String componentId) {
		return (T) this.getEnabledComponents().stream() //
				.filter(c -> c.id().equals(componentId)) //
				.findAny() //
				.orElse(null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		return (T) this.getEnabledComponents().stream() //
				.filter(c -> c.id().equals(componentId)) //
				.findAny() //
				.orElseThrow(() -> OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getPossiblyDisabledComponent(String componentId)
			throws OpenemsNamedException {
		return (T) this.components.stream() //
				.filter(c -> c.id().equals(componentId)) //
				.findAny() //
				.orElseThrow(() -> OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId));
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		final var config = ActualEdgeConfig.create();
		for (var component : this.components) {
			final var configParams = new JsonObject();
			final var props = component.getComponentContext().getProperties();
			final var enumeration = props.keys();
			while (enumeration.hasMoreElements()) {
				final var key = enumeration.nextElement();
				final var value = props.get(key);
				configParams.add(key, JsonUtils.getAsJsonElement(value));
			}
			config.addComponent(component.id(), new EdgeConfig.Component(//
					component.id(), //
					component.alias(), //
					component.serviceFactoryPid(), //
					configParams //
			));
		}
		return config.buildEdgeConfig();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(//
			final User user, //
			final JsonrpcRequest request //
	) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);

		switch (request.getMethod()) {

		case CreateComponentConfigRequest.METHOD:
			return this.handleCreateComponentConfigRequest(user, CreateComponentConfigRequest.from(request));
		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));
		case DeleteComponentConfigRequest.METHOD:
			return this.handleDeleteComponentConfigRequest(user, DeleteComponentConfigRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(//
			final User user, //
			final CreateComponentConfigRequest request //
	) throws OpenemsNamedException {

		final var component = componentOf(//
				request.getComponentId(), //
				request.getFactoryPid(), //
				request.getProperties() //
		);

		this.components.add(component);

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(//
			final User user, //
			final UpdateComponentConfigRequest request //
	) throws OpenemsNamedException {
		final var foundComponent = this.getPossiblyDisabledComponent(request.getComponentId());

		if (!(foundComponent instanceof DummyOpenemsComponent)) {
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
				return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
			} catch (IOException | InvalidSyntaxException e) {
				throw new OpenemsException("Can not update Component Config.");
			}
		} else {
			final var component = componentOf(//
					request.getComponentId(), //
					foundComponent.serviceFactoryPid(), //
					request.getProperties() //
			);
			this.components.removeIf(t -> t.id().equals(request.getComponentId()));
			this.components.add(component);
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(//
			final User user, //
			final DeleteComponentConfigRequest request //
	) throws OpenemsNamedException {
		this.components.removeIf(t -> t.id().equals(request.getComponentId()));
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Adds a {@link EdgeConfig.Component}.
	 * 
	 * @param component the component to add
	 */
	public void addComponent(EdgeConfig.Component component) {
		this.components.add(new DummyOpenemsComponent(component));
	}

	/**
	 * Adds a {@link OpenemsComponent}.
	 * 
	 * @param component the component to add
	 */
	public void addComponent(OpenemsComponent component) {
		this.components.add(component);
	}

	private static OpenemsComponent componentOf(//
			String componentId, //
			String factoryId, //
			List<UpdateComponentConfigRequest.Property> properties //
	) {
		final var alias = new AtomicReference<String>("");
		final var props = properties.stream() //
				.filter(prop -> {
					if (prop.getName().equalsIgnoreCase("alias")) {
						alias.set(prop.getValue().getAsString());
						return false;
					}
					return true;
				}) //
				.collect(JsonUtils.toJsonObject(UpdateComponentConfigRequest.Property::getName,
						UpdateComponentConfigRequest.Property::getValue));
		final var component = new EdgeConfig.Component(//
				componentId, //
				alias.get(), //
				factoryId, //
				props //
		);
		return new DummyOpenemsComponent(component);
	}

	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

	private static final class DummyOpenemsComponent implements OpenemsComponent {

		private final EdgeConfig.Component component;

		public DummyOpenemsComponent(Component component) {
			super();
			this.component = component;
		}

		@Override
		public String id() {
			return this.component.getId();
		}

		@Override
		public String alias() {
			return this.component.getAlias();
		}

		@Override
		public boolean isEnabled() {
			final var enabled = JsonUtils.getAsOptionalBoolean(this.component.getProperties().get("enabled"));
			return enabled.orElse(true);
		}

		@Override
		public ComponentContext getComponentContext() {
			final var table = new HashMap<String, JsonElement>(this.component.getProperties());
			table.put("service.factoryPid", new JsonPrimitive(this.component.getFactoryId()));
			return DummyComponentContext.from(table);
		}

		@Override
		public Channel<?> _channel(String channelName) {
			return null;
		}

		@Override
		public Collection<Channel<?>> channels() {
			return Collections.emptyList();
		}

		@Override
		public String toString() {
			return "DummyOpenemsComponent [id=" + this.id() + ", enabled=" + this.isEnabled() + "]";
		}

	}

	private static class DummyComponentContext implements ComponentContext {

		private final Dictionary<String, Object> properties;

		public static final DummyComponentContext from(Map<String, JsonElement> map) {
			return new DummyComponentContext(map.entrySet().stream() //
					.collect(Collectors.toMap(Entry::getKey, //
							t -> {
								final var json = t.getValue();
								try {
									return JsonUtils.getAsBestType(json);
								} catch (OpenemsNamedException e) {
									// unable to parse
									e.printStackTrace();
								}
								return t;
							}, //
							(t, u) -> {
								// duplicates take second
								return u;
							}, //
							Hashtable::new //
					)));
		}

		public DummyComponentContext(Dictionary<String, Object> properties) {
			super();
			this.properties = properties;
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			return this.properties;
		}

		@Override
		public <S> S locateService(String name) {
			return null;
		}

		@Override
		public <S> S locateService(String name, ServiceReference<S> reference) {
			return null;
		}

		@Override
		public Object[] locateServices(String name) {
			return null;
		}

		@Override
		public BundleContext getBundleContext() {
			return null;
		}

		@Override
		public Bundle getUsingBundle() {
			return null;
		}

		@Override
		public <S> ComponentInstance<S> getComponentInstance() {
			return null;
		}

		@Override
		public void enableComponent(String name) {
		}

		@Override
		public void disableComponent(String name) {
		}

		@Override
		public ServiceReference<?> getServiceReference() {
			return null;
		}

	}

}