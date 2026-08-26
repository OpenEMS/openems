package io.openems.edge.controller.ess.fixactivepower;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.controller.ess.fixactivepower.enums.HybridEssMode;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.ess.power.api.Relationship;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int power;
		private Mode mode;
		private HybridEssMode hybridEssMode;
		private SingleOrAllPhase phase;
		private Relationship relationship;
		private int chargeOncePower;
		private boolean chargeOnceTargetSocEnable;
		private int chargeOnceTargetSoc;
		private int dischargeOncePower;
		private boolean dischargeOnceTargetSocEnable;
		private int dischargeOnceTargetSoc;
		private boolean ignoreSystemLimitsPermissionsOnce;
		private boolean considerSystemLimits;

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

		public Builder setPower(int power) {
			this.power = power;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setHybridEssMode(HybridEssMode hybridEssMode) {
			this.hybridEssMode = hybridEssMode;
			return this;
		}

		public Builder setPhase(SingleOrAllPhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setRelationship(Relationship relationship) {
			this.relationship = relationship;
			return this;
		}

		public Builder setChargeOncePower(int chargeOncePower) {
			this.chargeOncePower = chargeOncePower;
			return this;
		}

		public Builder setChargeOnceTargetSocEnable(boolean chargeOnceTargetSocEnable) {
			this.chargeOnceTargetSocEnable = chargeOnceTargetSocEnable;
			return this;
		}

		public Builder setChargeOnceTargetSoc(int chargeOnceTargetSoc) {
			this.chargeOnceTargetSoc = chargeOnceTargetSoc;
			return this;
		}

		public Builder setDischargeOncePower(int dischargeOncePower) {
			this.dischargeOncePower = dischargeOncePower;
			return this;
		}

		public Builder setDischargeOnceTargetSocEnable(boolean dischargeOnceTargetSocEnable) {
			this.dischargeOnceTargetSocEnable = dischargeOnceTargetSocEnable;
			return this;
		}

		public Builder setDischargeOnceTargetSoc(int dischargeOnceTargetSoc) {
			this.dischargeOnceTargetSoc = dischargeOnceTargetSoc;
			return this;
		}

		public Builder setIgnoreSystemLimitsPermissionsOnce(boolean ignoreSystemLimitsPermissionsOnce) {
			this.ignoreSystemLimitsPermissionsOnce = ignoreSystemLimitsPermissionsOnce;
			return this;
		}

		public Builder setConsiderSystemLimits(boolean considerSystemLimits) {
			this.considerSystemLimits = considerSystemLimits;
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
	public int power() {
		return this.builder.power;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public HybridEssMode hybridEssMode() {
		return this.builder.hybridEssMode;
	}

	@Override
	public Relationship relationship() {
		return this.builder.relationship;
	}

	@Override
	public SingleOrAllPhase phase() {
		return this.builder.phase;
	}

	@Override
	public int chargeOncePower() {
		return this.builder.chargeOncePower;
	}

	@Override
	public boolean chargeOnceTargetSocEnable() {
		return this.builder.chargeOnceTargetSocEnable;
	}

	@Override
	public int chargeOnceTargetSoc() {
		return this.builder.chargeOnceTargetSoc;
	}

	@Override
	public int dischargeOncePower() {
		return this.builder.dischargeOncePower;
	}

	@Override
	public boolean dischargeOnceTargetSocEnable() {
		return this.builder.dischargeOnceTargetSocEnable;
	}

	@Override
	public int dischargeOnceTargetSoc() {
		return this.builder.dischargeOnceTargetSoc;
	}

	@Override
	public boolean ignoreSystemLimitsPermissionsOnce() {
		return this.builder.ignoreSystemLimitsPermissionsOnce;
	}

	@Override
	public boolean considerSystemLimits() {
		return this.builder.considerSystemLimits;
	}
}
