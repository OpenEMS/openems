package io.openems.edge.core.componentmanager;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.component.ComponentManager;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {

		private Builder() {
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
		super(Config.class, ComponentManager.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

}
