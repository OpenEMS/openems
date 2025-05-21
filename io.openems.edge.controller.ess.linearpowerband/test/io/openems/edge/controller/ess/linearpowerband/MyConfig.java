package io.openems.edge.controller.ess.linearpowerband;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int minPower;
		private int maxPower;
		private int adjustPower;
		private StartDirection startDirection;

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

		public Builder setMinPower(int minPower) {
			this.minPower = minPower;
			return this;
		}

		public Builder setMaxPower(int maxPower) {
			this.maxPower = maxPower;
			return this;
		}

		public Builder setAdjustPower(int adjustPower) {
			this.adjustPower = adjustPower;
			return this;
		}

		public Builder setStartDirection(StartDirection startDirection) {
			this.startDirection = startDirection;
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
	public int minPower() {
		return this.builder.minPower;
	}

	@Override
	public int maxPower() {
		return this.builder.maxPower;
	}

	@Override
	public int adjustPower() {
		return this.builder.adjustPower;
	}

	@Override
	public StartDirection startDirection() {
		return this.builder.startDirection;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}
}