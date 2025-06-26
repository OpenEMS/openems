package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.openems.edge.core.appmanager.OpenemsAppInstance;

public class TemporaryApps {

	private final List<OpenemsAppInstance> currentlyCreatingApps;
	private final List<OpenemsAppInstance> currentlyModifiedApps;
	private final List<OpenemsAppInstance> currentlyDeletingApps;

	public TemporaryApps() {
		this(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
	}

	protected TemporaryApps(List<OpenemsAppInstance> currentlyCreatingApps,
			List<OpenemsAppInstance> currentlyModifiedApps, List<OpenemsAppInstance> currentlyDeletingApps) {
		this.currentlyCreatingApps = currentlyCreatingApps;
		this.currentlyModifiedApps = currentlyModifiedApps;
		this.currentlyDeletingApps = currentlyDeletingApps;
	}

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

	/**
	 * Gets the instances which are currently being created.
	 * 
	 * @return the {@link OpenemsAppInstance}
	 */
	public List<OpenemsAppInstance> currentlyCreatingApps() {
		return this.currentlyCreatingApps;
	}

	/**
	 * Gets the instances which are going to be deleted.
	 * 
	 * @return the {@link OpenemsAppInstance}
	 */
	public List<OpenemsAppInstance> currentlyDeletingApps() {
		return this.currentlyDeletingApps;
	}

	/**
	 * Gets the instances which are currently being modified.
	 * 
	 * @return the {@link OpenemsAppInstance}
	 */
	public List<OpenemsAppInstance> currentlyModifiedApps() {
		return this.currentlyModifiedApps;
	}

	/**
	 * Creates a unmodifiable instance of the given {@link TemporaryApps}.
	 * 
	 * @param temporaryApps the {@link TemporaryApps} to get a unmodifiable version
	 *                      of
	 * @return a unmodifiable instance of {@link TemporaryApps}
	 */
	public static TemporaryApps unmodifiableApps(TemporaryApps temporaryApps) {
		return new UnmodifiableTemporaryApps(temporaryApps);
	}

	private static final class UnmodifiableTemporaryApps extends TemporaryApps {

		public UnmodifiableTemporaryApps(TemporaryApps origin) {
			super(Collections.unmodifiableList(origin.currentlyCreatingApps()), //
					Collections.unmodifiableList(origin.currentlyModifiedApps()), //
					Collections.unmodifiableList(origin.currentlyDeletingApps()));
		}

	}

}
