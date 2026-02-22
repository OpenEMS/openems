package io.openems.edge.ess.core.power;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.ess.power.api.SolverStrategy;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private SolverStrategy strategy;
		private boolean symmetricMode;
		private boolean debugMode;
		private boolean enablePid;
		private double p;
		private double i;
		private double d;
		private boolean enableLowPass;
		private double alpha;

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

		public Builder setEnableLowPass(boolean enableLowPass) {
			this.enableLowPass = enableLowPass;
			return this;
		}

		public Builder setAlpha(double alpha) {
			this.alpha = alpha;
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
	public boolean enableLowPass() {
		return this.builder.enableLowPass;
	}

	@Override
	public double alpha() {
		return this.builder.alpha;
	}

}