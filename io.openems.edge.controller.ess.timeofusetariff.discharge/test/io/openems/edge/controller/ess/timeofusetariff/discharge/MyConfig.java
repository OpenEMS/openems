package io.openems.edge.controller.ess.timeofusetariff.discharge;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private Mode mode;
		private int maxStartHour;
		private int maxEndHour;
		private DelayDischargeRiskLevel delayDischargeRiskLevel;

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

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setMaxStartHour(int maxStartHour) {
			this.maxStartHour = maxStartHour;
			return this;
		}

		public Builder setMaxEndHour(int maxEndHour) {
			this.maxEndHour = maxEndHour;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}

		public Builder setDelayDischargeRiskLevel(DelayDischargeRiskLevel delayDischargeRiskLevel) {
			this.delayDischargeRiskLevel = delayDischargeRiskLevel;
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
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public int maxStartHour() {
		return this.builder.maxStartHour;
	}

	@Override
	public int maxEndHour() {
		return this.builder.maxEndHour;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

	@Override
	public DelayDischargeRiskLevel delayDischargeRiskLevel() {
		return this.builder.delayDischargeRiskLevel;
	}
}