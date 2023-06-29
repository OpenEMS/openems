package io.openems.edge.controller.debuglog;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean showAlias;
		private String[] additionalChannels;
		private String[] ignoreComponents;
		private boolean condensedOutput;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setShowAlias(boolean showAlias) {
			this.showAlias = showAlias;
			return this;
		}

		public Builder setAdditionalChannels(String[] additionalChannels) {
			this.additionalChannels = additionalChannels;
			return this;
		}

		public Builder setIgnoreComponents(String[] ignoreComponents) {
			this.ignoreComponents = ignoreComponents;
			return this;
		}

		public Builder setCondensedOutput(boolean condensedOutput) {
			this.condensedOutput = condensedOutput;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	protected static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean showAlias() {
		return this.builder.showAlias;
	}

	@Override
	public String[] additionalChannels() {
		return this.builder.additionalChannels;
	}

	@Override
	public String[] ignoreComponents() {
		return this.builder.ignoreComponents;
	}

	@Override
	public boolean condensedOutput() {
		return this.builder.condensedOutput;
	}

}