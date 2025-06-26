package io.openems.edge.controller.ess.cycle;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private CycleOrder cycleOrder;
		private int standbyTime;
		private String startTime;
		private int maxSoc;
		private int minSoc;
		private int power;
		private Mode mode;
		private HybridEssMode hybridEssMode;
		private int totalCycleNumber;
		private int finalSoc;

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

		public Builder setCycleOrder(CycleOrder cycleOrder) {
			this.cycleOrder = cycleOrder;
			return this;
		}

		public Builder setStandbyTime(int standbyTime) {
			this.standbyTime = standbyTime;
			return this;
		}

		public Builder setStartTime(String startTime) {
			this.startTime = startTime;
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

		public Builder setTotalCycleNumber(int totalCycleNumber) {
			this.totalCycleNumber = totalCycleNumber;
			return this;
		}

		public Builder setFinalSoc(int finalSoc) {
			this.finalSoc = finalSoc;
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
	public CycleOrder cycleOrder() {
		return this.builder.cycleOrder;
	}

	@Override
	public int standbyTime() {
		return this.builder.standbyTime;
	}

	@Override
	public String startTime() {
		return this.builder.startTime;
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
	public int totalCycleNumber() {
		return this.builder.totalCycleNumber;
	}

	@Override
	public int finalSoc() {
		return this.builder.finalSoc;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}
}