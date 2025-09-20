package io.openems.edge.controller.api.mqtt;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
	private final AtomicInteger reconnectionAttempt = new AtomicInteger(0);
	private final SendChannelValuesWorker sendChannelValuesWorker = new SendChannelValuesWorker(this);
	private final MqttConnector mqttConnector = new MqttConnector();

	protected Config config;

	private volatile ScheduledFuture<?> reconnectFuture = null;
	private String topicPrefix;
	private IMqttClient mqttClient = null;
	private Pattern[] filterList;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	private static boolean hasFilter(Config config) {
		return !(config.filterSpec() == null || config.filterSpec().isBlank());
	}
	
	public ControllerApiMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerApiMqtt.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		this.config = config;

		this.filterList = this.getFilter(config);
		if (this.filterList.length != 0) {
			this.logInfo(this.log, "Enabled filter: '" + config.filterSpec() + "'");
		}
		
		// Publish MQTT messages under the topic "edge/edge0/..."
		this.topicPrefix = createTopicPrefix(config);

		super.activate(context, config.id(), config.alias(), config.enabled());
		
		if (this.isEnabled()) {
			this.scheduleReconnect();;
		}		
	}
	
	private Pattern[] getFilter(Config config) {
		// Expand the filterSpec to the filterList
		if (!hasFilter(config)) {
			return new Pattern[]{};
		}
		
		String[] buildList = config.filterSpec().split(";");
		final var newFilter = new Pattern[buildList.length];
		for (int i = 0; i < buildList.length; i++) {
			try {
				newFilter[i] = Pattern.compile(//
						"^" + buildList[i].replace("+", "[^/]+").replace("#", ".+") + "$"//
				);
			} catch (PatternSyntaxException e) {
				this.logWarn(this.log, "Unable to parse filter pattern: '" + buildList[i] + "'! Filter disabled!");
				this.log.warn(e.getMessage(), e);
				return new Pattern[]{};
			}
		}
		return newFilter;
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
	protected void deactivate() {
		super.deactivate();
		shutdownAndAwaitTermination(this.scheduledExecutorService, 0);

		if (this.mqttClient != null) {
			try {
				this.mqttClient.disconnect();
				this.mqttClient.close();
				this.mqttClient = null;
			} catch (MqttException e) {
				this.logWarn(this.log, "Unable to close connection to MQTT broker: " + e.getMessage());
				this.log.warn(e.getMessage(), e);
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
		super.logWarn(log, message);
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
			this.publish(ControllerApiMqtt.TOPIC_EDGE_CONFIG + "/", config.toJson().toString(), //
					1 /* QOS */, true /* retain */, new MqttProperties() /* no specific properties */);

			// Trigger sending of all channel values, because a Component might have
			// disappeared
			this.sendChannelValuesWorker.sendValuesOfAllChannelsOnce();
			break;
		}
	}

	/**
	 * Publish a message to a topic.
	 *
	 * @param subTopic the MQTT topic. The global MQTT Topic prefix is added in
	 *                 front of this string
	 * @param message  the message
	 * @return true if message was successfully published (or filtered); false
	 *         otherwise
	 */
	protected MqttPublishStatus publish(String subTopic, MqttMessage message) {
		if (!this.isEnabled()) {
			return MqttPublishStatus.ERROR;
		}
		
		var mqttClient = this.mqttClient;
		if (mqttClient == null) {
			return MqttPublishStatus.ERROR;
		}
		try {
			if (!this.filterTopic(subTopic)) {
				return MqttPublishStatus.FILTERED;
			}
			mqttClient.publish(this.topicPrefix + subTopic, message);
			return MqttPublishStatus.OK;
		} catch (MqttException e) {
			this.logWarn(this.log, e.getMessage());
			return MqttPublishStatus.ERROR;
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
	protected MqttPublishStatus publish(String subTopic, String message, int qos, boolean retained, MqttProperties properties) {
		var msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8), qos, retained, properties);
		return this.publish(subTopic, msg);
	}

	protected boolean filterTopic(String topic) {

		// Skip any attempts to check for empty filters
		if (this.filterList.length == 0) {
			return true;
		}

		// Otherwise filter by each regex until we either run out of patterns, or find a
		// matching one
		for (Pattern pattern : this.filterList) {
			final Matcher m = pattern.matcher(topic);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	private synchronized void scheduleReconnect() {
		if (this.reconnectFuture != null && !this.reconnectFuture.isDone()) {
			this.reconnectFuture.cancel(false);
		}

		this.attemptConnect();
	}

	private void attemptConnect() {
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			return; // Already connected
		}
		try {
			this.mqttConnector
					.connect(this.config.uri(), this.config.clientId(), this.config.username(), this.config.password(),
							this.config.certPem(), this.config.privateKeyPem(), this.config.trustStorePem())
					.thenAccept(client -> {
						this.mqttClient = client;
						this.logInfo(this.log, "Connected to MQTT Broker [" + this.config.uri() + "]! Publish to topics '" + this.topicPrefix + "#'");
						this.reconnectionAttempt.set(0); // Reset on successful connection.
					}) //
					.exceptionally(ex -> {
						this.log.error("Failed to connect to MQTT broker: " + ex.getMessage(), ex);
						this.scheduleNextAttempt(); // Schedule the next attempt with an increased delay.
						return null;
					});
		} catch (Exception e) {
			this.log.error("Error attempting to connect to MQTT broker", e);
			this.scheduleNextAttempt(); // Schedule the next attempt with an increased delay.
		}
	}

	private void scheduleNextAttempt() {
		long delay = this.calculateNextDelay();
		// Ensure the executor service is not shut down
		if (!this.scheduledExecutorService.isShutdown()) {
			this.reconnectFuture = this.scheduledExecutorService.schedule(this::attemptConnect, delay,
					TimeUnit.SECONDS);
		}
	}

	private long calculateNextDelay() {
		long delay = (long) (INITIAL_RECONNECT_DELAY_SECONDS
				* Math.pow(RECONNECT_DELAY_MULTIPLIER, this.reconnectionAttempt.getAndIncrement()));
		delay = Math.min(delay, MAX_RECONNECT_DELAY_SECONDS); // Ensure delay does not exceed maximum
		return delay;
	}

}