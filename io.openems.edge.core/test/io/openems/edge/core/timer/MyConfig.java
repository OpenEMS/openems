package io.openems.edge.core.timer;

import io.openems.common.test.AbstractComponentConfig;

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
	super(Config.class, TimerManager.SINGLETON_COMPONENT_ID);
	this.builder = builder;
    }

}