package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public final class TemporaryApps {

	public final List<OpenemsAppInstance> currentlyCreatingApps = new LinkedList<>();
	public final List<OpenemsAppInstance> currentlyModifiedApps = new LinkedList<>();
	public final List<OpenemsAppInstance> currentlyDeletingApps = new LinkedList<>();

	/**
	 * Gets a unmodifiable list of the apps that are currently creating or modified.
	 *
	 * @return the apps
	 */
	public final List<OpenemsAppInstance> currentlyCreatingModifiedApps() {
		var result = new ArrayList<OpenemsAppInstance>(this.currentlyCreatingApps.size() //
				+ this.currentlyModifiedApps.size());
		result.addAll(this.currentlyCreatingApps);
		result.addAll(this.currentlyModifiedApps);
		return Collections.unmodifiableList(result);
	}

}
