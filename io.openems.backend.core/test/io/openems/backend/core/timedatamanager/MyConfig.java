package io.openems.backend.core.timedatamanager;

import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.util.List;

@SuppressWarnings("all")
public record MyConfig(String[] timedata_ids) implements Config {

	public static final class Builder {
		private List<String> timedataIds = emptyList();

		private Builder() {
		}

		public Builder setTimedataIds(String... timedataIds) {
			this.timedataIds = List.of(timedataIds);
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this.timedataIds.toArray(String[]::new));
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

	@Override
	public Class<? extends Annotation> annotationType() {
		return Config.class;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "";
	}

}
