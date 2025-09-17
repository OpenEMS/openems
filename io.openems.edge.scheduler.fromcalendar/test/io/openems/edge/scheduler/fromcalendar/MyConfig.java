package io.openems.edge.scheduler.fromcalendar;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.scheduler.fromcalendar.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String controllerSchedule;
		private String[] alwaysRunAfterControllerIds;
		private String[] alwaysRunBeforeControllerIds;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlwaysRunAfterControllerIds(String... alwaysRunAfterController_ids) {
			this.alwaysRunAfterControllerIds = alwaysRunAfterController_ids;
			return this;
		}
		
		public Builder setAlwaysRunBeforeControllerIds(String... alwaysRunBeforeController_ids) {
			this.alwaysRunBeforeControllerIds = alwaysRunBeforeController_ids;
			return this;
		}
		
		public Builder setControllerSchedule(String controllerSchedule) {
			this.controllerSchedule = controllerSchedule;
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
	public String controllerSchedule() {
		return this.builder.controllerSchedule;
	}

	@Override
	public String[] alwaysRunAfterController_ids() {
		return this.builder.alwaysRunAfterControllerIds;
	}
}