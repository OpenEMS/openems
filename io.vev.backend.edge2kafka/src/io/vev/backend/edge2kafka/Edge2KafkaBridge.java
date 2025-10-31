package io.vev.backend.edge2kafka;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.TreeBasedTable;
import io.vev.backend.metadata.token.VevEdge;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edge.EdgeManager;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Vev.Edge2Kafka", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2KafkaBridge extends AbstractOpenemsBackendComponent implements Timedata {

	private enum DataType {
		TIMESTAMPED("timestamped"), //
		AGGREGATED("aggregated"), //
		RESEND("resend");

		private final String label;

		DataType(String label) {
			this.label = label;
		}

		public String label() {
			return this.label;
		}
	}

	private final Logger log = LoggerFactory.getLogger(Edge2KafkaBridge.class);

	@Reference
	private volatile EdgeManager edgeManager;

	@Reference
	private volatile Metadata metadata;

	private Config config;
	private Set<String> componentPrefixes = Set.of();
	private boolean acceptsAllComponents;
	private EnumSet<DataType> enabledTypes = EnumSet.of(DataType.TIMESTAMPED, DataType.RESEND);

	private String bootstrapServers = "";
	private boolean producerEnabled;
	private boolean consumerEnabled;
	private String dataTopic = "vev.edge.data";
	private String commandTopic = "vev.edge.commands";
	private String producerClientId = "edge2kafka-producer";
	private String consumerClientId = "edge2kafka-consumer";
	private String consumerGroupId = "edge2kafka";
	private int consumerPollIntervalMs = 1000;
	private int consumerShutdownTimeoutMs = 10000;

	private KafkaClient kafkaClient;
	private final Map<String, User> commandUsers = new ConcurrentHashMap<>();

	public Edge2KafkaBridge() {
		super("Vev.Edge2Kafka");
	}

	@Activate
	private void activate(Config config) {
		this.updateConfig(config);
		this.configureKafka();
		this.logInfo(this.log, "Activate [prefixes=" + this.componentPrefixes + "]");
	}

	@Modified
	private void modified(Config config) {
		this.updateConfig(config);
		this.configureKafka();
		this.logInfo(this.log, "Modified [prefixes=" + this.componentPrefixes + "]");
	}

	@Deactivate
	private void deactivate() {
		if (this.kafkaClient != null) {
			this.kafkaClient.stop();
			this.kafkaClient = null;
		}
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification data) {
		this.handleNotification(edgeId, data, DataType.TIMESTAMPED);
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification data) {
		if (this.enabledTypes.contains(DataType.AGGREGATED)) {
			this.handleNotification(edgeId, data, DataType.AGGREGATED);
		}
	}

	@Override
	public void write(String edgeId, ResendDataNotification data) {
		if (this.enabledTypes.contains(DataType.RESEND)) {
			this.handleNotification(edgeId, data, DataType.RESEND);
		} else {
			this.logInfo(this.log,
					"Resend notification ignored for Edge=" + edgeId + " (aggregated logging disabled)");
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		this.logWarn(this.log, "Historic data queries are not supported");
		return new TreeMap<>();
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		this.logWarn(this.log, "Historic energy queries are not supported");
		return new TreeMap<>();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		this.logWarn(this.log, "Historic energy-per-period queries are not supported");
		return new TreeMap<>();
	}

	@Override
	public String id() {
		return this.config.id();
	}

	private void updateConfig(Config config) {
		this.config = config;

		var prefixes = Optional.ofNullable(config.component_prefixes()).orElse("").trim();
		if (prefixes.isEmpty()) {
			this.componentPrefixes = Set.of();
		} else {
			this.componentPrefixes = List.of(prefixes.split(",")).stream() //
					.map(String::trim) //
					.filter(s -> !s.isEmpty()) //
					.collect(Collectors.toUnmodifiableSet());
		}
		this.acceptsAllComponents = this.componentPrefixes.isEmpty();

		this.enabledTypes = EnumSet.of(DataType.TIMESTAMPED, DataType.RESEND);
		if (config.log_aggregated()) {
			this.enabledTypes.add(DataType.AGGREGATED);
		}

		this.bootstrapServers = Optional.ofNullable(config.kafka_bootstrap_servers()).map(String::trim).orElse("");
		this.dataTopic = Optional.ofNullable(config.kafka_data_topic()).map(String::trim).filter(s -> !s.isEmpty())
				.orElse("vev.edge.data");
		this.commandTopic = Optional.ofNullable(config.kafka_command_topic()).map(String::trim)
				.filter(s -> !s.isEmpty()).orElse("vev.edge.commands");
		this.producerClientId = Optional.ofNullable(config.kafka_producer_client_id()).map(String::trim)
				.filter(s -> !s.isEmpty()).orElse("edge2kafka-producer");
		this.consumerClientId = Optional.ofNullable(config.kafka_consumer_client_id()).map(String::trim)
				.filter(s -> !s.isEmpty()).orElse("edge2kafka-consumer");
		this.consumerGroupId = Optional.ofNullable(config.kafka_consumer_group_id()).map(String::trim)
				.filter(s -> !s.isEmpty()).orElse("edge2kafka");
		this.consumerPollIntervalMs = Math.max(100, config.kafka_consumer_poll_interval_ms());
		this.consumerShutdownTimeoutMs = Math.max(1000, config.kafka_consumer_shutdown_timeout_ms());

		var bootstrapConfigured = !this.bootstrapServers.isBlank();
		this.producerEnabled = bootstrapConfigured && config.kafka_enable_producer();
		this.consumerEnabled = bootstrapConfigured && config.kafka_enable_consumer();
	}

	private void configureKafka() {
		if (this.kafkaClient != null) {
			this.kafkaClient.stop();
			this.kafkaClient = null;
		}
		this.commandUsers.clear();

		if (this.bootstrapServers.isBlank()) {
			this.logInfo(this.log, "Kafka disabled: no bootstrap servers configured");
			return;
		}

		this.kafkaClient = new KafkaClient(
				this.producerEnabled,
				this.consumerEnabled,
				this.dataTopic,
				this.commandTopic,
				this.consumerGroupId,
				this.createProducerProperties(),
				this.createConsumerProperties(),
				Duration.ofMillis(this.consumerPollIntervalMs),
				this.consumerShutdownTimeoutMs,
				this::processCommandRecord,
				message -> this.logInfo(this.log, message),
				message -> this.logWarn(this.log, message),
				this.config.id());

		this.kafkaClient.start();
	}

	private Properties createProducerProperties() {
		var props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, this.producerClientId);
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		return props;
	}

	private Properties createConsumerProperties() {
		var props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, this.consumerGroupId);
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, this.consumerClientId);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		return props;
	}

	private void processCommandRecord(ConsumerRecord<String, String> record) {
		var payload = record.value();
		if (payload == null || payload.isBlank()) {
			this.logWarn(this.log,
					"Ignoring Kafka command without payload [topic=%s, partition=%d, offset=%d]".formatted(
							record.topic(), record.partition(), record.offset()));
			return;
		}

		try {
			var jsonElement = JsonParser.parseString(payload);
			if (!jsonElement.isJsonObject()) {
				this.logWarn(this.log, "Kafka command is not a JSON object: " + payload);
				return;
			}
			var json = jsonElement.getAsJsonObject();

			var edgeIdOpt = this.getString(json, "edgeId");
			var componentIdOpt = this.getString(json, "componentId");
			var channelIdOpt = this.getString(json, "channelId");

			if (edgeIdOpt.isEmpty() || componentIdOpt.isEmpty() || channelIdOpt.isEmpty()) {
				this.logWarn(this.log, "Kafka command missing required fields: " + payload);
				return;
			}

			var edgeId = edgeIdOpt.get();
			var componentId = componentIdOpt.get();
			var channelId = channelIdOpt.get();
			var valueElement = json.has("value") ? json.get("value") : JsonNull.INSTANCE;

			var request = new SetChannelValueRequest(componentId, channelId, valueElement.deepCopy());
			var user = this.getCommandUser(edgeId);

			this.edgeManager.send(edgeId, user, request).whenComplete((response, throwable) -> {
				if (throwable != null) {
					this.logWarn(this.log,
							"Kafka command failed [edge=%s, component=%s, channel=%s]: %s".formatted(edgeId,
									componentId, channelId, throwable.getMessage()));
				} else {
					this.logInfo(this.log,
							"Kafka command applied [edge=%s, component=%s, channel=%s]".formatted(edgeId, componentId,
									channelId));
				}
			});
		} catch (Exception e) {
			this.logWarn(this.log, "Unable to process Kafka command message: " + e.getMessage());
		}
	}

	private Optional<String> getString(JsonObject json, String memberName) {
		if (!json.has(memberName)) {
			return Optional.empty();
		}
		var element = json.get(memberName);
		if (!element.isJsonPrimitive()) {
			return Optional.empty();
		}
		return Optional.of(element.getAsString());
	}

	private User getCommandUser(String edgeId) {
		return this.commandUsers.computeIfAbsent(edgeId, id -> {
			var roles = new TreeMap<String, Role>();
			roles.put(id, Role.ADMIN);
			return new User("edge2kafka", "Edge2Kafka", UUID.randomUUID().toString(), Language.EN, Role.ADMIN, roles,
					true, new JsonObject());
		});
	}

	private NavigableMap<Long, Map<String, JsonElement>> selectChannels(TreeBasedTable<Long, String, JsonElement> data) {
		var filtered = new TreeMap<Long, Map<String, JsonElement>>();
		for (var rowEntry : data.rowMap().entrySet()) {
			Map<String, JsonElement> selectedForRow = null;
				for (var channelEntry : rowEntry.getValue().entrySet()) {
					var rawAddress = channelEntry.getKey();
					final ChannelAddress address;
					try {
						address = ChannelAddress.fromString(rawAddress);
					} catch (IllegalArgumentException | OpenemsNamedException e) {
						this.logWarn(this.log, "Invalid ChannelAddress in data: " + rawAddress);
						continue;
					}

					if (!this.acceptsComponent(address.getComponentId())) {
						continue;
					}

					if (selectedForRow == null) {
						selectedForRow = new TreeMap<>();
				}
				selectedForRow.put(address.toString(), channelEntry.getValue().deepCopy());
			}
			if (selectedForRow != null && !selectedForRow.isEmpty()) {
				filtered.put(rowEntry.getKey(), selectedForRow);
			}
		}
			return filtered;
		}

	private boolean acceptsComponent(String componentId) {
		if (this.acceptsAllComponents) {
			return true;
		}
		for (var prefix : this.componentPrefixes) {
			if (componentId.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private Collection<String> buildPayloads(String edgeId, VevEdge edge,
			NavigableMap<Long, Map<String, JsonElement>> rows) {
		if (rows.isEmpty()) {
			return List.of();
		}

		var payloads = new ArrayList<String>(rows.size());
		for (var entry : rows.entrySet()) {
			var payload = new JsonObject();
			payload.addProperty("edgeId", edgeId);
			payload.addProperty("timestamp", entry.getKey());
			var comment = edge.getComment();
			if (comment != null && !comment.isBlank()) {
				payload.addProperty("edgeComment", comment);
			}
			var productType = edge.getProducttype();
			if (productType != null && !productType.isBlank()) {
				payload.addProperty("edgeProductType", productType);
			}
			var values = new JsonObject();
			for (var channelEntry : entry.getValue().entrySet()) {
				values.add(channelEntry.getKey(), channelEntry.getValue());
			}
			payload.add("values", values);
			payloads.add(payload.toString());
		}
		return payloads;
	}

	private void handleNotification(String edgeId, AbstractDataNotification notification, DataType type) {
		if (edgeId.equals("backend0")) {
			this.logInfo(this.log, "Ignoring notification from backend edge");
			return;
		}
		if (type != DataType.TIMESTAMPED) {
			this.logInfo(this.log, "Skipping handling of non-timestamped notification for Edge=" + edgeId);
			return;
		}
		final var optEdge = this.metadata.getEdge(edgeId);
		if (optEdge.isEmpty() || !(optEdge.get() instanceof VevEdge edge)) {
			return;
		}

		var selectedRows = this.selectChannels(notification.getData());
		if (selectedRows.isEmpty()) {
			return;
		}

		var payloads = this.buildPayloads(edgeId, edge, selectedRows);
		if (payloads.isEmpty()) {
			return;
		}

		if (this.kafkaClient != null) {
			this.kafkaClient.publish(edgeId, payloads);
		}
	}

}
