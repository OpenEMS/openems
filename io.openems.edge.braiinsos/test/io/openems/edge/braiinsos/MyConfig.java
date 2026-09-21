package io.openems.edge.braiinsos;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = "ctrlBraiinsSingle0";
		private boolean enabled = true;
		private boolean readOnly = false;
		private Mode mode = Mode.OFF;
		private int defaultConsumptionW = 3000;
		private String ip;
		private String username = "root";
		private String password = "";
		private SingleOrAllPhase phase = SingleOrAllPhase.L1;
		private MeterType type = MeterType.CONSUMPTION_METERED;
		private String jsCalendar = "[]";

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

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setDefaultConsumptionW(int defaultConsumptionW) {
			this.defaultConsumptionW = defaultConsumptionW;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setPhase(SingleOrAllPhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setJsCalendar(String jsCalendar) {
			this.jsCalendar = jsCalendar;
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
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public int defaultConsumptionW() {
		return this.builder.defaultConsumptionW;
	}

	@Override
	public String username() {
		return this.builder.username;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public SingleOrAllPhase phase() {
		return this.builder.phase;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String jsCalendar() {
		return this.builder.jsCalendar;
	}
}