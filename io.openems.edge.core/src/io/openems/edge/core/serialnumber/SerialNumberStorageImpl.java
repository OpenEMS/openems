package io.openems.edge.core.serialnumber;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.serialnumber.SerialNumberStorage;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = SerialNumberStorage.SINGLETON_SERVICE_PID, //
		scope = ServiceScope.SINGLETON, //
		immediate = true, //
		property = { //
				"enabled=true" //
		} //
)
public final class SerialNumberStorageImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, SerialNumberStorage {

	private static final int UPDATE_CONFIG_MINUTES_DELAY = 10;

	private final Logger log = LoggerFactory.getLogger(SerialNumberStorageImpl.class);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	private ScheduledExecutorService executor;
	private ScheduledFuture<?> activeFuture;

	private Map<String, Map<String, JsonElement>> dataFromConfig;
	private Map<String, Map<String, JsonElement>> data;

	public SerialNumberStorageImpl() {
		super(OpenemsComponent.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);

		try {
			this.dataFromConfig = parseFromConfig(config.data());
			this.data = this.dataFromConfig.entrySet().stream() //
					.collect(toConcurrentMap(Entry::getKey, t -> t.getValue().entrySet().stream()
							.collect(toConcurrentMap(Entry::getKey, Entry::getValue))));
		} catch (Exception e) {
			this.log.warn("Unable to parse config.", e);
			this.dataFromConfig = new HashMap<>();
			this.data = new ConcurrentHashMap<>();
		}

		this.executor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().name(SINGLETON_COMPONENT_ID).factory());

		if (OpenemsComponent.validateSingleton(this.configurationAdmin, SINGLETON_SERVICE_PID,
				SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		final var prev = this.dataFromConfig;
		try {
			this.dataFromConfig = parseFromConfig(config.data());
		} catch (Exception e) {
			this.log.warn("Unable to parse config.", e);
			this.dataFromConfig = new HashMap<>();
		}

		final var diff = ConfigDiff.between(prev, this.dataFromConfig);
		diff.applyTo(this.data);

		if (OpenemsComponent.validateSingleton(this.configurationAdmin, SINGLETON_SERVICE_PID,
				SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.executor.shutdownNow();
	}

	@Override
	public void put(String componentId, String channelId, JsonElement value) {
		final var previousValue = this.data.computeIfAbsent(componentId, t -> new ConcurrentHashMap<>()) //
				.put(channelId, value);

		if (value.equals(previousValue)) {
			return;
		}

		this.scheduleUpdateConfig();
	}

	private synchronized void scheduleUpdateConfig() {
		if (this.activeFuture != null) {
			this.activeFuture.cancel(false);
		}

		this.activeFuture = this.executor.schedule(this::updateConfig, UPDATE_CONFIG_MINUTES_DELAY, TimeUnit.MINUTES);
	}

	private void updateConfig() {
		try {
			final var config = this.configurationAdmin.getConfiguration(SINGLETON_SERVICE_PID, "?");

			final var properties = new Hashtable<String, Object>();
			properties.put("data", JsonUtils.prettyToString(serializer().serialize(this.data)));
			config.updateIfDifferent(properties);
		} catch (IOException e) {
			this.log.warn("Unable to update " + this.id() + " config", e);
		}
	}

	private static Map<String, Map<String, JsonElement>> parseFromConfig(String raw) throws OpenemsNamedException {
		if (raw == null || raw.isBlank()) {
			return new HashMap<>();
		}
		return serializer().deserialize(JsonUtils.parse(raw));
	}

	private static JsonSerializer<Map<String, Map<String, JsonElement>>> serializer() {
		return jsonObjectSerializer(json -> {
			return json.collectStringKeys(toMap(Entry::getKey, t -> {
				final var componentData = t.getValue().getAsJsonObjectPath();
				return componentData.collectStringKeys(toMap(Entry::getKey, e -> e.getValue().get()));
			}));
		}, obj -> {
			return obj.entrySet().stream() //
					.collect(toJsonObject(Entry::getKey, t -> t.getValue().entrySet().stream()//
							.collect(toJsonObject(Entry::getKey, Entry::getValue))));
		});
	}

	public record ConfigDiff(//
			List<Entry<ChannelAddress, JsonElement>> addedOrModifiedValues, //
			List<ChannelAddress> removedValues //
	) {

		/**
		 * Creates a {@link ConfigDiff} between the previous data and the current data.
		 * 
		 * @param prev    the previous data
		 * @param current the current data
		 * @return the difference between the two data sets
		 */
		public static ConfigDiff between(//
				Map<String, Map<String, JsonElement>> prev, //
				Map<String, Map<String, JsonElement>> current //
		) {

			final var addedOrModifiedValues = new ArrayList<Entry<ChannelAddress, JsonElement>>();
			for (var componentEntry : current.entrySet()) {
				final var prevComponentData = prev.get(componentEntry.getKey());
				if (prevComponentData == null) {
					for (var valueEntry : componentEntry.getValue().entrySet()) {
						addedOrModifiedValues
								.add(Map.entry(new ChannelAddress(componentEntry.getKey(), valueEntry.getKey()),
										valueEntry.getValue()));
					}
					continue;
				}

				for (var valueEntry : componentEntry.getValue().entrySet()) {
					final var value = prevComponentData.get(valueEntry.getKey());
					if (value == null) {
						addedOrModifiedValues
								.add(Map.entry(new ChannelAddress(componentEntry.getKey(), valueEntry.getKey()),
										valueEntry.getValue()));
						continue;
					}
					if (value.equals(valueEntry.getValue())) {
						continue;
					}

					addedOrModifiedValues.add(Map.entry(
							new ChannelAddress(componentEntry.getKey(), valueEntry.getKey()), valueEntry.getValue()));
				}
			}

			final var removedValues = new ArrayList<ChannelAddress>();
			for (var componentEntry : prev.entrySet()) {
				final var currentComponentData = current.get(componentEntry.getKey());

				if (currentComponentData == null) {
					removedValues.addAll(componentEntry.getValue().keySet().stream() //
							.map(t -> new ChannelAddress(componentEntry.getKey(), t)).toList());
					continue;
				}

				for (var valueEntry : componentEntry.getValue().entrySet()) {
					final var value = currentComponentData.get(valueEntry.getKey());
					if (value == null) {
						removedValues.add(new ChannelAddress(componentEntry.getKey(), valueEntry.getKey()));
					}
				}
			}

			return new ConfigDiff(addedOrModifiedValues, removedValues);
		}

		/**
		 * Applies the current changes to the provided data.
		 * 
		 * @param data the data to apply the current changes to
		 */
		public void applyTo(Map<String, Map<String, JsonElement>> data) {
			for (var entry : this.addedOrModifiedValues()) {
				data.computeIfAbsent(entry.getKey().getComponentId(), t -> new ConcurrentHashMap<>()) //
						.put(entry.getKey().getChannelId(), entry.getValue());
			}

			for (var channelAddress : this.removedValues()) {
				final var componentData = data.get(channelAddress.getComponentId());
				if (componentData == null) {
					continue;
				}
				componentData.remove(channelAddress.getChannelId());
				if (componentData.isEmpty()) {
					data.remove(channelAddress.getComponentId());
				}
			}
		}

	}

}
