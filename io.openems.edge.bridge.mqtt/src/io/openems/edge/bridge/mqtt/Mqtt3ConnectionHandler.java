package io.openems.edge.bridge.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mqtt.api.MqttVersion;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * MQTT 3.1/3.1.1 connection handler using Eclipse Paho v3 client.
 */
public class Mqtt3ConnectionHandler implements MqttConnectionHandler, MqttCallback {

	private final Logger log = LoggerFactory.getLogger(Mqtt3ConnectionHandler.class);

	private final Config config;
	private final String serverUri;
	private final String clientId;
	private final MqttVersion mqttVersion;

	private MqttAsyncClient client;
	private ConnectionCallback connectionCallback;
	private final Map<String, Consumer<io.openems.edge.bridge.mqtt.api.MqttMessage>> subscriptions = new ConcurrentHashMap<>();
	private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
	private volatile boolean shouldReconnect = true;

	/**
	 * Creates a new MQTT 3 connection handler.
	 *
	 * @param config      the configuration
	 * @param host        the broker host
	 * @param port        the broker port
	 * @param clientId    the client ID
	 * @param useSsl      whether to use SSL
	 * @param mqttVersion the MQTT version (V3_1 or V3_1_1)
	 */
	public Mqtt3ConnectionHandler(Config config, String host, int port, String clientId, boolean useSsl,
			MqttVersion mqttVersion) {
		this.config = config;
		this.serverUri = (useSsl ? "ssl://" : "tcp://") + host + ":" + port;
		this.clientId = clientId;
		this.mqttVersion = mqttVersion;
	}

	@Override
	public void connect(ConnectionCallback callback) {
		this.connectionCallback = callback;
		this.shouldReconnect = true;

		try {
			this.client = new MqttAsyncClient(this.serverUri, this.clientId);
			this.client.setCallback(this);

			var options = new MqttConnectOptions();
			options.setCleanSession(this.config.cleanSession());
			options.setKeepAliveInterval(this.config.keepAliveInterval());
			options.setAutomaticReconnect(false); // We handle reconnect ourselves
			options.setConnectionTimeout(30);

			// Set MQTT version
			if (this.mqttVersion == MqttVersion.V3_1) {
				options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
			} else {
				options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
			}

			// Authentication
			if (!this.config.username().isEmpty()) {
				options.setUserName(this.config.username());
				options.setPassword(this.config.password().toCharArray());
			}

			// Last Will and Testament
			if (!this.config.lwtTopic().isEmpty()) {
				options.setWill(this.config.lwtTopic(), this.config.lwtMessage().getBytes(StandardCharsets.UTF_8),
						MqttConnectionHandler.toPahoQos(this.config.lwtQos()), this.config.lwtRetained());
			}

			this.client.connect(options).waitForCompletion(30000);
			this.log.info("Connected to MQTT broker using {}: {}", this.mqttVersion.getDisplayName(), this.serverUri);
			callback.onConnected();

		} catch (MqttException e) {
			this.log.error("{} connection failed: {}", this.mqttVersion.getDisplayName(), e.getMessage());
			callback.onConnectionFailed(e.getMessage());
			this.scheduleReconnect();
		}
	}

	private void scheduleReconnect() {
		if (!this.shouldReconnect) {
			return;
		}
		this.reconnectExecutor.schedule(() -> {
			if (this.shouldReconnect && (this.client == null || !this.client.isConnected())) {
				this.log.info("Attempting to reconnect to MQTT broker...");
				this.connect(this.connectionCallback);
			}
		}, this.config.reconnectDelayMs(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void disconnect() {
		this.shouldReconnect = false;
		if (this.client != null && this.client.isConnected()) {
			try {
				this.client.disconnect().waitForCompletion(5000);
			} catch (MqttException e) {
				this.log.warn("Error disconnecting MQTT 3 client: {}", e.getMessage());
			}
		}
		if (this.client != null) {
			try {
				this.client.close();
			} catch (MqttException e) {
				this.log.warn("Error closing MQTT 3 client: {}", e.getMessage());
			}
			this.client = null;
		}
		this.reconnectExecutor.shutdownNow();
	}

	@Override
	public boolean isConnected() {
		return this.client != null && this.client.isConnected();
	}

	@Override
	public CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained) {
		if (this.client == null || !this.client.isConnected()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected"));
		}

		var future = new CompletableFuture<Void>();

		try {
			var message = new MqttMessage(payload);
			message.setQos(MqttConnectionHandler.toPahoQos(qos));
			message.setRetained(retained);

			this.client.publish(topic, message, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					future.complete(null);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					future.completeExceptionally(exception);
				}
			});
		} catch (MqttException e) {
			future.completeExceptionally(e);
		}

		return future;
	}

	@Override
	public void subscribe(String topicFilter, QoS qos, Consumer<io.openems.edge.bridge.mqtt.api.MqttMessage> callback) {
		if (this.client == null || !this.client.isConnected()) {
			return;
		}

		this.subscriptions.put(topicFilter, callback);

		try {
			this.client.subscribe(topicFilter, MqttConnectionHandler.toPahoQos(qos));
			if (this.config.debugMode()) {
				this.log.debug("Subscribed to {} with QoS {}", topicFilter, qos);
			}
		} catch (MqttException e) {
			this.log.error("Failed to subscribe to {}: {}", topicFilter, e.getMessage());
		}
	}

	@Override
	public CompletableFuture<Void> unsubscribe(String topicFilter) {
		var future = new CompletableFuture<Void>();

		if (this.client == null || !this.client.isConnected()) {
			future.complete(null);
			return future;
		}

		this.subscriptions.remove(topicFilter);

		try {
			this.client.unsubscribe(topicFilter, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					future.complete(null);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					future.completeExceptionally(exception);
				}
			});
		} catch (MqttException e) {
			future.completeExceptionally(e);
		}

		return future;
	}

	// MqttCallback implementation

	@Override
	public void connectionLost(Throwable cause) {
		var reason = cause != null ? cause.getMessage() : "Unknown";
		this.log.warn("Disconnected from MQTT broker: {}", reason);
		if (this.connectionCallback != null) {
			this.connectionCallback.onDisconnected(reason);
		}
		this.scheduleReconnect();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// Find matching subscription
		for (var entry : this.subscriptions.entrySet()) {
			if (MqttConnectionHandler.topicMatchesFilter(topic, entry.getKey())) {
				var msg = new io.openems.edge.bridge.mqtt.api.MqttMessage(//
						topic, //
						message.getPayload(), //
						MqttConnectionHandler.fromPahoQos(message.getQos()), //
						message.isRetained());
				entry.getValue().accept(msg);
			}
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// Not used for async publishing
	}

}
