package io.openems.edge.bridge.mqtt;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.LogVerbosity;
import io.openems.edge.bridge.mqtt.api.MqttData;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.core.timer.TimerManager;

/**
 * Provides a service for connecting to, querying and publish/subscribe to a
 * Broker with the Mqtt Version 3.1.1 .
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Mqtt.Version311", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE })

public class MqttBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, BridgeMqtt {

	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MqttBridgeImpl.class);

	private final MqttWorker worker = new MqttWorker(this);

	private LogVerbosity logVerbosity = LogVerbosity.NONE;

	@Reference
	private ComponentManager cpm;

	@Reference
	private Cycle cycle;
	@Reference
	private TimerManager tm;

	public MqttBridgeImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				BridgeMqtt.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this._setDisabled(!config.enabled());
		this.logVerbosity = config.logVerbosity();
		var mqttData = new MqttData(config.username().trim(), //
				config.password().trim(), //
				config.brokerUrl().trim(), //
				config.userRequired(), MqttConnectOptions.MQTT_VERSION_3_1_1);
		if (this.isEnabled()) {
			this.worker.initialize(mqttData, this.tm);
			this.worker.activate(this.id());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (this.isEnabled()) {
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.worker.onBeforeProcessImage();
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.worker.onExecuteWrite();
			}
		}
	}

	public LogVerbosity getLogVerbosity() {
		return this.logVerbosity;
	}

	@Override
	public void addMqttProtocol(String id, MqttProtocol mqttProtocol) {
		this.worker.addProtocol(id, mqttProtocol);
	}

	@Override
	public void removeMqttProtocol(String sourceId) {
		this.worker.removeMqttProtocol(sourceId);
	}

	@Override
	public Cycle getCycle() {
		return this.cycle;
	}

	@Override
	public ComponentManager getComponentManager() {
		return this.cpm;
	}
}
