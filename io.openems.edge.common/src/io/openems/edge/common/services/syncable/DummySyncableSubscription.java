package io.openems.edge.common.services.syncable;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;

public class DummySyncableSubscription<T> implements SyncableSubscription<T> {
	private final BehaviorSubject<SyncableEntry<T>> entrySubject;

	public DummySyncableSubscription(SyncableEntry<T> entry) {
		this.entrySubject = new BehaviorSubject<>(entry);
	}

	@Override
	public SyncableKey<T> getKey() {
		return this.entrySubject.getValue().key();
	}

	@Override
	public BehaviorSubject<SyncableEntry<T>> getEntry() {
		return this.entrySubject;
	}

	@Override
	public void dispose() {
		// Empty
	}
}
