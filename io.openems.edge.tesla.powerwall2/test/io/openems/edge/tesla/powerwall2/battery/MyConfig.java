package io.openems.edge.tesla.powerwall2.battery;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.ess.api.SinglePhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		public SinglePhase phase;
		public String coreId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setCoreId(String coreId) {
			this.coreId = coreId;
			return this;
		}

		public Builder setPhase(SinglePhase phase) {
			this.phase = phase;
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
	public SinglePhase phase() {
		return this.builder.phase;
	}

	@Override
	public String core_id() {
		return this.builder.coreId;
	}

	@Override
	public String core_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.core_id());
	}
}