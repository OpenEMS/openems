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

		public Builder setP(int p) {
			this.p = p;
			return this;
		}

		public Builder setI(int i) {
			this.i = i;
			return this;
		}

		public Builder setD(int d) {
			this.d = d;
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

}