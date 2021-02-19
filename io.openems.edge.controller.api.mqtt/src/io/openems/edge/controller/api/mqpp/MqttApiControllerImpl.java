package io.openems.edge.controller.api.mqpp;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.mqtt.MqttConnector;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Api.MQTT", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"org.ops4j.pax.logging.appender.name=Controller.Api.MQTT", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CONFIG_UPDATE //
		} //
)
public class MqttApiControllerImpl extends AbstractOpenemsComponent
		implements MqttApiController, Controller, OpenemsComponent, PaxAppender, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ChannelValuesWorker.class);
	private final ChannelValuesWorker channelValuesWorker = new ChannelValuesWorker(this);
	private final ApiWorker apiWorker = new ApiWorker(this);
	private final MqttConnector mqttConnector = new MqttConnector();

	private String topicPrefix;

	// Used for SubscribeSystemLogRequests
	private boolean isSystemLogSubscribed = false;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	public MqttApiControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				MqttApiController.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	private IMqttClient mqttClient = null;

	@Activate
	void activate(ComponentContext context, Config config) throws Exception {
		// Publish MQTT messages under the topic "edge/edge0/..."
		this.topicPrefix = String.format(MqttApiController.TOPIC_PREFIX, config.clientId());

		super.activate(context, config.id(), config.alias(), config.enabled());
		this.mqttConnector.connect(config.uri(), config.clientId(), config.username(), config.password())
				.thenAccept(client -> {
					this.mqttClient = client;
					System.out.println("Connected");
				});

		// initialize ApiWorker
		this.apiWorker.setTimeoutSeconds(config.apiTimeout());

		// Activate worker
		this.channelValuesWorker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.mqttConnector.deactivate();
		this.channelValuesWorker.deactivate();
		if (this.mqttClient != null) {
			try {
				this.mqttClient.close();
			} catch (MqttException e) {
				this.logWarn(this.log, "Unable to close connection to MQTT brokwer: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logInfo(log, message);
	}

	/**
	 * Activates/deactivates subscription to System-Log.
	 * 
	 * <p>
	 * If activated, all System-Log events are sent to the systemLog topic.
	 * 
	 * @param isSystemLogSubscribed true to activate
	 */
	protected synchronized void setSystemLogSubscribed(boolean isSystemLogSubscribed) {
		this.isSystemLogSubscribed = isSystemLogSubscribed;
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (!this.isSystemLogSubscribed) {
			return;
		}
//		try {
////			this.publish(QUEUE_SYSTEMLOG, SystemLog.fromPaxLoggingEvent(event).toJson().toString());
//		} catch (IOException e) {
//			this.logWarn(this.log, "Unable to send System-Log: " + e.getMessage());
//		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.channelValuesWorker.triggerNextRun();
			break;

		case EdgeEventConstants.TOPIC_CONFIG_UPDATE:
//			EdgeConfig config = (EdgeConfig) event.getProperty(EdgeEventConstants.TOPIC_CONFIG_UPDATE_KEY);
//			EdgeConfigNotification message = new EdgeConfigNotification(config);
//			WebsocketClient ws = this.websocket;
//			if (ws == null) {
//				return;
//			}
//			ws.sendMessage(message);
		}
	}

	/**
	 * Publish a message to a topic.
	 * 
	 * @param subTopic the MQTT topic. The global MQTT Topic prefix is added in
	 *                 front of this string
	 * @param message  the message
	 */
	protected boolean publish(String subTopic, MqttMessage message) {
		IMqttClient mqttClient = this.mqttClient;
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
	 * @param subTopic              the MQTT topic. The global MQTT Topic prefix is
	 *                              added in front of this string
	 * @param messageExpiryInterval The Message Expiry Interval in seconds
	 * @param message               the message
	 */
	protected boolean publish(String subTopic, String message, int qos, boolean retained, MqttProperties properties) {
		MqttMessage msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8), qos, retained, properties);
		return this.publish(subTopic, msg);
	}
}
