package io.openems.edge.controller.api.modbus;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;

@SuppressWarnings("all")
public class MyRtuConfig<T> extends AbstractComponentConfig
		implements io.openems.edge.controller.api.modbus.readonly.rtu.Config,
		io.openems.edge.controller.api.modbus.readwrite.rtu.Config {

	public static class Builder<T> {
		private final Class<T> clazz;

		private String id;
		private boolean enabled;
		private String portName;
		private String[] componentIds;
		private int baudrate;
		private int databits;
		private Stopbit stopbit;
		private Parity parity;
		private int apiTimeout;
		private int maxConcurrentConnections;
		private LogVerbosity logVerbosity;

		private Builder(Class<T> clazz) {
			this.clazz = clazz;
		}

		public Builder<T> setId(String id) {
			this.id = id;
			return this;
		}

		public Builder<T> setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder<T> setPortName(String portName) {
			this.portName = portName;
			return this;
		}

		public Builder<T> setComponentIds(String... componentIds) {
			this.componentIds = componentIds;
			return this;
		}

		public Builder<T> setMaxConcurrentConnections(int maxConcurrentConnections) {
			this.maxConcurrentConnections = maxConcurrentConnections;
			return this;
		}

		public Builder<T> setBaudrate(int baudrate) {
			this.baudrate = baudrate;
			return this;
		}

		public Builder<T> setDatabits(int databits) {
			this.databits = databits;
			return this;
		}

		public Builder<T> setStopbit(Stopbit stopbit) {
			this.stopbit = stopbit;
			return this;
		}

		public Builder<T> setParity(Parity parity) {
			this.parity = parity;
			return this;
		}

		public Builder<T> setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
			return this;
		}

		public Builder<T> setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
			return this;
		}

		public MyRtuConfig build() {
			return new MyRtuConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @param <T>   type of Config
	 * @param clazz {@link Class} of Config
	 * @return a {@link Builder}
	 */
	public static <T> Builder<T> create(Class<T> clazz) {
		return new Builder<T>(clazz);
	}

	private final Builder builder;

	private MyRtuConfig(Builder builder) {
		super(builder.clazz, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public String portName() {
		return this.builder.portName;
	}

	@Override
	public int apiTimeout() {
		return this.builder.apiTimeout;
	}

	@Override
	public int baudRate() {
		return this.builder.baudrate;
	}

	@Override
	public int databits() {
		return this.builder.baudrate;
	}

	@Override
	public Stopbit stopbits() {
		return this.builder.stopbit;
	}

	@Override
	public Parity parity() {
		return this.builder.parity;
	}

	@Override
	public String[] component_ids() {
		return this.builder.componentIds;
	}

	@Override
	public int maxConcurrentConnections() {
		return this.builder.maxConcurrentConnections;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}

	@Override
	public String Component_target() {
		return generateReferenceTargetFilter(this.id(), false, this.component_ids());
	}
}