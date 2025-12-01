package io.openems.edge.io.shelly.shellyplusplugs;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SinglePhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String mdnsName = "";
		private String ip;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private SinglePhase phase = SinglePhase.L1;
		private boolean invert = false;
		private DebugMode debugMode = DebugMode.OFF;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPhase(SinglePhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setMdnsName(String mdnsName) {
			this.mdnsName = mdnsName;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
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

		public Builder setDebugMode(DebugMode debugMode) {
			this.debugMode = debugMode;
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
	public SinglePhase phase() {
		return this.builder.phase;
	}

	@Override
	public String mdnsName() {
		return this.builder.mdnsName;
	}

	@Override
	public String ip() {
		return this.builder.ip;
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
	public DebugMode debugMode() {
		return this.builder.debugMode;
	}
}