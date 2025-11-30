package io.openems.edge.evcs.openwb;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String mqttUri = "tcp://127.0.0.1:1883";
		private String mqttUsername = "";
		private String mqttPassword = "";
		private ChargePoint chargePoint = ChargePoint.CP0;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMqttUri(String mqttUri) {
			this.mqttUri = mqttUri;
			return this;
		}

		public Builder setMqttUsername(String mqttUsername) {
			this.mqttUsername = mqttUsername;
			return this;
		}

		public Builder setMqttPassword(String mqttPassword) {
			this.mqttPassword = mqttPassword;
			return this;
		}

		public Builder setChargePoint(ChargePoint chargePoint) {
			this.chargePoint = chargePoint;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
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
	public String mqttUri() {
		return this.builder.mqttUri;
	}

	@Override
	public String mqttUsername() {
		return this.builder.mqttUsername;
	}

	@Override
	public String mqttPassword() {
		return this.builder.mqttPassword;
	}

	@Override
	public ChargePoint chargePoint() {
		return this.builder.chargePoint;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}
}
