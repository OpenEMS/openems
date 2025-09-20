package io.openems.edge.bridge.modbus;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.bridge.modbus.api.LogVerbosity;

@SuppressWarnings("all")
public class MyConfigTcp extends AbstractComponentConfig implements ConfigTcp {

	public static class Builder {
		private String id;
		private String ip;
		private int port;
		private LogVerbosity logVerbosity;
		private int invalidateElementsAfterReadErrors;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
			return this;
		}

		public Builder setInvalidateElementsAfterReadErrors(int invalidateElementsAfterReadErrors) {
			this.invalidateElementsAfterReadErrors = invalidateElementsAfterReadErrors;
			return this;
		}

		public MyConfigTcp build() {
			return new MyConfigTcp(this);
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

	private MyConfigTcp(Builder builder) {
		super(ConfigTcp.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public int port() {
		return this.builder.port;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}

	@Override
	public int invalidateElementsAfterReadErrors() {
		return this.builder.invalidateElementsAfterReadErrors;
	}

}