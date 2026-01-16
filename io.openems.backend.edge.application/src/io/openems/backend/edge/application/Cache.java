package io.openems.backend.edge.application;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.common.function.BooleanConsumer;

public class Cache {

	private final BooleanConsumer onInitializedChange;

	private volatile UpdateMetadataCache.Notification metadata = UpdateMetadataCache.Notification.empty();

	public Cache(BooleanConsumer onInitializedChange) {
		this.onInitializedChange = onInitializedChange;
	}

	protected synchronized void update(UpdateMetadataCache.Notification notification) {
		this.metadata = notification;
		this.onInitializedChange.accept(this.isInitialized());
	}

	/**
	 * Is the {@link Cache} initialized?.
	 * 
	 * @return true if initialized
	 */
	public synchronized boolean isInitialized() {
		return !this.metadata.getApikeysToEdgeIds().isEmpty();
	}

	/**
	 * Authenticates an Apikey.
	 * 
	 * @param apikey the Apikey
	 * @return the Edge-ID or null if authentication failed
	 */
	public String authenticateApikey(String apikey) {
		return this.metadata.getApikeysToEdgeIds().get(apikey);
	}
}
