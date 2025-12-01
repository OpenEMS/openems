package io.openems.edge.bridge.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * MQTT 5.0 connection handler using HiveMQ client.
 */
public class Mqtt5ConnectionHandler
		implements MqttConnectionHandler, MqttClientConnectedListener, MqttClientDisconnectedListener {

	private final Logger log = LoggerFactory.getLogger(Mqtt5ConnectionHandler.class);

	private final Config config;
	private final String host;
	private final int port;
	private final String clientId;
	private final boolean useSsl;

	private Mqtt5AsyncClient client;
	private ConnectionCallback connectionCallback;

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
		this.host = host;
		this.port = port;
		this.clientId = clientId;
		this.useSsl = useSsl;
	}

	@Override
	public void connect(ConnectionCallback callback) {
		this.connectionCallback = callback;

		var builder = MqttClient.builder() //
				.useMqttVersion5() //
				.identifier(this.clientId) //
				.serverHost(this.host) //
				.serverPort(this.port) //
				.addConnectedListener(this) //
				.addDisconnectedListener(this) //
				.automaticReconnect() //
				.initialDelay(this.config.reconnectDelayMs(), TimeUnit.MILLISECONDS) //
				.maxDelay(this.config.maxReconnectDelayMs(), TimeUnit.MILLISECONDS) //
				.applyAutomaticReconnect();

		if (this.useSsl) {
			builder.sslWithDefaultConfig();
		}

		this.client = builder.buildAsync();

		var connectBuilder = this.client.connectWith() //
				.cleanStart(this.config.cleanSession()) //
				.keepAlive(this.config.keepAliveInterval());

		// Authentication
		if (!this.config.username().isEmpty()) {
			connectBuilder.simpleAuth() //
					.username(this.config.username()) //
					.password(this.config.password().getBytes(StandardCharsets.UTF_8)) //
					.applySimpleAuth();
		}

		// Last Will and Testament
		if (!this.config.lwtTopic().isEmpty()) {
			connectBuilder.willPublish() //
					.topic(this.config.lwtTopic()) //
					.payload(this.config.lwtMessage().getBytes(StandardCharsets.UTF_8)) //
					.qos(MqttConnectionHandler.toHiveMqQos(this.config.lwtQos())) //
					.retain(this.config.lwtRetained()) //
					.applyWillPublish();
		}

		connectBuilder.send() //
				.whenComplete((connAck, throwable) -> {
					if (throwable != null) {
						this.log.error("MQTT 5.0 connection failed: {}", throwable.getMessage());
						callback.onConnectionFailed(throwable.getMessage());
					} else {
						this.log.info("Connected to MQTT broker using MQTT 5.0");
						callback.onConnected();
					}
				});
	}

	@Override
	public void disconnect() {
		if (this.client != null) {
			try {
				this.client.disconnect().get(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				this.log.warn("Error disconnecting MQTT 5 client: {}", e.getMessage());
			}
			this.client = null;
		}
	}

	@Override
	public boolean isConnected() {
		return this.client != null && this.client.getState() == MqttClientState.CONNECTED;
	}

	@Override
	public CompletableFuture<Void> publish(String topic, byte[] payload, QoS qos, boolean retained) {
		if (this.client == null) {
			var future = new CompletableFuture<Void>();
			future.completeExceptionally(new IllegalStateException("Not connected"));
			return future;
		}

		return this.client.publishWith() //
				.topic(topic) //
				.payload(payload) //
				.qos(MqttConnectionHandler.toHiveMqQos(qos)) //
				.retain(retained) //
				.send() //
				.thenApply(publish -> null);
	}

	@Override
	public void subscribe(String topicFilter, QoS qos, Consumer<MqttMessage> callback) {
		if (this.client == null) {
			return;
		}

		this.client.subscribeWith() //
				.topicFilter(topicFilter) //
				.qos(MqttConnectionHandler.toHiveMqQos(qos)) //
				.callback(publish -> {
					var msg = new MqttMessage(//
							publish.getTopic().toString(), //
							publish.getPayloadAsBytes(), //
							MqttConnectionHandler.fromHiveMqQos(publish.getQos()), //
							publish.isRetain());
					callback.accept(msg);
				}) //
				.send();

		if (this.config.debugMode()) {
			this.log.debug("Subscribed to {} with QoS {}", topicFilter, qos);
		}
	}

	@Override
	public CompletableFuture<Void> unsubscribe(String topicFilter) {
		if (this.client == null || !this.isConnected()) {
			return CompletableFuture.completedFuture(null);
		}

		return this.client.unsubscribeWith() //
				.topicFilter(topicFilter) //
				.send() //
				.thenApply(unsub -> null);
	}

	@Override
	public void onConnected(MqttClientConnectedContext context) {
		this.log.info("Connected to MQTT broker (MQTT 5.0)");
		if (this.connectionCallback != null) {
			this.connectionCallback.onConnected();
		}
	}

	@Override
	public void onDisconnected(MqttClientDisconnectedContext context) {
		var reason = context.getCause() != null ? context.getCause().getMessage() : "Unknown";
		this.log.warn("Disconnected from MQTT broker: {}", reason);
		if (this.connectionCallback != null) {
			this.connectionCallback.onDisconnected(reason);
		}
	}

}
