package io.openems.edge.mqtt.component.publish;

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
import io.openems.edge.bridge.mqtt.api.task.MqttPublishTaskImpl;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * An Implementation of the {@link AbstractOpenEmsMqttComponent}. This class
 * provides the basic ability to publish any OpenEmsComponent/OpenEmsComponent
 * Channel by providing a Config where a Channel is mapped to a Keyword. See
 * {@link Payload} for an
 * example Payload.
 */

@Designate(//
		ocd = GenericPublishConfig.class, //
		factory = true //
)
@Component(//
		name = "Mqtt.Publish.Generic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GenericPublishImpl extends AbstractOpenEmsMqttComponent implements OpenemsComponent, MqttComponent {

	@Reference
	private ConfigurationAdmin cm;
	private GenericPublishConfig config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setReferencedComponent(OpenemsComponent referencedComponent) {
		super.setReferencedComponent(referencedComponent);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMqtt(BridgeMqtt modbus) {
		super.setMqtt(modbus);
	}

	public GenericPublishImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				MqttComponent.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, GenericPublishConfig config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, config.mqtt_id(),
				config.referencedComponent_id())) {
			return;
		}
		super.updateChannelInConfig(this.config.channels().length, this.cm);
	}

	@Modified
	void modified(ComponentContext context, GenericPublishConfig config) throws OpenemsException {
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
		return new MqttProtocol(this, new MqttPublishTaskImpl(new Topic(this.config.topic(), //
				this.config.publishIntervalSeconds().qos, //
				new GenericPayloadImpl(super.createMap(Arrays.stream(this.config.keyToChannel()).toList()),
						this.config.deviceId())),
				this.config.publishIntervalSeconds().interval));
	}
}
