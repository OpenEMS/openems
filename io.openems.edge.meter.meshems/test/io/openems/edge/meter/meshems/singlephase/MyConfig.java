package io.openems.edge.meter.meshems.singlephase;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private SinglePhase phase = SinglePhase.L1;
		private boolean invert = false;
		private String mqttBridgeId = "mqtt0";
		private String topic = "";
		private int staleTimeoutSeconds = 90;

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

		public Builder setPhase(SinglePhase phase) {
			this.phase = phase;
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

		public Builder setStaleTimeoutSeconds(int staleTimeoutSeconds) {
			this.staleTimeoutSeconds = staleTimeoutSeconds;
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
	public SinglePhase phase() {
		return this.builder.phase;
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

	@Override
	public int staleTimeoutSeconds() {
		return this.builder.staleTimeoutSeconds;
	}
}
