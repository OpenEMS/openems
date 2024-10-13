package io.openems.edge.controller.ess.fastfrequencyreserve;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ControlMode;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private ControlMode mode;
		private String activationScheduleJson;
		private int preActivationTime;

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

		public Builder setMode(ControlMode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setactivationScheduleJson(String schedule) {
			this.activationScheduleJson = schedule;
			return this;
		}

		public Builder setPreActivationTime(int preActivationTime) {
			this.preActivationTime = preActivationTime;
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
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

	@Override
	public String meter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.meter_id());
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.mode;
	}

	@Override
	public String activationScheduleJson() {
		return this.builder.activationScheduleJson;
	}

	@Override
	public int preActivationTime() {
		return this.builder.preActivationTime;
	}

}
