package io.openems.edge.mqtt.component.subscribe;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigGenericSubscribe extends AbstractComponentConfig implements GenericSubscribeConfig {

	public MyConfigGenericSubscribe(Builder builder) {
		super(GenericSubscribeConfig.class, builder.id);
		this.builder = builder;
	}

	protected static class Builder {
		private String id;
		private String mqttId;
		private String referencedComponentId;
		private String[] channels;
		private String topic;
		private String[] keyToChanel;

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMqttId(String mqttId) {
			this.mqttId = mqttId;
			return this;
		}

		public Builder setReferencedComponentId(String referencedComponentId) {
			this.referencedComponentId = referencedComponentId;
			return this;
		}

		public Builder setChannels(String[] channels) {
			this.channels = channels;
			return this;
		}

		public Builder setTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder setKeyToChanel(String[] keyToChanel) {
			this.keyToChanel = keyToChanel;
			return this;
		}

		public MyConfigGenericSubscribe build() {
			return new MyConfigGenericSubscribe(this);
		}

	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	@Override
	public String mqtt_id() {
		return this.builder.mqttId;
	}

	@Override
	public String referencedComponent_id() {
		return this.builder.referencedComponentId;
	}

	@Override
	public String[] channels() {
		return this.builder.channels;
	}

	@Override
	public String topic() {
		return this.builder.topic;
	}

	@Override
	public String[] keyToChannel() {
		return this.builder.keyToChanel;
	}

	@Override
	public String ReferenceComponent_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.referencedComponent_id());
	}

	@Override
	public String Mqtt_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.mqtt_id());
	}

}
