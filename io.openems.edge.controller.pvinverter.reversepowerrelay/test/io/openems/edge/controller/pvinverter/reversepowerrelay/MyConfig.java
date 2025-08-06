package io.openems.edge.controller.pvinverter.reversepowerrelay;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias = ""; // Default to empty string
		private boolean enabled = true; // Default to true
		private boolean debugMode = true; // Default to true
		private String pvInverterId;
		private String inputChannelAddress0Percent;
		private String inputChannelAddress30Percent;
		private String inputChannelAddress60Percent;
		private String inputChannelAddress100Percent;
		private int powerLimit100;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}
		
		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}		

		public Builder setPvInverterId(String pvInverterId) {
			this.pvInverterId = pvInverterId;
			return this;
		}

		public Builder setInputChannelAddress0Percent(String inputChannelAddress0Percent) {
			this.inputChannelAddress0Percent = inputChannelAddress0Percent;
			return this;
		}

		public Builder setInputChannelAddress30Percent(String inputChannelAddress30Percent) {
			this.inputChannelAddress30Percent = inputChannelAddress30Percent;
			return this;
		}

		public Builder setInputChannelAddress60Percent(String inputChannelAddress60Percent) {
			this.inputChannelAddress60Percent = inputChannelAddress60Percent;
			return this;
		}

		public Builder setInputChannelAddress100Percent(String inputChannelAddress100Percent) {
			this.inputChannelAddress100Percent = inputChannelAddress100Percent;
			return this;
		}

		public Builder setPowerLimit100(int powerLimit100) {
			this.powerLimit100 = powerLimit100;
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
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}
	
	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}	
	
	@Override
	public String pvInverter_id() {
		return this.builder.pvInverterId;
	}

	@Override
	public String inputChannelAddress0Percent() {
		return this.builder.inputChannelAddress0Percent;
	}

	@Override
	public String inputChannelAddress30Percent() {
		return this.builder.inputChannelAddress30Percent;
	}

	@Override
	public String inputChannelAddress60Percent() {
		return this.builder.inputChannelAddress60Percent;
	}

	@Override
	public String inputChannelAddress100Percent() {
		return this.builder.inputChannelAddress100Percent;
	}

	@Override
	public int powerLimit100() {
		return this.builder.powerLimit100;
	}

}
