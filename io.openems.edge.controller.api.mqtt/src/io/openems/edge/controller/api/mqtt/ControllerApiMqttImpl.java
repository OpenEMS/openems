package io.openems.edge.controller.api.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.MQTT", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE //
})
public class ControllerApiMqttImpl extends AbstractOpenemsComponent
		implements ControllerApiMqtt, Controller, OpenemsComponent, EventHandler {

	protected static final String COMPONENT_NAME = "Controller.Api.MQTT";
	private static final long INITIAL_RECONNECT_DELAY_SECONDS = 5;
	private static final long MAX_RECONNECT_DELAY_SECONDS = 300; // 5 minutes maximum delay.
	private static final double RECONNECT_DELAY_MULTIPLIER = 1.5;

	private final Logger log = LoggerFactory.getLogger(ControllerApiMqttImpl.class);
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	private final SendChannelValuesWorker sendChannelValuesWorker = new SendChannelValuesWorker(this);
	private final MqttConnector mqttConnector = new MqttConnector();
	private final AtomicInteger reconnectionAttempt = new AtomicInteger(0);

	private volatile ScheduledFuture<?> reconnectFuture = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	protected Config config;

	private String topicPrefix;
	private IMqttClient mqttClient = null;

	public ControllerApiMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerApiMqtt.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
		scheduleReconnect();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		applyConfig(config);
		super.modified(context, config.id(), this.topicPrefix, config.enabled());
		scheduleReconnect();
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.topicPrefix = String.format(ControllerApiMqtt.TOPIC_PREFIX, config.clientId());
	}
		this.topicPrefix = createTopicPrefix(config);


	private synchronized void scheduleReconnect() {
		if (reconnectFuture != null && !reconnectFuture.isDone()) {
			reconnectFuture.cancel(false);
		}

		attemptConnect();
	}

	private void attemptConnect() {
		if (mqttClient != null && mqttClient.isConnected()) {
			return; // Already connected
		}
		try {
			if (mqttClient == null || !mqttClient.isConnected()) {
				this.mqttConnector.connect(config.uri(), config.clientId(), config.username(), config.password(),
						config.certPem(), config.privateKeyPem(), config.trustStorePem()).thenAccept(client -> {
							mqttClient = client;
							logInfo(this.log, "Connected to MQTT Broker [" + config.uri() + "]");
							reconnectionAttempt.set(0); // Reset on successful connection.
						}).exceptionally(ex -> {
							log.error("Failed to connect to MQTT broker: " + ex.getMessage(), ex);
							scheduleNextAttempt(); // Schedule the next attempt with an increased delay.
							return null;
						});
			}
		} catch (Exception e) {
			log.error("Error attempting to connect to MQTT broker", e);
			scheduleNextAttempt(); // Schedule the next attempt with an increased delay.
		}
	}

	private void scheduleNextAttempt() {
		long delay = calculateNextDelay();
		// Ensure the executor service is not shut down
		if (!scheduledExecutorService.isShutdown()) {
			reconnectFuture = scheduledExecutorService.schedule(this::attemptConnect, delay, TimeUnit.SECONDS);
		}
	}

	private long calculateNextDelay() {
		// Implement the adaptive delay calculation here
		long delay = (long) (INITIAL_RECONNECT_DELAY_SECONDS
				* Math.pow(RECONNECT_DELAY_MULTIPLIER, reconnectionAttempt.getAndIncrement()));
		delay = Math.min(delay, MAX_RECONNECT_DELAY_SECONDS); // Ensure delay does not exceed maximum
		return delay;
	}

	/**
	 * Creates the topic prefix in either format.
	 * 
	 * <ul>
	 * <li>topic_prefix/edge/edge_id/
	 * <li>edge/edge_id/
	 * </ul>
	 * 
	 * @param config the {@link Config}
	 * @return the prefix
	 */
	protected static String createTopicPrefix(Config config) {
		final var b = new StringBuilder();
		if (config.topicPrefix() != null && !config.topicPrefix().isBlank()) {
			b //
					.append(config.topicPrefix()) //
					.append("/");
		}
		b //
				.append("edge/") //
				.append(config.clientId()) //
				.append("/");
		return b.toString();
	}

	@Override
	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();
		ThreadPoolUtils.shutdownAndAwaitTermination(scheduledExecutorService, 0);
		disconnectMqttClient();
	}

	private void disconnectMqttClient() {
		if (this.mqttClient != null) {
			try {
				this.mqttClient.disconnect();
				this.mqttClient.close();
			} catch (MqttException e) {
				logWarn(this.log, "Unable to close connection to MQTT broker: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		// nothing to do here
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.sendChannelValuesWorker.collectData();
			break;

		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
			// Send new EdgeConfig
			var config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
			this.publish(ControllerApiMqtt.TOPIC_EDGE_CONFIG, config.toJson().toString(), //
					1 /* QOS */, true /* retain */, new MqttProperties() /* no specific properties */);

			// Trigger sending of all channel values, because a Component might have
			// disappeared
			this.sendChannelValuesWorker.sendValuesOfAllChannelsOnce();
		}
	}

	/**
	 * Publish a message to a topic.
	 *
	 * @param subTopic the MQTT topic. The global MQTT Topic prefix is added in
	 *                 front of this string
	 * @param message  the message
	 * @return true if message was successfully published; false otherwise
	 */
	protected boolean publish(String subTopic, MqttMessage message) {
		var mqttClient = this.mqttClient;
		if (mqttClient == null) {
			return false;
		}
		try {
			mqttClient.publish(this.topicPrefix + subTopic, message);
			return true;
		} catch (MqttException e) {
			this.logWarn(this.log, e.getMessage());
			return false;
		}
	}

	/**
	 * Publish a message to a topic.
	 *
	 * @param subTopic   the MQTT topic. The global MQTT Topic prefix is added in
	 *                   front of this string
	 * @param message    the message; internally translated to a UTF-8 byte array
	 * @param qos        the MQTT QOS
	 * @param retained   the MQTT retained parameter
	 * @param properties the {@link MqttProperties}
	 * @return true if message was successfully published; false otherwise
	 */
	protected boolean publish(String subTopic, String message, int qos, boolean retained, MqttProperties properties) {
		var msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8), qos, retained, properties);
		return this.publish(subTopic, msg);
	}
}
