package io.openems.edge.simulator.datasource.single.direct;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int timeDelta;
		private int[] values;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setTimeDelta(int timeDelta) {
			this.timeDelta = timeDelta;
			return this;
		}

		public Builder setValues(int... values) {
			this.values = values;
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
	public int timeDelta() {
		return this.builder.timeDelta;
	}

	@Override
	public int[] values() {
		return this.builder.values;
	}

}