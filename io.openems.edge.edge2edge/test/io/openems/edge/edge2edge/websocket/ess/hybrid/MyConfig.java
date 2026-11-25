package io.openems.edge.edge2edge.websocket.ess.hybrid;

import io.openems.common.channel.AccessMode;
import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id = "ess0";
		private String[] dcChargerIds = {};

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDcChargerIds(String... dcChargerIds) {
			this.dcChargerIds = dcChargerIds;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Creates a configuration builder.
	 *
	 * @return the builder
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
	public AccessMode remoteAccessMode() {
		return AccessMode.READ_ONLY;
	}

	@Override
	public String remoteComponentId() {
		return "ess0";
	}

	@Override
	public String[] dcChargerIds() {
		return this.builder.dcChargerIds;
	}

	@Override
	public String bridge_id() {
		return "bridge0";
	}
}
