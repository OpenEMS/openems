package io.openems.edge.core.appmanager;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;
import io.openems.edge.core.host.NetworkInterface;
import io.openems.edge.core.host.jsonrpc.SetNetworkConfigRequest;
import io.openems.edge.io.api.DigitalOutput;

@org.osgi.service.component.annotations.Component()
public class ComponentUtilImpl implements ComponentUtil {

	private final ComponentManager componentManager;
	private final ConfigurationAdmin cm;

	@Activate
	public ComponentUtilImpl(@Reference ComponentManager componentManager, @Reference ConfigurationAdmin cm) {
		this.componentManager = componentManager;
		this.cm = cm;
	}

	/**
	 * Validates if the 'actual' matches the 'expected' value.
	 *
	 * @param expected the expected value
	 * @param actual   the actual value
	 * @return true if they match
	 */
	public static boolean equals(JsonElement expected, JsonElement actual) {
		if (Objects.equals(expected, actual)) {
			return true;
		}

		// both are not null
		if (expected == null || actual == null || !expected.isJsonPrimitive() || !actual.isJsonPrimitive()) {
			return false;
		}

		// both are JsonPrimitives
		var e = expected.getAsJsonPrimitive();
		var a = actual.getAsJsonPrimitive();

		if (e.getAsString().equals(a.getAsString())) {
			// compare 'toString'
			return true;
		}
		return false;
	}

