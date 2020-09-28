package io.openems.edge.controller.ess.delayedselltogrid;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private String schedule;
		private int delayedSellToGridPower;
		private int chargePower;

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

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		
		public Builder setChargePower(int chargePower) {
			this.chargePower = chargePower;
			return this;
		}

		
		public Builder setDelayedSellToGridPower(int delayedSellToGridPower) {
			this.delayedSellToGridPower= delayedSellToGridPower;
			return this;
		}

		public Builder setSchedule(String schedule) {
			this.schedule = schedule;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

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
	public int delayedSellToGridPower() {
		return this.builder.delayedSellToGridPower;
	}

	@Override
	public int chargePower() {
		return this.builder.chargePower;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}
}