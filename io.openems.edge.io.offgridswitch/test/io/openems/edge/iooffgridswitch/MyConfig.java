package io.openems.edge.iooffgridswitch;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String inputMainContactor;
		private String inputGridStatus;
		private String inputGroundingContactor;
		private String outputMainContactor;
		private String outputGroundingContactor;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setInputMainContactor(String inputMainContactor) {
			this.inputMainContactor = inputMainContactor;
			return this;
		}

		public Builder setInputGridStatus(String inputGridStatus) {
			this.inputGridStatus = inputGridStatus;
			return this;
		}

		public Builder setInputGroundingContactor(String inputGroundingContactor) {
			this.inputGroundingContactor = inputGroundingContactor;
			return this;
		}

		public Builder setOutputMainContactor(String outputMainContactor) {
			this.outputMainContactor = outputMainContactor;
			return this;
		}

		public Builder setOutputGroundingContactor(String outputGroundingContactor) {
			this.outputGroundingContactor = outputGroundingContactor;
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
	public String inputMainContactor() {
		return this.builder.inputMainContactor;
	}

	@Override
	public String inputGridStatus() {
		return this.builder.inputGridStatus;
	}

	@Override
	public String inputGroundingContactor() {
		return this.builder.inputGroundingContactor;
	}

	@Override
	public String outputMainContactor() {
		return this.builder.outputMainContactor;
	}

	@Override
	public String outputGroundingContactor() {
		return this.builder.outputGroundingContactor;
	}
}