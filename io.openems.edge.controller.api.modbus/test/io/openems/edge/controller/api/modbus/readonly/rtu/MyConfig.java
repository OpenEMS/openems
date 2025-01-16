package io.openems.edge.controller.api.modbus.readonly.rtu;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
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

		public Builder setPortName(String portName) {
			this.portName = portName;
			return this;
		}

		public Builder setComponentIds(String... componentIds) {
			this.componentIds = componentIds;
			return this;
		}

		public Builder setMaxConcurrentConnections(int maxConcurrentConnections) {
			this.maxConcurrentConnections = maxConcurrentConnections;
			return this;
		}
		
		public Builder setBaudrate(int baudrate) {
			this.baudrate = baudrate;
			return this;
		}
		
		public Builder setDatabits(int databits) {
			this.databits = databits;
			return this;
		}
		
		public Builder setStopbit(Stopbit stopbit) {
			this.stopbit = stopbit;
			return this;
		}
		
		public Builder setParity(Parity parity) {
			this.parity = parity;
			return this;
		}
		
		public Builder setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
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
	public String Component_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), false, this.component_ids());
	}
	
}