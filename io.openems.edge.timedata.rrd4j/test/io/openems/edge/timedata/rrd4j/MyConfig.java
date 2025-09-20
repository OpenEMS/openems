package io.openems.edge.timedata.rrd4j;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private PersistencePriority persistencePriority;
		private boolean readOnly;
		private boolean debugMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPersistencePriority(PersistencePriority persistencePriority) {
			this.persistencePriority = persistencePriority;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
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
	public PersistencePriority persistencePriority() {
		return this.builder.persistencePriority;
	}

	@Override
	public boolean isReadOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

}