	/**
	 * Validates if the 'interfaces' matches the 'otherInterfaces'.
	 *
	 * @param interfaces      the first interfaces
	 * @param otherInterfaces the second interfaces
	 * @return true if they match
	 */
	public static boolean equals(List<NetworkInterface<?>> interfaces, List<NetworkInterface<?>> otherInterfaces) {
		if (otherInterfaces.size() != interfaces.size()) {
			return false;
		}
		for (NetworkInterface<?> networkInterface : otherInterfaces) {
			var netinterface = interfaces.stream().filter(t -> t.getName().equals(networkInterface.getName()))
					.findFirst().orElse(null);
			if (netinterface == null || netinterface.getAddresses().getValue().size() //
					!= networkInterface.getAddresses().getValue().size() || !netinterface.getAddresses().getValue()
							.stream().allMatch(t -> networkInterface.getAddresses().getValue().contains(t))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * parses all interfaces from the given {@linkplain JsonObject}.
	 *
	 * @param json the {@linkplain JsonObject} that contains the interfaces
	 * @return the parsed interfaces
	 * @throws OpenemsNamedException on error
	 */
	public static List<NetworkInterface<?>> getInterfaces(JsonObject json) throws OpenemsNamedException {
		List<NetworkInterface<?>> interfaces = new ArrayList<>();
		for (var entry : json.entrySet()) {
			try {
				var ni = NetworkInterface.from(entry.getKey(), entry.getValue().getAsJsonObject());
				interfaces.add(ni);
			} catch (OpenemsNamedException e) {
				throw e;
			}
		}
		return interfaces;
	}

	@Override
	public List<NetworkInterface<?>> getInterfaces() throws OpenemsNamedException {
		var hostConfig = this.componentManager.getEdgeConfig().getComponent(Host.SINGLETON_COMPONENT_ID).get();
		var config = hostConfig.getProperty("networkConfiguration").get().getAsJsonObject();
		var interfaces = config.get("interfaces").getAsJsonObject();
		return getInterfaces(interfaces);
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent.
	 *
	 * @param errors            list if something does not match
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @return true if the configurations are the same
	 */
	public static boolean isSameConfiguration(List<String> errors, Component expectedComponent,
			Component actualComponent) {
		return isSameConfiguration(errors, expectedComponent, actualComponent, true, true);
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent.
	 *
	 * @param errors            list if something does not match
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @param includeAlias      if the alias should be checked
	 * @param includeId         if the Component-ID should be checked
	 * @return true if the configurations are the same
	 */
	private static boolean isSameConfiguration(List<String> errors, Component expectedComponent,
			Component actualComponent, boolean includeAlias, boolean includeId) {
		if (errors == null) {
			// if the caller doesn't want errors use the fast way.
			return isSameConfigurationFast(expectedComponent, actualComponent, includeAlias, includeId);
		}

		var componentErrors = new ArrayList<String>();

		if (includeAlias && !expectedComponent.getAlias().equals(actualComponent.getAlias())) {
			componentErrors.add("Alias: " //
					+ "expected '" + expectedComponent.getAlias() + "', " //
					+ "got '" + actualComponent.getAlias() + "'");
		}

		// Validate the Component Factory (i.e. is the Component of the correct type)
		if (!Objects.equals(expectedComponent.getFactoryId(), actualComponent.getFactoryId())) {
			componentErrors.add("Factory-ID: " //
					+ "expected '" + expectedComponent.getFactoryId() + "', " //
					+ "got '" + actualComponent.getFactoryId() + "'");
		}

		for (Entry<String, JsonElement> entry : expectedComponent.getProperties().entrySet()) {
			var key = entry.getKey();
			var expectedProperty = entry.getValue();
			JsonElement actualProperty;
			try {
				actualProperty = actualComponent.getPropertyOrError(key);
			} catch (InvalidValueException e) {
				componentErrors.add("Property '" + key + "': " //
						+ "expected '" + expectedProperty.toString() + "', " //
						+ "but property does not exist");
				continue;
			}

			if (!equals(expectedProperty, actualProperty)) {
				componentErrors.add("Property '" + key + "': " //
						+ "expected '" + expectedProperty.toString() + "', " //
						+ "got '" + actualProperty.toString() + "'");
			}
		}

		if (includeId && !expectedComponent.getId().equals(actualComponent.getId())) {
			componentErrors.add("Id: " //
					+ "expected '" + expectedComponent.getId() + "', " //
					+ "got '" + actualComponent.getId() + "'");
		}

		if (!componentErrors.isEmpty()) {
			errors.add(expectedComponent.getId() + ": " //
					+ componentErrors.stream().collect(Collectors.joining("; ")));
			return false;
		}
		return true;
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent. Returns on
	 * the first error.
	 *
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @param includeAlias      if the alias should be checked
	 * @param includeId         if the Component-ID should be checked
	 * @return true if the configurations are the same
	 */
	private static boolean isSameConfigurationFast(Component expectedComponent, Component actualComponent,
			boolean includeAlias, boolean includeId) {

		if (includeId && !expectedComponent.getId().equals(actualComponent.getId())
				|| includeAlias && !expectedComponent.getAlias().equals(actualComponent.getAlias())) {
			return false;
		}

		// Validate the Component Factory (i.e. is the Component of the correct type)
		if (!Objects.equals(expectedComponent.getFactoryId(), actualComponent.getFactoryId())) {
			return false;
		}

		for (Entry<String, JsonElement> entry : expectedComponent.getProperties().entrySet()) {
			var key = entry.getKey();
			var expectedProperty = entry.getValue();
			JsonElement actualProperty;
			try {
				actualProperty = actualComponent.getPropertyOrError(key);
			} catch (InvalidValueException e) {
				return false;
			}

			if (!ComponentUtilImpl.equals(expectedProperty, actualProperty)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent without
	 * checking the alias.
	 *
	 * @param errors            list if something does not match
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @return true if the configurations are the same
	 */
	public static boolean isSameConfigurationWithoutAlias(List<String> errors, Component expectedComponent,
			Component actualComponent) {
		return isSameConfiguration(errors, expectedComponent, actualComponent, false, true);
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent without
	 * checking the Component-ID.
	 *
	 * @param errors            list if something does not match
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @return true if the configurations are the same
	 */
	public static boolean isSameConfigurationWithoutId(List<String> errors, Component expectedComponent,
			Component actualComponent) {
		return isSameConfiguration(errors, expectedComponent, actualComponent, true, false);
	}

	/**
	 * Checks if the expectedComonents match with the actualComponent without
	 * checking the Component-ID and the alias.
	 *
	 * @param errors            list if something does not match
	 * @param expectedComponent the expected component
	 * @param actualComponent   the actual existing component
	 * @return true if the configurations are the same
	 */
	public static boolean isSameConfigurationWithoutIdAndAlias(List<String> errors, Component expectedComponent,
			Component actualComponent) {
		return isSameConfiguration(errors, expectedComponent, actualComponent, false, false);
	}

	/**
	 * orders the list so components that need another component are coming after
	 * the needed component.
	 *
	 * @param components the component list
	 * @return an ordered copy of the list
	 */
	public static List<Component> order(List<Component> components) {
		var copy = new ArrayList<>(components);
		if (components.size() <= 1) {
			return copy;
		}
		for (Component component : components) {
			// determine which id s the component needs
			List<String> ids = new ArrayList<>();
			for (Component comp : components) {
				for (var entry : component.getProperties().entrySet()) {
					if (entry.getValue().toString().contains(comp.getId())) {
						ids.add(comp.getId());
						break;
					}
				}
			}

			if (ids.isEmpty()) {
				continue;
			}

			var maxIndex = copy.indexOf(component);
			copy.remove(component);
			var minIndex = 0;
			var count = 0;
			// determine minIndex to insert the component
			for (Component comp : copy) {
				if (ids.contains(comp.getId())) {
					ids.remove(comp.getId());
					minIndex = count;
					if (ids.isEmpty()) {
						break;
					}
				}
				count++;
			}
			copy.add(Math.max(minIndex + 1, maxIndex), component);
			// copy values that were already inserted below the component
			// with it
			if (maxIndex < minIndex) {
				for (var i = minIndex + 1; i > maxIndex; i--) {
					// switch places
					var temp = copy.get(i);
					copy.set(i, copy.set(maxIndex, temp));
				}
			}
		}
		return copy;
	}

	private List<OpenemsComponent> getComponentUsing(String value, List<String> ignoreIds) {
		return this.componentManager.getAllComponents().stream() //
				.filter(t -> !ignoreIds.stream().anyMatch(id -> t.id().equals(id))) //
				.filter(c -> { //
					var t = c.getComponentContext().getProperties();
					return enumerationAsStream(t.keys()).anyMatch(key -> {
						var element = t.get(key);
						if (element instanceof Object[] array) {
							for (var arrayElement : array) {
								if (arrayElement.toString().contains(value)) {
									return true;
								}
							}
						}
						return element.toString().contains(value);
					});
				}).toList();
	}

	@Override
	public boolean anyComponentUses(String value, List<String> ignoreIds) {
		return this.componentManager.getAllComponents().stream() //
				.filter(t -> !ignoreIds.stream().anyMatch(id -> t.id().equals(id))) //
				.anyMatch(c -> { //
					var t = c.getComponentContext().getProperties();
					return enumerationAsStream(t.keys()).anyMatch(key -> {
						var element = t.get(key).toString();
						return element.contains(value);
					});
				});
	}

	@Override
	public List<RelayInfo> getAllRelayInfos(//
			List<String> ignoreIds, //
			Predicate<DigitalOutput> componentFilter, //
			Function<DigitalOutput, String> componentAliasMapper, //
			BiPredicate<DigitalOutput, BooleanWriteChannel> channelFilter, //
			BiFunction<DigitalOutput, BooleanWriteChannel, String> channelAliasMapper, //
			BiFunction<DigitalOutput, BooleanWriteChannel, List<String>> disabledReasons //
	) {
		return this.getEnabledComponentsOfType(DigitalOutput.class).stream() //
				.filter(componentFilter) //
				.map(digitalOutput -> {
					final var outputChannels = digitalOutput.digitalOutputChannels();
					return new RelayInfo(digitalOutput.id(), componentAliasMapper.apply(digitalOutput),
							digitalOutput.digitalOutputChannels().length, //
							IntStream.range(0, outputChannels.length) //
									.filter(i -> channelFilter.test(digitalOutput, outputChannels[i])) //
									.mapToObj(i -> {
										final var outputChannel = outputChannels[i];
										return new RelayContactInfo(//
												outputChannel.address().toString(), //
												channelAliasMapper.apply(digitalOutput, outputChannel), //
												i, //
												this.getComponentUsing(outputChannel.address().toString(), ignoreIds), //
												disabledReasons.apply(digitalOutput, outputChannel));
									}) //
									.toList());
				}) //
				.sorted((o1, o2) -> o1.id().compareTo(o2.id())) //
				.toList();
	}

	@Override
	public Component getComponentByConfig(Component component) {
		for (var comp : this.componentManager.getEdgeConfig().getComponentsByFactory(component.getFactoryId())) {
			if (ComponentUtilImpl.isSameConfigurationWithoutIdAndAlias(null, component, comp)) {
				return comp;
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfStartingId(String id) {
		List<T> result = new ArrayList<>();
		for (OpenemsComponent component : this.componentManager.getAllComponents()) {
			if (component.id().startsWith(id)) {
				result.add((T) component);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz) {
		List<T> result = new ArrayList<>();
		for (OpenemsComponent component : this.componentManager.getAllComponents()) {
			if (clazz.isInstance(component)) {
				result.add((T) component);
			}
		}
		return result;
	}

	@Override
	public String getNextAvailableId(String baseName, int startingNumber, List<String> componentIds) {
		for (var i = startingNumber; true; i++) {
			var id = baseName + i;
			if (this.componentManager.getEdgeConfig().getComponent(id).isPresent()) {
				continue;
			}
			if (componentIds.stream().anyMatch(t -> t.equals(id))) {
				continue;
			}
			return id;
		}
	}

	@Override
	public String[] getPreferredRelays(//
			final List<RelayInfo> relayInfos, //
			final int numberOfRelays, //
			final List<PreferredRelay> preferredRelaysDefinitions //
	) {
		String[] fallBackInARowRelays = null;
		var fallBackFirstAvailableRelays = new String[numberOfRelays];
		var firstAvailableNextIndex = 0;
		for (var relayInfo : relayInfos) {
			final var relays = preferredRelaysDefinitions.stream() //
					.filter(t -> t.matchesRelay().test(relayInfo)) //
					.findAny()//
					.map(PreferredRelay::preferredRelays) //
					.orElse(null); //
			var containsAllPreferredRelays = true && relays != null;
			var preferredRelays = new String[numberOfRelays];
			var count = 0;
			if (relays != null) {
				for (var number : relays) {
					if (number < 0) {
						containsAllPreferredRelays = false;
						break;
					}
					if (number > relayInfo.numberOfChannels()) {
						containsAllPreferredRelays = false;
						break;
					}
					final var channel = relayInfo.channels().stream() //
							.filter(r -> r.position() == number - 1) //
							.findFirst().orElse(null);
					if (channel == null //
							|| !channel.usingComponents().isEmpty()//
							|| !channel.disabledReasons().isEmpty()) {
						containsAllPreferredRelays = false;
						break;
					}
					preferredRelays[count++] = channel.channel();
				}
			}
			final var availableChannels = relayInfo.channels().stream() //
					.filter(t -> t.usingComponents().isEmpty()) //
					.filter(t -> t.disabledReasons().isEmpty()) //
					.toList();
			for (var availableChannel : availableChannels) {
				if (firstAvailableNextIndex >= numberOfRelays) {
					break;
				}
				fallBackFirstAvailableRelays[firstAvailableNextIndex++] = availableChannel.channel();
			}
			if (containsAllPreferredRelays) {
				return preferredRelays;
			}
			if (fallBackInARowRelays == null) {
				count = 0;
				var startIndex = 1;
				for (var channelInfo : relayInfo.channels()) {
					if (!channelInfo.usingComponents().isEmpty() //
							|| !channelInfo.disabledReasons().isEmpty()) {
						startIndex += count;
						count = 1;
					}
					if (count >= numberOfRelays) {
						break;
					}
				}
				if (count >= numberOfRelays) {
					fallBackInARowRelays = new String[numberOfRelays];
					for (var i = 0; i < fallBackInARowRelays.length; i++) {
						fallBackInARowRelays[i] = relayInfo.channels().get(startIndex + i).channel();
					}
				}

			}
		}
		return fallBackInARowRelays != null ? fallBackInARowRelays : fallBackFirstAvailableRelays;
	}

	@Override
	public void updateInterfaces(User user, List<NetworkInterface<?>> interfaces) throws OpenemsNamedException {
		JsonApi host = this.componentManager.getComponent(Host.SINGLETON_COMPONENT_ID);
		host.handleJsonrpcRequest(user, new SetNetworkConfigRequest(interfaces));

		// wait until its updated
		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!ComponentUtilImpl.equals(interfaces, this.getInterfaces()));

	}

	@Override
	public synchronized void updateScheduler(User user, List<String> schedulerExecutionOrder,
			List<EdgeConfig.Component> components) throws OpenemsNamedException {
		if (schedulerExecutionOrder == null || schedulerExecutionOrder.isEmpty()) {
			return;
		}

		schedulerExecutionOrder = this.removeIdsWhichNotExist(schedulerExecutionOrder, components);
		if (schedulerExecutionOrder.isEmpty()) {
			return;
		}

		// get current order
		var controllerIds = this.getSchedulerIds();

		// remove existing id s in the scheduler and insert them in the right place
		controllerIds.removeAll(schedulerExecutionOrder);

		// update order
		this.setSchedulerComponentIds(user, this.insertSchedulerOrder(controllerIds, schedulerExecutionOrder));
	}

	@Override
	public void removeIdsInSchedulerIfExisting(User user, List<String> removedIds) throws OpenemsNamedException {
		if (removedIds == null || removedIds.isEmpty()) {
			return;
		}
		var controllerIds = this.getSchedulerIds();

		controllerIds.removeAll(removedIds);

		this.setSchedulerComponentIds(user, controllerIds);
	}

	private void setSchedulerComponentIds(User user, List<String> componentIds) throws OpenemsNamedException {
		try {
			var scheduler = this.getScheduler();
			// null is necessary otherwise a new configuration gets created
			var config = this.cm.getConfiguration(scheduler.getPid(), null);

			var properties = config.getProperties();
			if (properties == null) {
				// No configuration existing yet -> create new configuration
				properties = new Hashtable<>();
			} else {
				// configuration exists -> update configuration
			}

			var existingIds = JsonUtils
					.getAsJsonArray(scheduler.getProperty("controllers.ids").orElse(new JsonArray()));
			// check if the ids in the scheduler are the exact same as given
			if (existingIds.size() == componentIds.size()) {
				Set<String> newIds = new HashSet<>(componentIds);
				for (var item : existingIds) {
					newIds.remove(JsonUtils.getAsString(item));
				}
				if (newIds.isEmpty()) {
					return;
				}
			}

			var ids = componentIds.stream().toArray(String[]::new);
			properties.put("controllers.ids", ids);

			if (user != null) {
				properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, user.getId() + ": " + user.getName());
			}
			properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
					LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());

			config.update(properties);
		} catch (IOException e) {
			throw new OpenemsException("Could not update Scheduler!");
		}
	}

	@Override
	public List<String> getSchedulerIds() throws OpenemsNamedException {
		try {
			final var config = this.cm.getConfiguration(this.getScheduler().getPid(), null);
			final var properties = config.getProperties();
			if (properties == null) {
				return emptyList();
			}
			final var ids = properties.get("controllers.ids");
			return ids == null ? emptyList() : Lists.newArrayList((String[]) ids);
		} catch (IOException e) {
			throw new OpenemsException("Unable to get Scheduler configuration!", e);
		}
	}

	@Override
	public Component getScheduler() throws OpenemsNamedException {
		var schedulerComponents = this.componentManager.getEdgeConfig()
				.getComponentsByFactory("Scheduler.AllAlphabetically");
		if (schedulerComponents.size() != 1) {
			throw new OpenemsException("Not exactly 1 Scheduler available!");
		}
		return schedulerComponents.get(0);
	}

	@Override
	public List<String> removeIdsWhichNotExist(List<String> ids, List<EdgeConfig.Component> components) {
		var existingIds = new ArrayList<>(ids);
		for (var id : ids) {

			if (components != null && components.stream().anyMatch(t -> t.getId().equals(id))) {
				continue;
			}

			try {
				this.componentManager.getComponent(id);
				continue;
			} catch (OpenemsNamedException e) {
				// component not found
			}

			existingIds.remove(id);

		}
		return existingIds;
	}

	@Override
	public List<String> insertSchedulerOrder(List<String> actualOrder, List<String> insertOrder) {
		if (actualOrder == null || actualOrder.isEmpty()) {
			return new ArrayList<>(insertOrder);
		}
		var order = new ArrayList<>(actualOrder);
		insertOrder = new ArrayList<String>(insertOrder);

		Collections.reverse(insertOrder);
		var index = actualOrder.size();
		for (String id : insertOrder) {
			var idIndex = order.indexOf(id);
			if (idIndex != -1) {
				var nextIndex = idIndex;
				if (nextIndex < index) {
					// error
				}
				index = nextIndex;
				continue;
			}
			order.add(index, id);
		}

		return order;
	}

	@Override
	public void updateHosts(//
			final User user, //
			final List<InterfaceConfiguration> ips, //
			final List<InterfaceConfiguration> oldIps //
	) throws OpenemsNamedException {
		if ((ips == null || ips.isEmpty()) && (oldIps == null || oldIps.isEmpty())) {
			return;
		}

		final var errors = new ArrayList<String>();

		var interfaces = this.getInterfaces();
		interfaces.stream() //
				.forEach(networkInterface -> {
					if (oldIps != null) {
						// remove ip's in the old configuration
						oldIps.stream() //
								.filter(t -> t.interfaceName.equals(networkInterface.getName())) //
								.forEach(t -> {
									networkInterface.getAddresses().getValue().removeAll(t.getIps());
								});
					}
					if (ips != null) {
						// add new ip's
						ips.stream() //
								.filter(t -> t.interfaceName.equals(networkInterface.getName())) //
								.forEach(t -> {
									networkInterface.getAddresses().getValue().addAll(t.getIps());
								});
					}
				});

		ips.stream() //
				.filter(ic -> !interfaces.stream().anyMatch(i -> i.getName().equals(ic.interfaceName)))
				.map(ic -> "Can not add Ip-Addresses for interface '" + ic.interfaceName + "'") //
				.forEach(errors::add);

		oldIps.stream() //
				.filter(ic -> !interfaces.stream().anyMatch(i -> i.getName().equals(ic.interfaceName)))
				.map(ic -> "Can not remove Ip-Addresses for interface '" + ic.interfaceName + "'") //
				.forEach(errors::add);

		try {
			this.updateInterfaces(user, interfaces);
		} catch (OpenemsException e) {
			errors.add(e.getMessage());
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	@Override
	public Optional<EdgeConfig.Component> getComponent(String id, String factoryId) {
		var comp = this.componentManager.getEdgeConfig().getComponent(id);
		if (comp.isEmpty() || !comp.get().getFactoryId().equals(factoryId)) {
			return Optional.empty();
		}
		return comp;
	}

	@Override
	public int[] getUsedModbusUnitIds(//
			final String modbusComponent //
	) {
		final var components = this.componentManager.getAllComponents();

		final var usedModbusUnitIds = new ArrayList<Integer>();
		for (var component : components) {
			final var props = component.getComponentContext().getProperties();

			if (find(props, t -> "modbus.id".equals(t), //
					t -> modbusComponent.equals(t)) == null) {
				continue;
			}

			final var modbusUnitIdObj = find(props, t -> "modbusUnitId".equals(t), t -> true);
			if (modbusUnitIdObj == null) {
				continue;
			}
			if (modbusUnitIdObj instanceof Integer modbusUnitId) {
				usedModbusUnitIds.add(modbusUnitId);
			}
		}

		return usedModbusUnitIds.stream() //
				.mapToInt(value -> value) //
				.toArray();
	}

	private static Object find(//
			Dictionary<String, Object> dict, //
			Predicate<String> keyPredicate, //
			Predicate<Object> objPredicate //
	) {
		return enumerationAsStream(dict.keys()) //
				.filter(key -> {
					if (!keyPredicate.test(key)) {
						return false;
					}
					var element = dict.get(key);
					if (!objPredicate.test(element)) {
						return false;
					}
					return true;
				}) //
				.findFirst() //
				.orElse(null);
	}

	// TODO move to utility class
	private static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
			public boolean tryAdvance(Consumer<? super T> action) {
				if (e.hasMoreElements()) {
					action.accept(e.nextElement());
					return true;
				}
				return false;
			}

			public void forEachRemaining(Consumer<? super T> action) {
				while (e.hasMoreElements()) {
					action.accept(e.nextElement());
				}
			}
		}, false);
	}

}
