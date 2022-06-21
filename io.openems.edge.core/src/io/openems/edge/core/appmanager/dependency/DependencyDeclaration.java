package io.openems.edge.core.appmanager.dependency;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Function;
import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class DependencyDeclaration {

	public final String key;
	public final String appId;
	public final String alias;
	public final JsonObject properties;

	public final CreatePolicy createPolicy;
	public final UpdatePolicy updatePolicy;
	public final DeletePolicy deletePolicy;

	// Dependency Supplier?
	private final Supplier<String> supplierForAppId = null;
	private final Function<List<OpenemsAppInstance>, String> supplierForInstanceIdFromExisting = null;

	public DependencyDeclaration(String key, String appId, String alias, CreatePolicy createPolicy,
			UpdatePolicy updatePolicy, DeletePolicy deletePolicy, JsonObject properties) {
		this.key = key;
		this.appId = appId;
		this.alias = alias;
		this.properties = properties;
		this.createPolicy = createPolicy;
		this.updatePolicy = updatePolicy;
		this.deletePolicy = deletePolicy;
	}

	public static enum CreatePolicy {
		/**
		 * Always creates the dependent app except an {@link OpenemsAppInstance} is
		 * already created and not a dependency of another app.
		 */
		ALWAYS, //
		/**
		 * lazy singleton.
		 */
		IF_NOT_EXISTING, //
		NEVER, //
		;
	}

	public static enum UpdatePolicy {
		ALWAYS, //
		IF_MINE, //
		NEVER, //
		;
	}

	public static enum DeletePolicy {
		ALWAYS, //
		IF_MINE, //
		NEVER, //
		;
	}

}
