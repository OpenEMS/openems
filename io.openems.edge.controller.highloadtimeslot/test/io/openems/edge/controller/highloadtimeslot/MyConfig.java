package io.openems.edge.controller.highloadtimeslot;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int hysteresisSoc;
		private int dischargePower;
		private int chargePower;
		private WeekdayFilter weekdayFilter;
		private String endTime;
		private String startTime;
		private String endDate;
		private String startDate;
		private String ess;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setHysteresisSoc(int hysteresisSoc) {
			this.hysteresisSoc = hysteresisSoc;
			return this;
		}

		public Builder setDischargePower(int dischargePower) {
			this.dischargePower = dischargePower;
			return this;
		}

		public Builder setChargePower(int chargePower) {
			this.chargePower = chargePower;
			return this;
		}

		public Builder setWeekdayFilter(WeekdayFilter weekdayFilter) {
			this.weekdayFilter = weekdayFilter;
			return this;
		}

		public Builder setEndDate(String endDate) {
			this.endDate = endDate;
			return this;
		}

		public Builder setStartDate(String startDate) {
			this.startDate = startDate;
			return this;
		}

		public Builder setStartTime(String startTime) {
			this.startTime = startTime;
			return this;
		}

		public Builder setEndTime(String endTime) {
			this.endTime = endTime;
			return this;
		}

		public Builder setEss(String ess) {
			this.ess = ess;
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
	public String ess() {
		return this.builder.ess;
	}

	@Override
	public String startDate() {
		return this.builder.startDate;
	}

	@Override
	public String endDate() {
		return this.builder.endDate;
	}

	@Override
	public String startTime() {
		return this.builder.startTime;
	}

	@Override
	public String endTime() {
		return this.builder.endTime;
	}

	@Override
	public WeekdayFilter weekdayFilter() {
		return this.builder.weekdayFilter;
	}

	@Override
	public int chargePower() {
		return this.builder.chargePower;
	}

	@Override
	public int dischargePower() {
		return this.builder.dischargePower;
	}

	@Override
	public int hysteresisSoc() {
		return this.builder.hysteresisSoc;
	}
}