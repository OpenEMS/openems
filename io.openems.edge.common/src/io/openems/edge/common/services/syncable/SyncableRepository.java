package io.openems.edge.common.services.syncable;

import java.util.Set;

import io.openems.common.jsonrpc.syncable.SyncableKey;

public interface SyncableRepository {

	/**
	 * Subscribe to a specific SyncableKey. When multiple subscriptions using the
	 * same key are made, the subscription is shared in the background.
	 * 
	 * <p>
	 * Important! You need to dispose() the created {@link SyncableSubscription}
	 * object when it's no longer needed. When every subsribe() caller called
	 * dispose(), the underlying subscription is unsubscribed and removed.
	 * 
	 * @param key Key to subscribe to
	 * @return Newly created {@link SyncableSubscription} instance containing the
	 *         value and a method to dispose() the subscription.
	 * @param <T> SyncableKey value type
	 */
	<T> SyncableSubscription<T> subscribe(SyncableKey<T> key);

	/**
	 * Returns a list of all subscribed syncable keys.
	 * 
	 * @return Set with all subscribed syncable keys in it.
	 */
	Set<SyncableKey<?>> getSubscribedSyncableKeys();

}
