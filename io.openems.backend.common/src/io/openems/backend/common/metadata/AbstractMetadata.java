package io.openems.backend.common.metadata;

import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.common.event.EventBuilder;

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

	/**
	 * Make sure to call this method once initialized!.
	 */
	protected void setInitialized() {
		this.isInitialized.set(true);
		EventBuilder.post(this.getEventAdmin(), Events.AFTER_IS_INITIALIZED);
	}

	@Override
	public final boolean isInitialized() {
		return this.isInitialized.get();
	}

}
