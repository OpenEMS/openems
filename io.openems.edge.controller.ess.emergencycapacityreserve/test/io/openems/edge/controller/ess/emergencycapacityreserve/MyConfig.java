package io.openems.edge.controller.ess.emergencycapacityreserve;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int reserveSoc;
		private boolean isReserveSocEnabled;

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

		public Builder setReserveSoc(int reserveSoc) {
			this.reserveSoc = reserveSoc;
			return this;
		}

		public Builder setReserveSocEnabled(boolean isReserveSocEnabled) {
			this.isReserveSocEnabled = isReserveSocEnabled;
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
	public int reserveSoc() {
		return this.builder.reserveSoc;
	}

	@Override
	public boolean isReserveSocEnabled() {
		return this.builder.isReserveSocEnabled;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}
}