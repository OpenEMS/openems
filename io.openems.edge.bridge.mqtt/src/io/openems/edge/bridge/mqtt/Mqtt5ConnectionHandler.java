package io.openems.edge.bridge.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * MQTT 5.0 connection handler using Eclipse Paho client.
 */
public class Mqtt5ConnectionHandler implements MqttConnectionHandler, MqttCallback {

	private final Logger log = LoggerFactory.getLogger(Mqtt5ConnectionHandler.class);

	private final Config config;
	private final String serverUri;
	private final String clientId;

	private MqttAsyncClient client;
	private ConnectionCallback connectionCallback;
	private final Map<String, Consumer<io.openems.edge.bridge.mqtt.api.MqttMessage>> subscriptions = new ConcurrentHashMap<>();
	private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
	private volatile boolean shouldReconnect = true;

	/**
	 * Creates a new MQTT 5 connection handler.
	 *
	 * @param config   the configuration
	 * @param host     the broker host
	 * @param port     the broker port
	 * @param clientId the client ID
	 * @param useSsl   whether to use SSL
	 */
	public Mqtt5ConnectionHandler(Config config, String host, int port, String clientId, boolean useSsl) {
		this.config = config;
		this.serverUri = (useSsl ? "ssl://" : "tcp://") + host + ":" + port;
		this.clientId = clientId;
	}

	@Override
	public void connect(ConnectionCallback callback) {
		this.connectionCallback = callback;
		this.shouldReconnect = true;

		try {
			this.client = new MqttAsyncClient(this.serverUri, this.clientId);
			this.client.setCallback(this);

			var options = new MqttConnectionOptions();
			options.setCleanStart(this.config.cleanSession());
			options.setKeepAliveInterval(this.config.keepAliveInterval());
			options.setAutomaticReconnect(false); // We handle reconnect ourselves
			options.setConnectionTimeout(30);

			// Authentication
			if (!this.config.username().isEmpty()) {
				options.setUserName(this.config.username());
				options.setPassword(this.config.password().getBytes(StandardCharsets.UTF_8));
			}

			// Last Will and Testament
			if (!this.config.lwtTopic().isEmpty()) {
				var willMessage = new MqttMessage(this.config.lwtMessage().getBytes(StandardCharsets.UTF_8));
				willMessage.setQos(MqttConnectionHandler.toPahoQos(this.config.lwtQos()));
				willMessage.setRetained(this.config.lwtRetained());
				options.setWill(this.config.lwtTopic(), willMessage);
			}

			this.client.connect(options).waitForCompletion(30000);
			this.log.info("Connected to MQTT broker using MQTT 5.0: {}", this.serverUri);
			callback.onConnected();

		} catch (MqttException e) {
			this.log.error("MQTT 5.0 connection failed: {}", e.getMessage());
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
				this.log.warn("Error disconnecting MQTT 5 client: {}", e.getMessage());
			}
		}
		if (this.client != null) {
			try {
				this.client.close();
			} catch (MqttException e) {
				this.log.warn("Error closing MQTT 5 client: {}", e.getMessage());
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

			this.client.publish(topic, message, null, new org.eclipse.paho.mqttv5.client.MqttActionListener() {
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
			this.client.unsubscribe(topicFilter, null, new org.eclipse.paho.mqttv5.client.MqttActionListener() {
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
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		var reason = disconnectResponse != null ? disconnectResponse.getReasonString() : "Unknown";
		this.log.warn("Disconnected from MQTT broker: {}", reason);
		if (this.connectionCallback != null) {
			this.connectionCallback.onDisconnected(reason);
		}
		this.scheduleReconnect();
	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		this.log.error("MQTT error: {}", exception.getMessage());
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
	public void deliveryComplete(IMqttToken token) {
		// Not used for async publishing
	}

	@Override
	public void connectComplete(boolean reconnect, String serverUri) {
		this.log.info("{}connected to MQTT broker: {}", reconnect ? "Re" : "", serverUri);
		if (this.connectionCallback != null) {
			this.connectionCallback.onConnected();
		}
		// Re-subscribe to all topics after reconnect
		if (reconnect) {
			for (var entry : this.subscriptions.entrySet()) {
				try {
					this.client.subscribe(entry.getKey(), 1); // Default QoS 1
				} catch (MqttException e) {
					this.log.error("Failed to re-subscribe to {}: {}", entry.getKey(), e.getMessage());
				}
			}
		}
	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// Not used
	}

}
