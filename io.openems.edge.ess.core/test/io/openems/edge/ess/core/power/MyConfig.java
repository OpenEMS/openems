package io.openems.edge.ess.core.power;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.ess.power.api.SolverStrategy;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private SolverStrategy strategy;
		private boolean symmetricMode;
		private boolean debugMode;
		private boolean enablePid;
		private double p;
		private double i;
		private double d;
		private boolean enablePT1Filter;
		private int pt1TimeConstant;

		private Builder() {
		}

		public Builder setStrategy(SolverStrategy strategy) {
			this.strategy = strategy;
			return this;
		}

		public Builder setSymmetricMode(boolean symmetricMode) {
			this.symmetricMode = symmetricMode;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setEnablePid(boolean enablePid) {
			this.enablePid = enablePid;
			return this;
		}

		public Builder setP(double p) {
			this.p = p;
			return this;
		}

		public Builder setI(double i) {
			this.i = i;
			return this;
		}

		public Builder setD(double d) {
			this.d = d;
			return this;
		}

		public Builder setEnablePT1Filter(boolean enablePT1Filter) {
			this.enablePT1Filter = enablePT1Filter;
			return this;
		}

		public Builder setPT1TimeConstant(int pt1TimeConstant) {
			this.pt1TimeConstant = pt1TimeConstant;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create the Config-Builder.
	 *
	 * @return the {@link Builder}.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, "_power");
		this.builder = builder;
	}

	@Override
	public SolverStrategy strategy() {
		return this.builder.strategy;
	}

	@Override
	public boolean symmetricMode() {
		return this.builder.symmetricMode;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean enablePid() {
		return this.builder.enablePid;
	}

	@Override
	public double p() {
		return this.builder.p;
	}

	@Override
	public double i() {
		return this.builder.i;
	}

	@Override
	public double d() {
		return this.builder.d;
	}

	@Override
	public boolean enablePT1Filter() {
		return this.builder.enablePT1Filter;
	}

	@Override
	public int pt1TimeConstant() {
		return this.builder.pt1TimeConstant;
	}
}