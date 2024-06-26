package io.openems.edge.controller.pvinverter.reversepowerrelay;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
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
	public String pvInverter_id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String inputChannelAddress0Percent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String inputChannelAddress30Percent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String inputChannelAddress60Percent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String inputChannelAddress100Percent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int powerLimit30() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int powerLimit60() {
		// TODO Auto-generated method stub
		return 0;
	}

}