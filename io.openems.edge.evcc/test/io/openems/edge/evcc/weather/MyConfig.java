package io.openems.edge.evcc.weather;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
        private String id;
        private String apiUrl;
		private LogVerbosity logVerbosity;

        private Builder() {
            // empty
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

		public Builder setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
			return this;
		}
		
        public MyConfig build() {
            return new MyConfig(this);
        }
    }

    /**
    * Creates a new {@link Builder} instance for configuring and constructing
    * a {@code MyConfig} object.
    *
    * <p>
    * This is the entry point for the fluent builder pattern. After calling
    * this method, you can chain setter methods on the {@link Builder} and
    * finally call {@link Builder#build()} to obtain the configured instance.
    *
    * @return a new {@link Builder} instance
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
    public String apiUrl() {
        return this.builder.apiUrl;
    }

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}
}
