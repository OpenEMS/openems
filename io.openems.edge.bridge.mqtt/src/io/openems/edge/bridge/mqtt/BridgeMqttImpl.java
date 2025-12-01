package io.openems.edge.bridge.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mqtt.MqttConnectionHandler.ConnectionCallback;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.Mqtt", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BridgeMqttImpl extends AbstractOpenemsComponent
		implements BridgeMqtt, OpenemsComponent, ConnectionCallback {

	private final Logger log = LoggerFactory.getLogger(BridgeMqttImpl.class);

	private final Map<String, SubscriptionHolder> subscriptions = new ConcurrentHashMap<>();

	private Config config;
	private MqttConnectionHandler connectionHandler;

	public BridgeMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMqtt.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!config.enabled()) {
			return;
		}

		this.connect();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());

		// Disconnect and reconnect if configuration changed
		this.disconnect();
		this.config = config;

		if (config.enabled()) {
			this.connect();
		}
	}

	@Deactivate
	protected void deactivate() {
		this.disconnect();
		super.deactivate();
	}

	/**
	 * Establishes connection to the MQTT broker.
	 */
	private void connect() {
		var clientId = this.config.clientId().isEmpty() //
				? "openems-" + UUID.randomUUID().toString().substring(0, 8) //
				: this.config.clientId();

		// Parse URI to get host and port
		var uri = this.config.uri();
		var host = "localhost";
		var port = 1883;
		var useSsl = false;

		if (uri.startsWith("ssl://") || uri.startsWith("tls://")) {
			useSsl = true;
			uri = uri.substring(6);
		} else if (uri.startsWith("tcp://")) {
			uri = uri.substring(6);
		}

		var colonIndex = uri.indexOf(':');
		if (colonIndex > 0) {
			host = uri.substring(0, colonIndex);
			try {
				port = Integer.parseInt(uri.substring(colonIndex + 1));
			} catch (NumberFormatException e) {
				this.log.warn("Invalid port in URI, using default: {}", port);
			}
		} else {
			host = uri;
		}

		if (this.config.debugMode()) {
			this.log.info("Connecting to MQTT broker: {}:{} with client ID: {} using MQTT {}", //
					host, port, clientId, this.config.mqttVersion().getDisplayName());
		}

		// Create appropriate handler based on MQTT version
		this.connectionHandler = switch (this.config.mqttVersion()) {
		case V3_1, V3_1_1 -> new Mqtt3ConnectionHandler(this.config, host, port, clientId, useSsl);
		case V5 -> new Mqtt5ConnectionHandler(this.config, host, port, clientId, useSsl);
		};

		this.connectionHandler.connect(this);
	}

	/**
	 * Disconnects from the MQTT broker.
	 */
	private void disconnect() {
		if (this.connectionHandler != null) {
			this.connectionHandler.disconnect();
			this.connectionHandler = null;
		}
		this._setConnected(false);
		this.log.info("Disconnected from MQTT broker");
	}

	/**
	 * Re-subscribes to all active subscriptions after reconnect.
	 */
	private void resubscribeAll() {
		if (this.connectionHandler == null) {
			return;
		}

		this.subscriptions.forEach((topicFilter, holder) -> {
			try {
				this.connectionHandler.subscribe(topicFilter, holder.qos, this.createDispatcher(topicFilter));
				if (this.config.debugMode()) {
					this.log.debug("Re-subscribed to topic: {}", topicFilter);
				}
			} catch (Exception e) {
				this.log.error("Failed to re-subscribe to {}: {}", topicFilter, e.getMessage());
			}
		});
	}

	/**
	 * Creates a message dispatcher for a topic filter.
	 *
	 * @param filter the topic filter
	 * @return the message consumer
	 */
	private Consumer<MqttMessage> createDispatcher(String filter) {
		return msg -> {
			if (this.config.debugMode()) {
				this.log.debug("Message arrived on {}: {} bytes", msg.topic(), msg.payload().length);
			}

			this.subscriptions.forEach((f, holder) -> {
				if (this.topicMatchesFilter(msg.topic(), f)) {
					try {
						holder.callback.accept(msg);
					} catch (Exception e) {
						this.log.error("Error in message callback for {}: {}", msg.topic(), e.getMessage());
					}
				}
			});
		};
	}

	@Override
	public void onConnected() {
		this._setConnected(true);
		this._setConnectionFailed(false);
		this._setBrokerUnreachable(false);
		this.resubscribeAll();
	}

	@Override
	public void onConnectionFailed(String error) {
		this._setConnectionFailed(true);
		this._setConnected(false);
	}

	@Override
	public void onDisconnected(String reason) {
		this._setConnected(false);
		this._setBrokerUnreachable(true);
	}

	@Override
	public MqttVersion getMqttVersion() {
		return this.config.mqttVersion();
	}

	@Override
	public boolean isConnected() {
		return this.connectionHandler != null && this.connectionHandler.isConnected();
	}

	@Override
	public CompletableFuture<Void> publish(String topic, String payload, QoS qos, boolean retained) {
		return this.publish(topic, payload.getBytes(StandardCharsets.UTF_8), qos, retained);
	}

	@Override
	public CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained) {
		if (this.config.debugMode()) {
			this.log.debug("Publishing to {}: {} bytes, QoS={}, retained={}", //
					topic, payload.length, qos, retained);
		}

		if (this.connectionHandler == null) {
			var future = new CompletableFuture<Void>();
			future.completeExceptionally(new IllegalStateException("Not connected to MQTT broker"));
			return future;
		}

		return this.connectionHandler.publish(topic, payload, qos, retained);
	}

	@Override
	public MqttSubscription subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
		var holder = new SubscriptionHolder(this, topicFilter, qos, callback);
		this.subscriptions.put(topicFilter, holder);

		if (this.connectionHandler != null && this.connectionHandler.isConnected()) {
			this.connectionHandler.subscribe(topicFilter, qos, this.createDispatcher(topicFilter));
		}

		return holder;
	}

	@Override
	public CompletableFuture<Void> unsubscribe(MqttSubscription subscription) {
		this.subscriptions.remove(subscription.topicFilter());

		if (this.connectionHandler != null && this.connectionHandler.isConnected()) {
			return this.connectionHandler.unsubscribe(subscription.topicFilter());
		}

		return CompletableFuture.completedFuture(null);
	}

	private void _setConnected(boolean value) {
		this.channel(BridgeMqtt.ChannelId.CONNECTED).setNextValue(value);
	}

	private void _setConnectionFailed(boolean value) {
		this.channel(BridgeMqtt.ChannelId.CONNECTION_FAILED).setNextValue(value);
	}

	private void _setBrokerUnreachable(boolean value) {
		this.channel(BridgeMqtt.ChannelId.BROKER_UNREACHABLE).setNextValue(value);
	}

	/**
	 * Checks if a topic matches a topic filter with wildcards.
	 *
	 * @param topic  the topic to check
	 * @param filter the topic filter (may contain wildcards)
	 * @return true if the topic matches the filter
	 */
	private boolean topicMatchesFilter(String topic, String filter) {
		if (filter.equals("#")) {
			return true;
		}
		if (filter.equals(topic)) {
			return true;
		}

		var topicParts = topic.split("/");
		var filterParts = filter.split("/");

		int i = 0;
		for (; i < filterParts.length; i++) {
			var filterPart = filterParts[i];

			if (filterPart.equals("#")) {
				return true; // Multi-level wildcard matches everything
			}

			if (i >= topicParts.length) {
				return false; // Topic is shorter than filter
			}

			if (!filterPart.equals("+") && !filterPart.equals(topicParts[i])) {
				return false; // Single-level wildcard or exact match failed
			}
		}

		return i == topicParts.length; // Lengths must match (except for #)
	}

}
