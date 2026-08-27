package io.openems.edge.core.appmanager;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
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
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.ActualEdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StreamUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;

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
	public void handleCreateComponentConfigRequest(//
			final User user, //
			final CreateComponentConfig.Request request //
	) throws OpenemsNamedException {

		final var component = componentOf(//
				request.getComponentId(), //
				request.factoryPid(), //
				request.properties() //
		);

		this.components.add(component);
	}

	@Override
	public void handleUpdateComponentConfigRequest(//
			final User user, //
			final UpdateComponentConfig.Request request //
	) throws OpenemsNamedException {
		final var foundComponent = this.getPossiblyDisabledComponent(request.componentId());

		if (foundComponent instanceof DummyOpenemsComponent) {
			final var fullProps = new ArrayList<>(this.removeComponentsAndGetProperties(request));
			fullProps.addAll(request.properties());

			final var component = componentOf(//
					request.componentId(), //
					foundComponent.serviceFactoryPid(), //
					fullProps //
			);
			this.components.add(component);
		}
		if (this.configurationAdmin == null) {
			throw new OpenemsException("Can not update Component Config. ConfigurationAdmin is null!");
		}
		try {
			for (var configuration : this.configurationAdmin.listConfigurations(null)) {
				final var props = configuration.getProperties();
				if (props == null) {
					continue;
				}
				if (props.get("id") == null || !props.get("id").equals(request.componentId())) {
					continue;
				}
				var properties = new Hashtable<String, JsonElement>();
				for (var property : request.properties()) {
					properties.put(property.name(), property.value());
				}
				configuration.update(properties);
			}
		} catch (IOException | InvalidSyntaxException e) {
			throw new OpenemsException("Can not update Component Config.");
		}
	}

	@Override
	public void handleDeleteComponentConfigRequest(//
			final User user, //
			final DeleteComponentConfig.Request request //
	) throws OpenemsNamedException {
		this.components.removeIf(t -> t.id().equals(request.componentId()));
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

	/**
	 * Adds a {@link EdgeConfig.Component} from a {@link ComponentDef}.
	 * 
	 * @param component the component to add
	 */
	public void addComponentFromComponentConfig(ComponentDef component) {
		this.addComponent(component.toEdgeConfigComponent());
	}

	private List<UpdateComponentConfigRequest.Property> removeComponentsAndGetProperties(//
			UpdateComponentConfig.Request request //
	) {
		final var componentsToDelete = this.components.stream() //
				.filter(t -> t.id().equals(request.componentId())) //
				.toList();
		this.components.removeAll(componentsToDelete);

		return componentsToDelete.stream() //
				.map(t -> t instanceof DummyOpenemsComponent(EdgeConfig.Component c) ? c.getProperties()
						: Collections.<String, JsonElement>emptyMap()) //
				.reduce(new HashMap<>(), (hashMap, stringJsonElementMap) -> {
					hashMap.putAll(stringJsonElementMap);
					return hashMap;
				}).entrySet().stream() //
				.filter(p -> request.properties().stream().noneMatch(rp -> rp.name().equals(p.getKey()))) //
				.map(p -> new UpdateComponentConfigRequest.Property(p.getKey(), p.getValue())) //
				.toList();
	}

	private static OpenemsComponent componentOf(//
			String componentId, //
			String factoryId, //
			List<UpdateComponentConfigRequest.Property> properties //
	) {
		final var alias = new AtomicReference<String>("");
		final var props = properties.stream() //
				.filter(prop -> {
					if (prop.name().equalsIgnoreCase("alias")) {
						alias.set(prop.value().getAsString());
						return false;
					}
					return true;
				}) //
				.collect(JsonUtils.toJsonObject(UpdateComponentConfigRequest.Property::name,
						UpdateComponentConfigRequest.Property::value));
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

	private record DummyOpenemsComponent(EdgeConfig.Component component) implements OpenemsComponent {

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

	/**
	 * Updates the configuration of the internal <code>_host</code> component. This
	 * method removes any existing <code>_host</code> component and recreates it
	 * with the provided network and USB configuration values.
	 *
	 * @param newNetworkConfig the serialized network configuration JSON string to
	 *                         apply to the <code>_host</code> component
	 */
	public void updateHostConfiguration(String newNetworkConfig) {
		this.components.removeIf(c -> c.id().equals("_host"));
		var newProperties = JsonUtils.buildJsonObject()//
				.addProperty("networkConfiguration", newNetworkConfig)//
				.addProperty("usbConfiguration", "")//
				.build();
		var newHostComponent = new EdgeConfig.Component(//
				"_host", //
				"Core Host", //
				"Core.Host", //
				newProperties//
		);
		this.components.add(new DummyOpenemsComponent(newHostComponent));
	}

	@Override
	public Map<String, Object> getComponentProperties(String componentId) {
		try {
			var dic = this.getComponent(componentId).getComponentContext().getProperties();
			return StreamUtils.dictionaryToStream(dic) //
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		} catch (OpenemsNamedException e) {
			return Collections.emptyMap();
		}
	}

}
