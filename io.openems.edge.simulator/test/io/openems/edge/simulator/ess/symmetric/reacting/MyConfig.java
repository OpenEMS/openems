package io.openems.edge.simulator.ess.symmetric.reacting;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.sum.GridMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private Integer maxApparentPower = null;
		private Integer capacity = null;
		private Integer initialSoc = null;
		private GridMode gridMode = null;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMaxApparentPower(int maxApparentPower) {
			this.maxApparentPower = maxApparentPower;
			return this;
		}

		public Builder setCapacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		public Builder setInitialSoc(int initialSoc) {
			this.initialSoc = initialSoc;
			return this;
		}

		public Builder setGridMode(GridMode gridMode) {
			this.gridMode = gridMode;
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
	public int maxApparentPower() {
		return this.builder.maxApparentPower;
	}

	@Override
	public int capacity() {
		return this.builder.capacity;
	}

	@Override
	public int initialSoc() {
		return this.builder.initialSoc;
	}

	@Override
	public GridMode gridMode() {
		return this.builder.gridMode;
	}

}