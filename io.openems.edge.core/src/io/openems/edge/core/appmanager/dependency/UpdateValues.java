package io.openems.edge.core.appmanager.dependency;

import java.util.Collections;
import java.util.List;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class UpdateValues {

	public final OpenemsAppInstance rootInstance;
	public final List<OpenemsAppInstance> modifiedOrCreatedApps;
	public final List<OpenemsAppInstance> deletedApps;

	public final List<String> warnings;

	public UpdateValues(OpenemsAppInstance rootInstance, List<OpenemsAppInstance> modifiedOrCreatedApps,
			List<OpenemsAppInstance> deletedApps) {
		this(rootInstance, modifiedOrCreatedApps, deletedApps, null);
	}

	public UpdateValues(OpenemsAppInstance rootInstance, List<OpenemsAppInstance> modifiedOrCreatedApps,
			List<OpenemsAppInstance> deletedApps, List<String> warnings) {
		this.rootInstance = rootInstance;
		this.modifiedOrCreatedApps = Collections.unmodifiableList(modifiedOrCreatedApps);
		this.deletedApps = Collections.unmodifiableList(deletedApps);
		this.warnings = Collections.unmodifiableList(warnings);
	}

}
