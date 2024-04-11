package io.openems.edge.ess.sma.stpxx3se.dccharger;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.ess.sma.enums.PvString;
import io.openems.edge.ess.sma.stpxx3se.dccharger.MyConfig.Builder;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String coreId;
		private PvString pvString;

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
		
		public Builder setPvString(PvString pvString) {
			this.pvString = pvString;
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
	public PvString pvString() {
		return this.builder.pvString;
	}

}
