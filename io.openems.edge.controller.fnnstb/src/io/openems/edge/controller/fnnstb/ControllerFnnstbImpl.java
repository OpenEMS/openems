package io.openems.edge.controller.fnnstb;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.fnnstb.mqtt.MqttConnectionManager;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Fnn.stb", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CONFIG_UPDATE }//
)
public class ControllerFnnstbImpl extends AbstractOpenemsComponent
		implements ControllerFnnstb, Controller, OpenemsComponent, EventHandler {

	private final String topicName = "AnOut_mxVal_f";
	private final Logger log = LoggerFactory.getLogger(ControllerFnnstbImpl.class);

	private Config config = null;
	private MqttClient client;
	private MqttConnectionManager connectionManager;
	protected boolean signalValue = false;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManagedSymmetricEss ess;

	public ControllerFnnstbImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerFnnstb.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.makeConnection(this.config.uri(), this.config.clientId(), this.config.username(), this.config.password(),
				this.config.certPem(), this.config.privateKeyPem(), this.config.trustStorePem());
	}

	private void makeConnection(String broker, String clientId, String username, String password, String certPem,
			String privateKeyPem, String trustStorePem) {
		try {
			this.connectionManager = new MqttConnectionManager(broker, clientId, username, password, certPem,
					privateKeyPem, trustStorePem);
			this.connectionManager.connect();
			this.client = this.connectionManager.getClient();
		} catch (MqttException me) {
			me.printStackTrace();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		try {
			this.connectionManager.disconnect();
		} catch (MqttException me) {
			this.log.error("Error during MQTT disconnection: {}", me.getMessage());
		}
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.signalValue) {
			try {
				this.ess.setActivePowerGreaterOrEquals(-4200);
			} catch (OpenemsNamedException e) {
				this.log.error("Error setting active power: {}", e.getMessage());
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CONFIG_UPDATE -> this.subscriber(this.client, this.topicName);
		}
	}

	private void subscriber(MqttClient client, String topicName) {
		this.setCallback(client);
		int qos = 0;
		try {
			client.subscribe(topicName, qos);
		} catch (MqttException e) {
			e.printStackTrace();
		}
		this.log.info("Subscribed to topic: " + topicName);
	}

	private void setCallback(MqttClient client) {
		client.setCallback(new MqttCallback() {

			@Override
			public void authPacketArrived(int reasonCode, MqttProperties properties) {
				ControllerFnnstbImpl.this.log.info("Auth Packet Arrived " + reasonCode);
			}

			@Override
			public void connectComplete(boolean reconnect, String serverUrl) {
				ControllerFnnstbImpl.this.log.info("Connection complete to the broker : " + serverUrl);
			}

			@Override
			public void deliveryComplete(IMqttToken token) {
				ControllerFnnstbImpl.this.log.info("Delivery Complete");
			}

			@Override
			public void disconnected(MqttDisconnectResponse disconnectResponse) {
				ControllerFnnstbImpl.this.log.info("Disconnected");
			}

			@Override
			public void messageArrived(String topicName, MqttMessage message) throws Exception {
				var payload = new String(message.getPayload());
				ControllerFnnstbImpl.this.parseMessage(payload);

			}

			@Override
			public void mqttErrorOccurred(MqttException exception) {
				ControllerFnnstbImpl.this.log.info("Mqtt Error Occurred ");
			}
		});
	}

	protected void parseMessage(String payload) {
		Gson gson = new Gson();
		JsonObject jsonObject = gson.fromJson(payload, JsonObject.class);
		this.signalValue = jsonObject.get("signal").getAsBoolean();
	}
}
