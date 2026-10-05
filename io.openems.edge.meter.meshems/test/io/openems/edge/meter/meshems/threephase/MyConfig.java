package io.openems.edge.meter.meshems.threephase;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private boolean invert = false;
		private String mqttBridgeId = "mqtt0";
		private String topic = "";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setInvert(boolean invert) {
			this.invert = invert;
			return this;
		}

		public Builder setMqttBridgeId(String mqttBridgeId) {
			this.mqttBridgeId = mqttBridgeId;
			return this;
		}

		public Builder setTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
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

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

	@Override
	public String mqttBridgeId() {
		return this.builder.mqttBridgeId;
	}

	@Override
	public String topic() {
		return this.builder.topic;
	}
}
