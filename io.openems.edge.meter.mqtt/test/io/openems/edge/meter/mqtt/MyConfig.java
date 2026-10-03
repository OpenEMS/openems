package io.openems.edge.meter.mqtt;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;
		private String mqttBridgeId = "mqtt0";
		private String topic = "";
		private String[] mapping = {};

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

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
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

		public Builder setMapping(String... mapping) {
			this.mapping = mapping;
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
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public String mqttBridgeId() {
		return this.builder.mqttBridgeId;
	}

	@Override
	public String topic() {
		return this.builder.topic;
	}

	@Override
	public String[] mapping() {
		return this.builder.mapping;
	}
}
