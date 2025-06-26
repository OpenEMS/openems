package io.openems.edge.core.appmanager.dependency;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import com.google.common.base.Function;
import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class DependencyDeclaration {

	public final String key;

	// unmodifiableList
	public final List<AppDependencyConfig> appConfigs;

	public final CreatePolicy createPolicy;
	public final UpdatePolicy updatePolicy;
	public final DeletePolicy deletePolicy;

	public final DependencyUpdatePolicy dependencyUpdatePolicy;
	public final DependencyDeletePolicy dependencyDeletePolicy;

	public DependencyDeclaration(String key, CreatePolicy createPolicy, UpdatePolicy updatePolicy,
			DeletePolicy deletePolicy, DependencyUpdatePolicy dependencyUpdatePolicy,
			DependencyDeletePolicy dependencyDeletePolicy, AppDependencyConfig... appConfigs) {
		this.key = key;

		if (appConfigs.length == 0) {
			throw new IllegalArgumentException("There has to be atleast one 'appConfig'!");
		}
		// TODO check for duplicated appIds
		this.appConfigs = Collections.unmodifiableList(Arrays.asList(appConfigs));

		this.createPolicy = createPolicy;
		this.updatePolicy = updatePolicy;
		this.deletePolicy = deletePolicy;
		this.dependencyUpdatePolicy = dependencyUpdatePolicy;
		this.dependencyDeletePolicy = dependencyDeletePolicy;
	}

	public static class AppDependencyConfig {

		// NOTE: must have either appId or specificInstanceId
		public final String appId;
		public final UUID specificInstanceId;
		public final String alias;
		public final JsonObject properties;
		public final JsonObject initialProperties;

		private AppDependencyConfig(String appId, UUID specificInstanceId, String alias, JsonObject properties,
				JsonObject initialProperties) {
			if (appId == null && specificInstanceId == null) {
				throw new NullPointerException(
						"'appId' and 'specificInstanceId' of a AppDependencyConfig can't be both null!");
			}
			this.appId = appId;
			this.specificInstanceId = specificInstanceId;
			this.alias = alias;
			this.properties = properties == null ? new JsonObject() : properties;
			this.initialProperties = initialProperties == null ? this.properties : initialProperties;
		}

		/**
		 * Gets a {@link Builder} for an {@link AppDependencyConfig}.
		 *
		 * @return the builder
		 */
		public static Builder create() {
			return new Builder();
		}

		public static final class Builder {
			private String appId;
			private UUID specificInstanceId;
			private String alias;
			private JsonObject properties;
			private JsonObject initialProperties;

			public Builder() {
			}

			public Builder setAppId(String appId) {
				this.appId = appId;
				return this;
			}

			public Builder setSpecificInstanceId(UUID specificInstanceId) {
				this.specificInstanceId = specificInstanceId;
				return this;
			}

			public Builder setAlias(String alias) {
				this.alias = alias;
				return this;
			}

			/**
			 * The properties that are used to update the instance.
			 *
			 * @param properties the properties
			 * @return this
			 */
			public Builder setProperties(JsonObject properties) {
				this.properties = properties;
				return this;
			}

			/**
			 * The properties that are used to firstly instantiate the app.
			 *
			 * <p>
			 * If not set the properties are used.
			 *
			 * @param initialProperties the properties
			 * @return this
			 */
			public Builder setInitialProperties(JsonObject initialProperties) {
				this.initialProperties = initialProperties;
				return this;
			}

			public AppDependencyConfig build() {
				return new AppDependencyConfig(this.appId, this.specificInstanceId, this.alias, this.properties,
						this.initialProperties);
			}
		}

	}

	/**
	 * Defines if the dependency app should get created when creating the parent
	 * app.
	 */
	public static enum CreatePolicy {
		/**
		 * Always creates the dependent app except an {@link OpenemsAppInstance} is
		 * already created and not a dependency of another app.
		 */
		ALWAYS((instances, app) -> true), //

		/**
		 * lazy singleton.
		 */
		IF_NOT_EXISTING((instances, app) -> instances.stream().anyMatch(t -> t.appId.equals(app))), //

		/**
		 * Never allowed to create the app.
		 */
		NEVER((instances, app) -> false), //
		;

		private final BiFunction<List<OpenemsAppInstance>, String, Boolean> isAllowedToCreateFunction;

		private CreatePolicy(BiFunction<List<OpenemsAppInstance>, String, Boolean> isAllowedToCreateFunction) {
			this.isAllowedToCreateFunction = isAllowedToCreateFunction;
		}

		/**
		 * Determines if the app of the given appId is allowed to create. This does not
		 * mean an existing app can't be used as the dependency.
		 *
		 * @param allInstances all app instances
		 * @param appId        the appId
		 * @return s true if the app is allowed to create
		 */
		public final boolean isAllowedToCreate(List<OpenemsAppInstance> allInstances, String appId) {
			return this.isAllowedToCreateFunction.apply(allInstances, appId);
		}
	}

	/**
	 * Defines if the dependency should get updated when updating the parent app.
	 */
	public static enum UpdatePolicy {
		ALWAYS(v -> true), //
		IF_MINE(v -> !v.allInstances.stream() //
				.filter(i -> !i.equals(v.parent)) //
				.anyMatch(a -> a.dependencies != null
						&& a.dependencies.stream().anyMatch(d -> d.instanceId.equals(v.app2Update.instanceId)))), //
		NEVER(v -> false), //
		;

		private final Function<AllowedToValues, Boolean> isAllowedToUpdateFunction;

		private UpdatePolicy(Function<AllowedToValues, Boolean> isAllowedToUpdate) {
			this.isAllowedToUpdateFunction = isAllowedToUpdate;
		}

		/**
		 * Determines if an {@link OpenemsAppInstance} is allowed to be updated.
		 *
		 * @param allInstances all {@link OpenemsAppInstance}
		 * @param parent       the parent {@link OpenemsAppInstance}
		 * @param app2Update   the {@link OpenemsAppInstance} to updated
		 * @return true if the instance is allowed to be updated else false
		 */
		public final boolean isAllowedToUpdate(List<OpenemsAppInstance> allInstances, OpenemsAppInstance parent,
				OpenemsAppInstance app2Update) {
			return this.isAllowedToUpdateFunction.apply(new AllowedToValues(allInstances, parent, app2Update));
		}

	}

	/**
	 * Defines if the dependency app gets deleted when deleting its parent.
	 */
	public static enum DeletePolicy {
		ALWAYS(v -> true), //
		IF_MINE(v -> !v.allInstances.stream().filter(a -> !a.equals(v.parent) && a.dependencies != null)
				.anyMatch(a -> a.dependencies.stream().anyMatch(d -> d.instanceId.equals(v.app2Update.instanceId)))), //
		NEVER(v -> false), //
		;

		private final Function<AllowedToValues, Boolean> isAllowedToDeleteFunction;

		private DeletePolicy(Function<AllowedToValues, Boolean> isAllowedToDelete) {
			this.isAllowedToDeleteFunction = isAllowedToDelete;
		}

		/**
		 * Determines if an {@link OpenemsAppInstance} is allowed to be deleted.
		 *
		 * @param allInstances all {@link OpenemsAppInstance}
		 * @param parent       the parent {@link OpenemsAppInstance}
		 * @param app2Delete   the {@link OpenemsAppInstance} to delete
		 * @return true if the instance is allowed to be deleted else false
		 */
		public final boolean isAllowedToDelete(List<OpenemsAppInstance> allInstances, OpenemsAppInstance parent,
				OpenemsAppInstance app2Delete) {
			return this.isAllowedToDeleteFunction.apply(new AllowedToValues(allInstances, parent, app2Delete));
		}
	}

	private static class AllowedToValues {
		public final List<OpenemsAppInstance> allInstances;
		public final OpenemsAppInstance parent;
		public final OpenemsAppInstance app2Update;

		public AllowedToValues(List<OpenemsAppInstance> allInstances, OpenemsAppInstance parent,
				OpenemsAppInstance app2Update) {
			this.allInstances = allInstances;
			this.parent = parent;
			this.app2Update = app2Update;
		}
	}

	/**
	 * Defines if the user can change properties of the dependency app.
	 */
	public static enum DependencyUpdatePolicy {
		ALLOW_ALL, //
		ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
		ALLOW_NONE, //
		;
	}

	/**
	 * Defines if the user can delete an app which is a dependency of another app.
	 */
	public static enum DependencyDeletePolicy {
		NOT_ALLOWED, //
		ALLOWED, //
		;
	}

}
