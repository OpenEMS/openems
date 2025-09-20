package io.openems.edge.controller.generic.jsonlogic;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.JsonUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = "ctrlJsonLogic0";
		private String rule = null;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setRule(String rule) throws OpenemsNamedException {
			JsonUtils.prettyPrint(JsonUtils.parse(rule));
			this.rule = rule;
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
	public String rule() {
		return this.builder.rule;
	}

}