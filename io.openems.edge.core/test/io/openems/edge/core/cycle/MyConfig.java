package io.openems.edge.core.cycle;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private int cycleTime;

		private Builder() {
		}

		public MyConfig build() {
			return new MyConfig(this);
		}

		public Builder cycleTime(int cycleTime) {
			this.cycleTime = cycleTime;
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

	private MyConfig(Builder builder) {
		super(Config.class, CycleImpl.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public int cycleTime() {
		return this.builder.cycleTime;
	}
}