package io.openems.edge.controller.timeslotpeakshaving;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private String startDate;
		private String endDate;
		private String endTime;
		private boolean monday;
		private boolean tuesday;
		private boolean wednesday;
		private boolean thursday;
		private boolean friday;
		private boolean saturday;
		private boolean sunday;
		private int peakShavingPower;
		private int rechargePower;
		private String slowChargeStartTime;
		private int slowChargePower;
		private int hysteresisSoc;
		private String startTime;

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

		public Builder setStartDate(String startDate) {
			this.startDate = startDate;
			return this;
		}

		public Builder setEndDate(String endDate) {
			this.endDate = endDate;
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

		public Builder setMonday(boolean monday) {
			this.monday = monday;
			return this;
		}

		public Builder setTuesday(boolean tuesday) {
			this.tuesday = tuesday;
			return this;
		}

		public Builder setWednesday(boolean wednesday) {
			this.wednesday = wednesday;
			return this;
		}

		public Builder setThursday(boolean thursday) {
			this.thursday = thursday;
			return this;
		}

		public Builder setFriday(boolean friday) {
			this.friday = friday;
			return this;
		}

		public Builder setSaturday(boolean saturday) {
			this.saturday = saturday;
			return this;
		}

		public Builder setSunday(boolean sunday) {
			this.sunday = sunday;
			return this;
		}

		public Builder setPeakShavingPower(int peakShavingPower) {
			this.peakShavingPower = peakShavingPower;
			return this;
		}

		public Builder setRechargePower(int rechargePower) {
			this.rechargePower = rechargePower;
			return this;
		}

		public Builder setSlowChargeStartTime(String slowChargeStartTime) {
			this.slowChargeStartTime = slowChargeStartTime;
			return this;
		}

		public Builder setSlowChargePower(int slowChargePower) {
			this.slowChargePower = slowChargePower;
			return this;
		}

		public Builder setHysteresisSoc(int hysteresisSoc) {
			this.hysteresisSoc = hysteresisSoc;
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
		return this.builder.essId;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
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
	public boolean monday() {
		return this.builder.monday;
	}

	@Override
	public boolean tuesday() {
		return this.builder.tuesday;
	}

	@Override
	public boolean wednesday() {
		return this.builder.wednesday;
	}

	@Override
	public boolean thursday() {
		return this.builder.thursday;
	}

	@Override
	public boolean friday() {
		return this.builder.friday;
	}

	@Override
	public boolean saturday() {
		return this.builder.saturday;
	}

	@Override
	public boolean sunday() {
		return this.builder.sunday;
	}

	@Override
	public int peakShavingPower() {
		return this.builder.peakShavingPower;
	}

	@Override
	public int rechargePower() {
		return this.builder.rechargePower;
	}

	@Override
	public String slowChargeStartTime() {
		return this.builder.slowChargeStartTime;
	}

	@Override
	public int slowChargePower() {
		return this.builder.slowChargePower;
	}

	@Override
	public int hysteresisSoc() {
		return this.builder.hysteresisSoc;
	}

}