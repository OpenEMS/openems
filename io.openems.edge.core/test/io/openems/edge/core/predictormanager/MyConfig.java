package io.openems.edge.core.predictormanager;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String[] predictorIds;

		private Builder() {
		}

		public Builder setPredictorIds(String... predictorIds) {
			this.predictorIds = predictorIds;
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
		super(Config.class, PredictorManagerImpl.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public String[] predictor_ids() {
		return this.builder.predictorIds;
	}
}