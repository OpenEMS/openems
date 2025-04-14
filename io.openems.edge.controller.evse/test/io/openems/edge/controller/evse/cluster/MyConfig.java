package io.openems.edge.controller.evse.cluster;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private DistributionStrategy distributionStrategy;
		private boolean debugMode;
		private String[] ctrlIds;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDistributionStrategy(DistributionStrategy distributionStrategy) {
			this.distributionStrategy = distributionStrategy;
			return this;
		}
		
		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setCtrlIds(String... ctrlIds) {
			this.ctrlIds = ctrlIds;
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
	public DistributionStrategy distributionStrategy() {
		return this.builder.distributionStrategy;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String[] ctrl_ids() {
		return this.builder.ctrlIds;
	}

	@Override
	public String ctrls_target() {
		return generateReferenceTargetFilter(this.id(), this.ctrl_ids());
	}
}
