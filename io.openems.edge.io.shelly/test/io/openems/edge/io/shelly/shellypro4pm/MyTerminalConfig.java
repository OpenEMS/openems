package io.openems.edge.io.shelly.shellypro4pm;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyTerminalConfig extends AbstractComponentConfig implements TerminalConfig {

	protected static class Builder {
		private String id;
		private TerminalEnum terminal;
		private MeterType type;
		private boolean invert;
		private String deviceId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setTerminal(TerminalEnum terminal) {
			this.terminal = terminal;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setInvert(boolean invert) {
			this.invert = invert;
			return this;
		}

		public Builder setDeviceId(String deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public MyTerminalConfig build() {
			return new MyTerminalConfig(this);
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

	private MyTerminalConfig(Builder builder) {
		super(TerminalConfig.class, builder.id);
		this.builder = builder;
	}

	@Override
	public TerminalEnum terminal() {
		return this.builder.terminal;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

	@Override
	public String device_id() {
		return this.builder.deviceId;
	}

	@Override
	public String Device_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.device_id());
	}
}