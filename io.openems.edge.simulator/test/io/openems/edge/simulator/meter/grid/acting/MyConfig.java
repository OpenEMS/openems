package io.openems.edge.simulator.meter.grid.acting;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String datasourceId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDatasourceId(String datasourceId) {
			this.datasourceId = datasourceId;
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
	public String datasource_id() {
		return this.builder.datasourceId;
	}

	@Override
	public String datasource_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.datasource_id());
	}

}