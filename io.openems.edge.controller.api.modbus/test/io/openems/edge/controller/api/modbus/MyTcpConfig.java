package io.openems.edge.controller.api.modbus;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import java.time.Clock;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyTcpConfig extends AbstractComponentConfig
		implements io.openems.edge.controller.api.modbus.readonly.tcp.Config,
		io.openems.edge.controller.api.modbus.readwrite.tcp.Config {

	public static class Builder<T> {
		private final Class<T> clazz;

		private String id;
		private boolean enabled;
		private int port;
		private String[] componentIds;
		private int maxConcurrentConnections;
		private int apiTimeout;
		private Clock clock;
		private String[] writeChannels = {};
		private String[] readChannels = {};
		private LogVerbosity logVerbosity;

		private Builder(Class<T> clazz) {
			this.clazz = clazz;
		}

		public Builder<T> setWriteChannels(String... writeChannels) {
			this.writeChannels = writeChannels;
			return this;
		}

		public Builder<T> setReadChannels(String... readChannels) {
			this.readChannels = readChannels;
			return this;
		}

		public Builder<T> setId(String id) {
			this.id = id;
			return this;
		}

		public Builder<T> setClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		public Builder<T> setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder<T> setPort(int port) {
			this.port = port;
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

		public Builder<T> setApiTimeout(int apiTimeout) {
			this.apiTimeout = apiTimeout;
			return this;
		}

		public Builder<T> setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
			return this;
		}

		public MyTcpConfig build() {
			return new MyTcpConfig(this);
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

	private MyTcpConfig(Builder builder) {
		super(builder.clazz, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public int port() {
		return this.builder.port;
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
		return generateReferenceTargetFilter(this.id(), false, this.component_ids());
	}

	@Override
	public int apiTimeout() {
		return this.builder.apiTimeout;
	}

	@Override
	public String[] readChannels() {
		return this.builder.readChannels;
	}

	@Override
	public String[] writeChannels() {
		return this.builder.writeChannels;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}
}