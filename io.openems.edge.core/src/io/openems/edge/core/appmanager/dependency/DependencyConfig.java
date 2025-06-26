package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;

public class DependencyConfig {

	public final OpenemsApp app;
	public final OpenemsApp parent;

	// @Nullable
	// if not a dependency of an app.
	public final DependencyDeclaration sub;
	public final AppConfiguration config;

	public final DependencyDeclaration.AppDependencyConfig appDependencyConfig;

	public final List<DependencyConfig> declarations;

	public DependencyConfig(OpenemsApp app, OpenemsApp parent, DependencyDeclaration sub, AppConfiguration config,
			DependencyDeclaration.AppDependencyConfig appDependencyConfig, List<DependencyConfig> declarations) {
		this.app = app;
		this.parent = parent;
		this.sub = sub;
		this.config = config;
		this.appDependencyConfig = appDependencyConfig;
		this.declarations = declarations;
	}

	public final boolean isDependency() {
		return this.sub != null;
	}

}
