package io.openems.backend.common.metadata;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;

public abstract class AbstractMetadata extends AbstractOpenemsBackendComponent implements Metadata {

	/**
	 * Initializes the AbstractMetadata.
	 *
	 * @param name a descriptive name for this component. Available via
	 *             {@link #getName()}
	 */
	protected AbstractMetadata(String name) {
		super(name);
	}

	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final Set<Runnable> onIsInitializedListeners = new HashSet<>();

	/**
	 * Make sure to call this method once initialized!.
	 */
	protected void setInitialized() {
		this.isInitialized.set(true);
		synchronized (this.onIsInitializedListeners) {
			for (Runnable callback : this.onIsInitializedListeners) {
				callback.run();
			}
		}
	}

	@Override
	public final boolean isInitialized() {
		return this.isInitialized.get();
	}

	@Override
	public final void addOnIsInitializedListener(Runnable callback) {
		synchronized (this.onIsInitializedListeners) {
			this.onIsInitializedListeners.add(callback);
		}
		// Run callback if I was already initialized
		if (this.isInitialized.get()) {
			callback.run();
		}
	}

	@Override
	public final void removeOnIsInitializedListener(Runnable callback) {
		synchronized (this.onIsInitializedListeners) {
			this.onIsInitializedListeners.remove(callback);
		}
	}
}
