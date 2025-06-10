package io.openems.edge.controller.ess.standby;

import java.time.DayOfWeek;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String essId = null;
		private String startDate = null;
		private String endDate = null;
		private DayOfWeek dayOfWeek = null;

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

		public Builder setStartDate(String startDate) {
			this.startDate = startDate;
			return this;
		}

		public Builder setEndDate(String endDate) {
			this.endDate = endDate;
			return this;
		}

		public Builder setDayOfWeek(DayOfWeek dayOfWeek) {
			this.dayOfWeek = dayOfWeek;
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
	public String startDate() {
		return this.builder.startDate;
	}

	@Override
	public String endDate() {
		return this.builder.endDate;
	}

	@Override
	public DayOfWeek dayOfWeek() {
		return this.builder.dayOfWeek;
	}

}