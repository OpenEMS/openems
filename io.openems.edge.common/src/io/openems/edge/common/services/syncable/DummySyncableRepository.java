package io.openems.edge.common.services.syncable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;

public class DummySyncableRepository implements SyncableRepository {
	private final Map<SyncableKey<?>, BehaviorSubject<SyncableEntry<?>>> storedValues = new HashMap<>();

	public <T> void setEntry(SyncableKey<T> key, SyncableEntry<T> entry) {
		final var stored = this.storedValues.computeIfAbsent(key, k -> new BehaviorSubject<>(null));
		stored.setValue(entry);
	}

	@Override
	public <T> SyncableSubscription<T> subscribe(SyncableKey<T> key) {
		final var stored = this.storedValues.computeIfAbsent(key, k -> new BehaviorSubject<>(null));
		@SuppressWarnings("unchecked")
		var storedCasted = (BehaviorSubject<SyncableEntry<T>>) (BehaviorSubject<?>) stored;
		return new DummySyncableSubscription<>(storedCasted);
	}

	@Override
	public Set<SyncableKey<?>> getSubscribedSyncableKeys() {
		return new HashSet<>(this.storedValues.keySet());
	}
}
