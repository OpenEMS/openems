package io.openems.edge.scheduler.daily;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String[] alwaysRunBeforeControllerIds;
		private String controllerScheduleJson;
		private String[] alwaysRunAfterControllerIds;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlwaysRunAfterControllerIds(String... alwaysRunAfterControllerIds) {
			this.alwaysRunAfterControllerIds = alwaysRunAfterControllerIds;
			return this;
		}

		public Builder setAlwaysRunBeforeControllerIds(String... alwaysRunBeforeControllerIds) {
			this.alwaysRunBeforeControllerIds = alwaysRunBeforeControllerIds;
			return this;
		}

		public Builder setControllerScheduleJson(String controllerScheduleJson) {
			this.controllerScheduleJson = controllerScheduleJson;
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
	public String[] alwaysRunBeforeController_ids() {
		return this.builder.alwaysRunBeforeControllerIds;
	}

	@Override
	public String controllerScheduleJson() {
		return this.builder.controllerScheduleJson;
	}

	@Override
	public String[] alwaysRunAfterController_ids() {
		return this.builder.alwaysRunAfterControllerIds;
	}

}