package io.openems.edge.mqtt.component.subscribe;

import java.util.Arrays;

import org.osgi.service.cm.ConfigurationAdmin;
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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.AbstractOpenEmsMqttComponent;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttProtocol;
import io.openems.edge.bridge.mqtt.api.Topic;
import io.openems.edge.bridge.mqtt.api.payloads.GenericPayloadImpl;
import io.openems.edge.bridge.mqtt.api.payloads.Payload;
import io.openems.edge.bridge.mqtt.api.task.MqttSubscribeTaskImpl;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * An Implementation of the {@link AbstractOpenEmsMqttComponent}. This class
 * provides the basic ability to subscribe to a topic and map values from the
 * broker to any OpenEmsComponent/OpenEmsComponent Channel In this Case in the
 * style of a
 * {@link Payload}
 * This is possible, due to configuring which Channel listens to a certain
 * keyword. See
 * {@link Payload} for an
 * example Payload. E.g. when the Payload contains "Consumption" and your
 * OpenEmsComponent listens to this keyword with its channel "ActivePower" the
 * value received within Consumption is set to ActivePower channel.
 */

@Designate(//
		ocd = GenericSubscribeConfig.class, //
		factory = true //
)
@Component(//
		name = "Mqtt.Subscribe.Generic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GenericSubscribeImpl extends AbstractOpenEmsMqttComponent implements OpenemsComponent, MqttComponent {

	private GenericSubscribeConfig config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setReferenceComponent(OpenemsComponent referencedComponent) {
		super.setReferenceComponent(referencedComponent);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMqtt(BridgeMqtt modbus) {
		super.setMqtt(modbus);
	}

	public GenericSubscribeImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				MqttComponent.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, GenericSubscribeConfig config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, config.mqtt_id(),
				config.referencedComponent_id())) {
			return;
		}
		super.updateChannelInConfig(this.config.channels().length, this.cm);

	}

	@Modified
	void modified(ComponentContext context, GenericSubscribeConfig config) throws OpenemsException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), this.cm, config.mqtt_id(),
				config.referencedComponent_id())) {
			return;
		}
		super.updateChannelInConfig(this.config.channels().length, this.cm);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected MqttProtocol defineMqttProtocol() throws OpenemsException {

		return new MqttProtocol(this, new MqttSubscribeTaskImpl(new Topic(this.config.topic(), //
				new GenericPayloadImpl(super.createMap(Arrays.stream(this.config.keyToChannel()).toList()), ""))));
	}
}
