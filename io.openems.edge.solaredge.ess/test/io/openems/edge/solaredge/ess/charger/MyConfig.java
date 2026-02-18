package io.openems.edge.solaredge.ess.charger;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.solaredge.ess.charger.Config;
import io.openems.edge.solaredge.ess.charger.MyConfig;
import io.openems.edge.solaredge.ess.charger.MyConfig.Builder;
import io.openems.edge.solaredge.ess.enums.ControlMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String essInverter;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssInverterId(String essInverter) {
			this.essInverter = essInverter;
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
	public String essInverter_id() {
		return this.builder.essInverter;
	}

	@Override
	public String essInverter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.essInverter_id());
	}

}