package io.openems.edge.bridge.mqtt;

import io.openems.common.utils.ConfigUtils;
import org.osgi.framework.Constants;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.AbstractOpenEmsMqttComponent;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyConfigurationAdmin.DummyConfiguration;

public abstract class DummyMqttComponent extends AbstractOpenEmsMqttComponent implements MqttComponent {

	public DummyMqttComponent(String id, BridgeMqtt bridge, OpenemsComponent referenceComponent)
			throws OpenemsException {
		super(OpenemsComponent.ChannelId.values(), //
				MqttComponent.ChannelId.values()//
		);
		this.channels().forEach(Channel::nextProcessImage);
		this.setMqtt(bridge);
		this.setReferencedComponent(referenceComponent);
		var context = new DummyComponentContext();
		context.addProperty(Constants.SERVICE_PID, Constants.SERVICE_PID);
		var cm = new DummyConfigurationAdmin();
		var dummyConfiguration = new DummyConfiguration();
		dummyConfiguration.addProperty("Mqtt.target",
				ConfigUtils.generateReferenceTargetFilter(Constants.SERVICE_PID, bridge.id()));
		dummyConfiguration.addProperty("ReferenceComponent.target",
				ConfigUtils.generateReferenceTargetFilter(Constants.SERVICE_PID, referenceComponent.id()));
		cm.addConfiguration(Constants.SERVICE_PID, dummyConfiguration);
		super.activate(context, id, "", true, cm, bridge.id(), referenceComponent.id());
	}

	@Override
	protected abstract MqttProtocol defineMqttProtocol() throws OpenemsException;

}
