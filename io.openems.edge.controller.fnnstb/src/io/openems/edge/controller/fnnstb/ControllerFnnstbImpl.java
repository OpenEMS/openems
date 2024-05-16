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
import com.google.gson.JsonArray;
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

	private final Logger log = LoggerFactory.getLogger(ControllerFnnstbImpl.class);

	private Config config = null;
	private MqttClient client;
	private MqttConnectionManager connectionManager;
	protected boolean signalValue = false;
	protected PowerInfo powerLimitInfo;
	protected PayloadType type;

	record Node(double magF, long t, int unitsMultiplier) {
	}

	private enum PayloadType {
		SIGNAL, NODES, SCHEDULE
	}

	public static class PowerInfo {

		public int maxConsumptionPower = 0; //
		public int MaxProductionPower = 0; //
		public int PowerLimit = 0; //
		public JsonArray PowerLimitSchedule;
		public int ScaleFactor = 0;

		public PowerInfo(int maxConsumptionPower, int maxProductionPower, int powerLimit, JsonArray powerLimitSchedule,
				int scaleFactor) {
			this.maxConsumptionPower = maxConsumptionPower;
			this.MaxProductionPower = maxProductionPower;
			this.PowerLimit = powerLimit;
			this.PowerLimitSchedule = powerLimitSchedule;
			this.ScaleFactor = scaleFactor;

		}

		@Override
		public String toString() {
			return "PowerInfo{" + "maxConsumptionPower=" + this.maxConsumptionPower + ", MaxProductionPower="
					+ this.MaxProductionPower + ", PowerLimit=" + this.PowerLimit + ", PowerLimitSchedule="
					+ this.PowerLimitSchedule + ", ScaleFactor=" + this.ScaleFactor + '}';
		}

	}

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
		// Connect to mqtt
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
		if (this.type == null) {
			return;
		}
		switch (this.type) {
		case SIGNAL -> this.ess.setActivePowerGreaterOrEquals(-4200);
		case SCHEDULE -> this.ess.setActivePowerGreaterOrEquals(-this.powerLimitInfo.PowerLimit);
		case NODES -> {
		}
		}

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.client == null) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CONFIG_UPDATE -> this.subscriber(this.client, this.config.topicName());
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
				ControllerFnnstbImpl.this.setConnectionStatus(true);
				ControllerFnnstbImpl.this.log.info("Connection complete to the broker : " + serverUrl);
			}

			@Override
			public void deliveryComplete(IMqttToken token) {
				ControllerFnnstbImpl.this.log.info("Delivery Complete");
			}

			@Override
			public void disconnected(MqttDisconnectResponse disconnectResponse) {
				ControllerFnnstbImpl.this.setConnectionStatus(false);
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

	protected void parseMessage(String jsonString) {
		Gson gson = new Gson();
		JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

		this.type = this.getPayloadType(jsonObject);

		switch (this.type) {
		case SIGNAL -> this.parseSignal(jsonObject);
		case NODES -> this.parseNodes(jsonObject);
		case SCHEDULE -> this.parseSchedule(jsonObject);
		}
	}

	private PayloadType getPayloadType(JsonObject jsonObject) {
		if (jsonObject.has("signal")) {
			return PayloadType.SIGNAL;
		} else {
			return PayloadType.SCHEDULE;
		}
	}

	private void parseSignal(JsonObject jsonObject) {
		this.signalValue = jsonObject.get("signal").getAsBoolean();
		this.log.info("Signal: " + this.signalValue);
	}

	private void parseNodes(JsonObject jsonObject) {
		JsonObject totwObject = jsonObject.getAsJsonObject("totw");
		Node totwNode = this.parseNode(totwObject, "totw");

		this.log.info("totw" + ":");
		this.log.info("  mag_f: " + totwNode.magF);
		this.log.info("  t: " + totwNode.t);
		this.log.info("  units_multiplier: " + totwNode.unitsMultiplier);

		JsonObject hzObject = jsonObject.getAsJsonObject("hz");
		Node hzNode = this.parseNode(hzObject, "hz");

		this.log.info("hz" + ":");
		this.log.info("  mag_f: " + hzNode.magF);
		this.log.info("  t: " + hzNode.t);
		this.log.info("  units_multiplier: " + hzNode.unitsMultiplier);
	}

	/*
	 * { "MaxConsumptionPower": 8, "MaxProductionPower": 0, "PowerLimit": 5,
	 * "PowerLimitSchedule": [ [ 1714122924277, 5 ], [ 1714122924277, 0 ] ],
	 * "ScaleFactor": 3 }
	 */

	private void parseSchedule(JsonObject jsonObject) {

		var maxConsumptionPower = jsonObject.get("MaxConsumptionPower").getAsInt();
		var maxProductionPower = jsonObject.get("MaxProductionPower").getAsInt();
		var powerLimit = jsonObject.get("PowerLimit").getAsInt();
		var powerLimitSchedule = jsonObject.get("PowerLimitSchedule").getAsJsonArray();
		var scaleFactor = jsonObject.get("ScaleFactor").getAsInt();

		var powerInfo = new PowerInfo(maxConsumptionPower, maxProductionPower, powerLimit, powerLimitSchedule,
				scaleFactor);

		this.powerLimitInfo = powerInfo;
		System.out.println(powerInfo.toString());

	}

	/**
	 * Parses a JsonObject representing a node in the JSON structure. Example JSON :
	 * 
	 * <pre>
	 * {
	 *    "totw": {
	 *        "mag_f": 123.456,
	 *        "t": 1234567890,
	 *        "units_multiplier": 1
	 *    },
	 *    "hz": {
	 *        "mag_f": 50.0,
	 *        "t": 1234567890,        
	 *        "units_multiplier": 1
	 *    }
	 * }
	 * </pre>
	 *
	 * @param node     The JsonObject representing the node to parse.
	 * @param nodeName The name of the node being parsed.
	 * @return Node type of node
	 */
	private Node parseNode(JsonObject node, String nodeName) {
		double magF = node.get("mag_f").getAsDouble();
		long t = node.get("t").getAsLong();
		int unitsMultiplier = node.get("units_multiplier").getAsInt();
		return new Node(magF, t, unitsMultiplier);
	}
}
