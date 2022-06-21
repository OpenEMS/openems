package io.openems.edge.core.appmanager.dependency;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class ExistingDependencyConfig extends DependencyConfig {

//	@Nullable
	public final OpenemsAppInstance parentInstance;
	public final OpenemsAppInstance instance;

//	public final List<Dependency> existingDependencies = null;

	public ExistingDependencyConfig(OpenemsApp app, OpenemsApp parentApp, DependencyDeclaration sub,
			AppConfiguration config, String alias, JsonObject properties, List<DependencyConfig> declarations,
			OpenemsAppInstance parent, OpenemsAppInstance instance) {
		super(app, parentApp, sub, config, alias, properties, declarations);
		this.parentInstance = parent;
		this.instance = instance;
	}
}
