package io.openems.edge.system.fenecon.home;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.system.fenecon.home.enums.LedOrder;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String relayId;
		private LedOrder ledOrder;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setRelayId(String relayId) {
			this.relayId = relayId;
			return this;
		}

		public Builder setLedOrder(LedOrder ledOrder) {
			this.ledOrder = ledOrder;
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
	public LedOrder ledOrder() {
		return this.builder.ledOrder;
	}

	@Override
	public String relayId() {
		return this.builder.relayId;
	}
}
