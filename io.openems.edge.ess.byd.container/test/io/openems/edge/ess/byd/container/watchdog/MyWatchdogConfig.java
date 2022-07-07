package io.openems.edge.ess.byd.container.watchdog;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyWatchdogConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;

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

		public MyWatchdogConfig build() {
			return new MyWatchdogConfig(this);
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

	private MyWatchdogConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

}