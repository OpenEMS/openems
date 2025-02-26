package io.openems.edge.evse.electricvehicle.generic;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int maxCurrentSinglePhase;
		private int maxCurrentThreePhase;
		private boolean canInterrupt;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMaxCurrentSinglePhase(int maxCurrentSinglePhase) {
			this.maxCurrentSinglePhase = maxCurrentSinglePhase;
			return this;
		}

		public Builder setMaxCurrentThreePhase(int maxCurrentThreePhase) {
			this.maxCurrentThreePhase = maxCurrentThreePhase;
			return this;
		}
		
		public Builder setCanInterrupt(boolean canInterrupt) {
			this.canInterrupt = canInterrupt;
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
	public int maxCurrentSinglePhase() {
		return this.builder.maxCurrentSinglePhase;
	}

	@Override
	public int maxCurrentThreePhase() {
		return this.builder.maxCurrentThreePhase;
	}

	@Override
	public boolean canInterrupt() {
		return this.builder.canInterrupt;
	}
}