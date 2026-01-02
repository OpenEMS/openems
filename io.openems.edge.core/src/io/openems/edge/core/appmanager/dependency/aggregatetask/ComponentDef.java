package io.openems.edge.core.appmanager.dependency.aggregatetask;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public record ComponentDef(String id, String alias, String factoryId, ComponentProperties properties,
		Configuration config) {

	public record Configuration(boolean installAlways, boolean forceUpdateOrCreate) {

		/**
		 * Creates a default {@link Configuration}.
		 * 
		 * @return the default {@link Configuration}
		 */
		public static Configuration defaultConfig() {
			return new Configuration(false, false);
		}

		/**
		 * Returns a copy of this {@link Configuration} with a new installAlways value.
		 *
		 * @param installAlways new value
		 * @return copied {@link Configuration}
		 */
		public Configuration withInstallAlways(boolean installAlways) {
			return new Configuration(installAlways, this.forceUpdateOrCreate());
		}

		/**
		 * Returns a copy of this {@link Configuration} with a new forceUpdateOrCreate value.
		 *
		 * @param forceUpdateOrCreate new value
		 * @return copied {@link Configuration}
		 */
		public Configuration withForceUpdateOrCreate(boolean forceUpdateOrCreate) {
			return new Configuration(this.installAlways(), forceUpdateOrCreate);
		}

	}

	/**
	 * Returns a copy of this {@link ComponentDef} with the new properties.
	 * 
	 * @param properties the new properties
	 * @return copied {@link ComponentDef}
	 */
	public ComponentDef withProperties(ComponentProperties properties) {
		return new ComponentDef(this.id, this.alias, this.factoryId, properties, this.config);
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
				ComponentProperties.fromMap(comp.getProperties()), Configuration.defaultConfig());
	}
}