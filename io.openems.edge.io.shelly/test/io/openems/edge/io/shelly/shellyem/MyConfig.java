package io.openems.edge.io.shelly.shellyem;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String ip;
		private MeterType type;
		private SinglePhase phase;
		private Boolean sumEmeter1AndEmeter2;
		private int channel;

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

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setSumEmeter1AndEmeter2(boolean sumEmeter1AndEmeter2) {
			this.sumEmeter1AndEmeter2 = sumEmeter1AndEmeter2;
			return this;
		}

		public Builder setChannel(int channel) {
			this.channel = channel;
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
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public boolean sumEmeter1AndEmeter2() {
		return this.builder.sumEmeter1AndEmeter2;
	}

	@Override
	public int channel() {
		return this.builder.channel;
	}
}