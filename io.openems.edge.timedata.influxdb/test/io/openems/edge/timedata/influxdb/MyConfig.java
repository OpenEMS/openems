package io.openems.edge.timedata.influxdb;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.shared.influxdb.QueryLanguageConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean isReadOnly;
		private int maxQueueSize;
		private int noOfCycles;
		private String bucket;
		private String apiKey;
		private String org;
		private String url;
		private QueryLanguageConfig queryLanguage;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
			return this;
		}

		public Builder setMaxQueueSize(int maxQueueSize) {
			this.maxQueueSize = maxQueueSize;
			return this;
		}

		public Builder setNoOfCycles(int noOfCycles) {
			this.noOfCycles = noOfCycles;
			return this;
		}

		public Builder setBucket(String bucket) {
			this.bucket = bucket;
			return this;
		}

		public Builder setApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder setOrg(String org) {
			this.org = org;
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}

		public Builder setQueryLanguage(QueryLanguageConfig queryLanguage) {
			this.queryLanguage = queryLanguage;
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
	public QueryLanguageConfig queryLanguage() {
		return this.builder.queryLanguage;
	}

	@Override
	public String url() {
		return this.builder.url;
	}

	@Override
	public String org() {
		return this.builder.org;
	}

	@Override
	public String apiKey() {
		return this.builder.apiKey;
	}

	@Override
	public String bucket() {
		return this.builder.bucket;
	}

	@Override
	public int noOfCycles() {
		return this.builder.noOfCycles;
	}

	@Override
	public int maxQueueSize() {
		return this.builder.maxQueueSize;
	}

	@Override
	public boolean isReadOnly() {
		return this.builder.isReadOnly;
	}
}