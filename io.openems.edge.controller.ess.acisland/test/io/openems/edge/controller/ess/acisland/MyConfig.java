package io.openems.edge.controller.ess.acisland;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		public String essId;
		public int maxSoc;
		public int minSoc;
		public int switchDelay;
		public boolean invertOnGridOutput;
		public boolean invertOffGridOutput;
		public String onGridOutputChannelAddress;
		public String offGridOutputChannelAddress;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMaxSoc(int maxSoc) {
			this.maxSoc = maxSoc;
			return this;
		}

		public Builder setMinSoc(int minSoc) {
			this.minSoc = minSoc;
			return this;
		}

		public Builder setSwitchDelay(int switchDelay) {
			this.switchDelay = switchDelay;
			return this;
		}

		public Builder setInvertOffGridOutput(boolean invertOffGridOutput) {
			this.invertOffGridOutput = invertOffGridOutput;
			return this;
		}

		public Builder setInvertOnGridOutput(boolean invertOnGridOutput) {
			this.invertOnGridOutput = invertOnGridOutput;
			return this;
		}

		public Builder setOffGridOutputChannelAddress(String offGridOutputChannelAddress) {
			this.offGridOutputChannelAddress = offGridOutputChannelAddress;
			return this;
		}

		public Builder setOnGridOutputChannelAddress(String onGridOutputChannelAddress) {
			this.onGridOutputChannelAddress = onGridOutputChannelAddress;
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public int maxSoc() {
		return this.builder.maxSoc;
	}

	@Override
	public int minSoc() {
		return this.builder.minSoc;
	}

	@Override
	public int switchDelay() {
		return this.builder.switchDelay;
	}

	@Override
	public boolean invertOnGridOutput() {
		return this.builder.invertOnGridOutput;
	}

	@Override
	public boolean invertOffGridOutput() {
		return this.builder.invertOffGridOutput;
	}

	@Override
	public String onGridOutputChannelAddress() {
		return this.builder.onGridOutputChannelAddress;
	}

	@Override
	public String offGridOutputChannelAddress() {
		return this.builder.offGridOutputChannelAddress;
	}
}