package io.openems.edge.io.shelly.shellypro2pm;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.DebugMode;
import io.openems.edge.common.type.Phase;

@SuppressWarnings("all")
public class MyDeviceConfig extends AbstractComponentConfig implements DeviceConfig {

	protected static class Builder {
		private String id;
		private String ip;
		private String mdnsName = "";
		private Phase.SinglePhase phase;
		private DebugMode debugMode = DebugMode.OFF;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setMdnsName(String mdnsName) {
			this.mdnsName = mdnsName;
			return this;
		}

		public Builder setPhase(Phase.SinglePhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setDebugMode(DebugMode debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public MyDeviceConfig build() {
			return new MyDeviceConfig(this);
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

	private MyDeviceConfig(Builder builder) {
		super(DeviceConfig.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public String mdnsName() {
		return this.builder.mdnsName;
	}

	@Override
	public Phase.SinglePhase phase() {
		return this.builder.phase;
	}

	@Override
	public DebugMode debugMode() {
		return this.builder.debugMode;
	}
}