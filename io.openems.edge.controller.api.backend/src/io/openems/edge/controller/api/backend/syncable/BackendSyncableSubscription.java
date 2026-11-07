package io.openems.edge.controller.api.backend.syncable;

import java.util.function.Consumer;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.common.services.syncable.SyncableSubscription;

public class BackendSyncableSubscription<T> implements SyncableSubscription<T> {
	private final SubscriptionHandler<T> handler;
	private final Consumer<BackendSyncableSubscription<T>> onDispose;

	protected BackendSyncableSubscription(SubscriptionHandler<T> handler,
			Consumer<BackendSyncableSubscription<T>> onDispose) {
		this.handler = handler;
		this.onDispose = onDispose;
	}

	public SyncableKey<T> getKey() {
		return this.handler.getKey();
	}

	@Override
	public BehaviorSubject<SyncableEntry<T>> getEntry() {
		return this.handler.getEntry();
	}

	@Override
	public void dispose() {
		this.onDispose.accept(this);
	}
}
