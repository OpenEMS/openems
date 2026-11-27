package io.openems.backend.common.jsonrpc.syncable;

import java.util.List;

import io.openems.backend.common.edge.EdgeClient;
import io.openems.backend.common.edge.subscription.SubscriptionHandler;
import io.openems.common.jsonrpc.notification.SyncableChangeNotification;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.ThrottledLoopExecutor;

public abstract class DynamicSyncableSubscriptionHandler<VALUE> implements SubscriptionHandler {
	protected final SyncableKey<VALUE> key;
	protected final BehaviorSubject<SyncableEntry<VALUE>> value = new BehaviorSubject<>(null);

	protected DynamicSyncableSubscriptionHandler(SyncableKey<VALUE> key, ThrottledLoopExecutor sendExecutor) {
		this.key = key;

		this.value.subscribe(newEntry -> {
			var clients = this.getClients();
			if (newEntry != null && !clients.isEmpty()) {
				sendExecutor.add(clients, c -> c.send(new SyncableChangeNotification(newEntry)));
			}
		});
	}

	public SyncableKey<VALUE> getKey() {
		return this.key;
	}

	public BehaviorSubject<SyncableEntry<VALUE>> getEntry() {
		return this.value;
	}

	protected abstract List<EdgeClient> getClients();
}
