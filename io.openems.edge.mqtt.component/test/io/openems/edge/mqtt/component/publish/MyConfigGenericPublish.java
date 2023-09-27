package io.openems.edge.mqtt.component.publish;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.mqtt.component.enums.TimeIntervalSeconds;

@SuppressWarnings("all")
public class MyConfigGenericPublish extends AbstractComponentConfig implements GenericPublishConfig {
	public MyConfigGenericPublish(Builder builder) {
		super(GenericPublishConfig.class, builder.id);
		this.builder = builder;
	}

	protected static class Builder {
		private String id;
		private String mqttId;
		private String referencedComponentId;
		private String[] channels;
		private String topic;
		private String[] keyToChanel;

		private TimeIntervalSeconds interval;

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

		public MyConfigGenericPublish build() {
			return new MyConfigGenericPublish(this);
		}

		public Builder setPubInterval(TimeIntervalSeconds interval) {
			this.interval = interval;
			return this;
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
	public String deviceId() {
		return "test";
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
	public TimeIntervalSeconds publishIntervalSeconds() {
		return this.builder.interval;
	}

	@Override
	public String[] keyToChannel() {
		return this.builder.keyToChanel;
	}

	@Override
	public String ReferencedComponent_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.referencedComponent_id());
	}

	@Override
	public String Mqtt_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.mqtt_id());
	}

}
