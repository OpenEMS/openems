package io.openems.edge.controller.evcs.fixactivepower;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String evcsId;
		private int power;
		private int updateFrequency;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEvcsId(String evcsId) {
			this.evcsId = evcsId;
			return this;
		}

		public Builder setPower(int power) {
			this.power = power;
			return this;
		}

		public Builder setUpdateFrequency(int updateFrequency) {
			this.updateFrequency = updateFrequency;
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
	public String evcs_id() {
		return this.builder.evcsId;
	}

	@Override
	public int power() {
		return this.builder.power;
	}

	@Override
	public int updateFrequency() {
		return this.builder.updateFrequency;
	}
}