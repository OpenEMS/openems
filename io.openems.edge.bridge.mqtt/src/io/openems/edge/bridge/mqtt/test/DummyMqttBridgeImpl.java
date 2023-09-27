package io.openems.edge.bridge.mqtt.test;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.test.DummyCycle;

public class DummyMqttBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, BridgeMqtt {

	private final Map<String, MqttProtocol> protocols = new HashMap<>();

	private final Cycle cycle = new DummyCycle(1000);

	public DummyMqttBridgeImpl(String id) {
		super(OpenemsComponent.ChannelId.values(), //
				BridgeMqtt.ChannelId.values() //
		);
		this.channels().forEach(Channel::nextProcessImage);
		super.activate(null, id, "", true);
	}

	@Override
	public void addMqttProtocol(String id, MqttProtocol mqttProtocol) {
		this.protocols.put(id, mqttProtocol);
	}

	@Override
	public void removeMqttProtocol(String sourceId) {
		this.protocols.remove(sourceId);
	}

	@Override
	public Cycle getCycle() {
		return this.cycle;
	}

	@Override
	public ComponentManager getComponentManager() {
		return null;
	}

}
