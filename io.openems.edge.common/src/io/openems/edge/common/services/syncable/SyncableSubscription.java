package io.openems.edge.common.services.syncable;

import io.openems.common.function.Disposable;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;

public interface SyncableSubscription<T> extends Disposable {
	/**
	 * Gets the underlying {@link SyncableKey}.
	 * 
	 * @return Underlying {@link SyncableKey}
	 */
	SyncableKey<T> getKey();

	/**
	 * Gets an {@link BehaviorSubject} for the syncable entry. This value changes
	 * everytime we receive a new value from backend. Directly after the first
	 * subscribe, the entry will be null.
	 * 
	 * @return BehaviorSubject containing the entry information or null (if not
	 *         loaded yet or no data is available in backend)
	 */
	BehaviorSubject<SyncableEntry<T>> getEntry();
}
