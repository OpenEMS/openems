package io.openems.edge.simulator.timedata;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.simulator.CsvFormat;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private CsvFormat format;
		private String filename;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setFormat(CsvFormat format) {
			this.format = format;
			return this;
		}

		public Builder setFilename(String filename) {
			this.filename = filename;
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
	public CsvFormat format() {
		return this.builder.format;
	}

	@Override
	public String filename() {
		return this.builder.filename;
	}

}