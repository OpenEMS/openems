package io.openems.edge.controller.api.modbus.readwrite;

import java.nio.channels.Channels;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean enabled;
		private int port;
		private String[] componentIds;
		private int maxConcurrentConnections;
		private int apiTimeout;
		private String[] writeChannels = {};
		private String[] readChannels = {};

		private Builder() {
		}
		
		public Builder setWriteChannels(String... writeChannels) {
			this.writeChannels = writeChannels;
			return this;
		}
		
		public Builder setReadChannels(String... readChannels) {
			this.readChannels = readChannels;
			return this;
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
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
		return ConfigUtils.generateReferenceTargetFilter(this.id(), false, this.component_ids());
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

}