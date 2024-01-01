package io.openems.edge.core.sum;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.sum.Sum;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {

		private int gridMinActivePower;
		private int gridMaxActivePower;
		private int productionMaxActivePower;
		private int consumptionMaxActivePower;
		private String[] ignoreStateComponents;

		private Builder() {
		}

		public Builder setGridMinActivePower(int gridMinActivePower) {
			this.gridMinActivePower = gridMinActivePower;
			return this;
		}

		public Builder setGridMaxActivePower(int gridMaxActivePower) {
			this.gridMaxActivePower = gridMaxActivePower;
			return this;
		}

		public Builder setProductionMaxActivePower(int productionMaxActivePower) {
			this.productionMaxActivePower = productionMaxActivePower;
			return this;
		}

		public Builder setConsumptionMaxActivePower(int consumptionMaxActivePower) {
			this.consumptionMaxActivePower = consumptionMaxActivePower;
			return this;
		}

		public Builder setIgnoreStateComponents(String... ignoreStateComponents) {
			this.ignoreStateComponents = ignoreStateComponents;
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
		super(Config.class, Sum.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public int gridMinActivePower() {
		return this.builder.gridMinActivePower;
	}

	@Override
	public int gridMaxActivePower() {
		return this.builder.gridMaxActivePower;
	}

	@Override
	public int productionMaxActivePower() {
		return this.builder.productionMaxActivePower;
	}

	@Override
	public int consumptionMaxActivePower() {
		return this.builder.consumptionMaxActivePower;
	}

	@Override
	public String[] ignoreStateComponents() {
		return this.builder.ignoreStateComponents;
	}

}
