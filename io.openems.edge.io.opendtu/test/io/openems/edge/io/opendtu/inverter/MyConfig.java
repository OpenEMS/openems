package io.openems.edge.io.opendtu.inverter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	private final String ip;
	private final MeterType type;
	private final String username;
	private final String password;
	private final String serialNumberL1;
	private final String serialNumberL2;
	private final String serialNumberL3;
	private final int relativeLimit;
	private final int absoluteLimit;
	private final int threshold;
	private final int delay;
	private final boolean debugMode;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.ip = builder.ip;
		this.type = builder.type;
		this.username = builder.username;
		this.password = builder.password;
		this.serialNumberL1 = builder.serialNumberL1;
		this.serialNumberL2 = builder.serialNumberL2;
		this.serialNumberL3 = builder.serialNumberL3;
		this.relativeLimit = builder.relativeLimit;
		this.absoluteLimit = builder.absoluteLimit;
		this.threshold = builder.threshold;
		this.delay = builder.delay;
		this.debugMode = builder.debugMode;
	}

	public static class Builder {
		private String id;
		private String ip;
		private MeterType type;
		private String username;
		private String password;
		private String serialNumberL1;
		private String serialNumberL2;
		private String serialNumberL3;
		private int relativeLimit;
		private int absoluteLimit;
		private int threshold;
		private int delay;
		private boolean debugMode;

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
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

		public Builder setSerialNumberL1(String serialNumberL1) {
			this.serialNumberL1 = serialNumberL1;
			return this;
		}

		public Builder setSerialNumberL2(String serialNumberL2) {
			this.serialNumberL2 = serialNumberL2;
			return this;
		}

		public Builder setSerialNumberL3(String serialNumberL3) {
			this.serialNumberL3 = serialNumberL3;
			return this;
		}

		public Builder setRelativeLimit(int relativeLimit) {
			this.relativeLimit = relativeLimit;
			return this;
		}

		public Builder setAbsoluteLimit(int absoluteLimit) {
			this.absoluteLimit = absoluteLimit;
			return this;
		}

		public Builder setThreshold(int threshold) {
			this.threshold = threshold;
			return this;
		}

		public Builder setDelay(int delay) {
			this.delay = delay;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates a new instance of the Builder class to construct a {@link MyConfig}
	 * object.
	 *
	 * @return A new instance of the {@link Builder}, ready for setting
	 *         configuration properties.
	 */
	public static Builder create() {
		return new Builder();
	}

	@Override
	public String ip() {
		return this.ip;
	}

	@Override
	public MeterType type() {
		return this.type;
	}

	@Override
	public String username() {
		return this.username;
	}

	@Override
	public String password() {
		return this.password;
	}

	@Override
	public String serialNumberL1() {
		return this.serialNumberL1;
	}

	@Override
	public String serialNumberL2() {
		return this.serialNumberL2;
	}

	@Override
	public String serialNumberL3() {
		return this.serialNumberL3;
	}

	@Override
	public int relativeLimit() {
		return this.relativeLimit;
	}

	@Override
	public int absoluteLimit() {
		return this.absoluteLimit;
	}

	@Override
	public int threshold() {
		return this.threshold;
	}

	@Override
	public int delay() {
		return this.delay;
	}

	@Override
	public boolean debugMode() {
		return this.debugMode;
	}
}
