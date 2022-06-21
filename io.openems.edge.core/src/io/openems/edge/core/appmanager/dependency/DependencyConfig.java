package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;

public class DependencyConfig {

	public final OpenemsApp app;
	public final OpenemsApp parent;

	// @Nullable
	// if not a dependency of an app.
	public final DependencyDeclaration sub;
	public final AppConfiguration config;

	public final String alias;
	public final JsonObject properties;

	public final List<DependencyConfig> declarations;

	public DependencyConfig(OpenemsApp app, OpenemsApp parent, DependencyDeclaration sub, AppConfiguration config,
			String alias, JsonObject properties, List<DependencyConfig> declarations) {
		this.app = app;
		this.parent = parent;
		this.sub = sub;
		this.config = config;
		this.alias = alias;
		this.properties = properties;
		this.declarations = declarations;
	}

	public final boolean isDependency() {
		return this.sub != null;
	}

}
