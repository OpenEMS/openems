package io.openems.edge.controller.ess.limitusablecapacity;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int stopDischargeSoc = 10;
		private int allowDischargeSoc = 12;
		private int forceChargeSoc = 8;
		private int stopChargeSoc = 90;
		private int allowChargeSoc = 85;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setStopDischargeSoc(int stopDischargeSoc) {
			this.stopDischargeSoc = stopDischargeSoc;
			return this;
		}

		public Builder setAllowDischargeSoc(int allowDischargeSoc) {
			this.allowDischargeSoc = allowDischargeSoc;
			return this;
		}

		public Builder setForceChargeSoc(int forceChargeSoc) {
			this.forceChargeSoc = forceChargeSoc;
			return this;
		}

		public Builder setStopChargeSoc(int stopChargeSoc) {
			this.stopChargeSoc = stopChargeSoc;
			return this;
		}

		public Builder setAllowChargeSoc(int allowChargeSoc) {
			this.allowChargeSoc = allowChargeSoc;
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public int stopDischargeSoc() {
		return this.builder.stopDischargeSoc;
	}

	@Override
	public int allowDischargeSoc() {
		return this.builder.allowDischargeSoc;
	}

	@Override
	public int forceChargeSoc() {
		return this.builder.forceChargeSoc;
	}

	@Override
	public int stopChargeSoc() {
		return this.builder.stopChargeSoc;
	}

	@Override
	public int allowChargeSoc() {
		return this.builder.allowChargeSoc;
	}
	
	@Override
    public String ess_target() {
        return "(&(enabled=true)(!(service.pid=ctrl0))(|(id=" + this.ess_id() + ")))";
    }

}