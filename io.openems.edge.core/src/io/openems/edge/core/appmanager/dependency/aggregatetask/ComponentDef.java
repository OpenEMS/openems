package io.openems.edge.core.appmanager.dependency.aggregatetask;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public record ComponentDef(String id, String alias, String factoryId, ComponentProperties properties,
		Configuration config) {

	public record Configuration(boolean installAlways) {

		private Configuration(Builder builder) {
			this(builder.installAlways);
		}

		/**
		 * Creates a new Builder for {@link Configuration}.
		 *
		 * @return a new {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}

		/**
		 * Builder class for {@link Configuration}.
		 */
		public static final class Builder {
			private boolean installAlways = false;

			private Builder() {
			}

			/**
			 * Sets whether the component should always be installed.
			 *
			 * @param installAlways whether to always install
			 * @return this builder
			 */
			public Builder installAlways(boolean installAlways) {
				this.installAlways = installAlways;
				return this;
			}

			/**
			 * Builds the {@link Configuration} instance.
			 *
			 * @return the built {@link Configuration}
			 */
			public Configuration build() {
				return new Configuration(this);
			}
		}

		/**
		 * Returns a copy of this {@link Configuration} with a new installAlways value.
		 *
		 * @param installAlways new value
		 * @return copied {@link Configuration}
		 */
		public Configuration withInstallAlways(boolean installAlways) {
			return Configuration.create().installAlways(installAlways).build();
		}
	}

	/**
	 * Creates a {@link EdgeConfig.Component} from this {@link ComponentDef}.
	 * 
	 * @return the {@link EdgeConfig.Component}
	 */
	public EdgeConfig.Component toEdgeConfigComponent() {
		var json = this.properties().values().stream()//
				.collect(JsonUtils.toJsonObject(p -> p.name(), p -> p.value()));
		return new EdgeConfig.Component(this.id(), this.alias(), this.factoryId(), json);
	}

	/**
	 * Creates a {@link ComponentDef} from a {@link EdgeConfig.Component}.
	 * 
	 * @param comp the {@link EdgeConfig.Component}
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef from(EdgeConfig.Component comp) {
		if (comp == null) {
			return null;
		}
		return new ComponentDef(comp.getId(), comp.getAlias(), comp.getFactoryId(),
				ComponentProperties.fromMap(comp.getProperties()), Configuration.create().build());
	}
}