package io.openems.backend.common.jsonrpc.syncable;

import io.openems.backend.common.edge.subscription.SubscriptionHandler;
import io.openems.backend.common.edge.subscription.SubscriptionManager;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.utils.BehaviorSubject;

public class SyncableUtils {
	private SyncableUtils() {
	}

	/**
	 * Tries to extract the {@link SyncableEntry} from clients subscription data.
	 * 
	 * @param subscription Subscription data to extract the entry from.
	 * @return Extracted {@link SyncableEntry} or null if no supported handler was
	 *         found.
	 */
	public static BehaviorSubject<SyncableEntry<?>> getSyncableEntryBySubscription(
			SubscriptionManager.ActiveClientSubscription subscription) {
		var entry = extractSyncableEntryFromHandler(subscription.clientHandler());
		if (entry != null) {
			return entry;
		}

		entry = extractSyncableEntryFromHandler(subscription.globalHandler());
		if (entry != null) {
			return entry;
		}

		return null;
	}

	private static BehaviorSubject<SyncableEntry<?>> extractSyncableEntryFromHandler(SubscriptionHandler handler) {
		if (handler instanceof DynamicSyncableSubscriptionHandler<?> syncableHandler) {
			@SuppressWarnings("unchecked")
			var entry = (BehaviorSubject<SyncableEntry<?>>) (BehaviorSubject<?>) syncableHandler.getEntry();
			return entry;
		}

		return null;
	}
}
