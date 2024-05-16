package io.openems.edge.core.meta;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.currency.CurrencyConfig;
import io.openems.edge.common.meta.Meta;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {

		private CurrencyConfig currency;

		private Builder() {
		}

		public Builder setCurrency(CurrencyConfig currency) {
			this.currency = currency;
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
		super(Config.class, Meta.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public CurrencyConfig currency() {
		return this.builder.currency;
	}

}
