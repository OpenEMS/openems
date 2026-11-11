package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private boolean enabled = true;
		private Country country = Country.GERMANY;
		private String email = "";
		private String password = "";
		private String accountNumber = "";
		private int refreshMinutes = 30;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setCountry(Country country) {
			this.country = country;
			return this;
		}

		public Builder setEmail(String email) {
			this.email = email;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setAccountNumber(String accountNumber) {
			this.accountNumber = accountNumber;
			return this;
		}

		public Builder setRefreshMinutes(int refreshMinutes) {
			this.refreshMinutes = refreshMinutes;
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
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public Country country() {
		return this.builder.country;
	}

	@Override
	public String email() {
		return this.builder.email;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public String accountNumber() {
		return this.builder.accountNumber;
	}

	@Override
	public int refreshMinutes() {
		return this.builder.refreshMinutes;
	}
}
