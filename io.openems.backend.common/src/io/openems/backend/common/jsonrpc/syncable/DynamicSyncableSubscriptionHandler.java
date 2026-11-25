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
	private final List<EdgeClient> clients;

	protected DynamicSyncableSubscriptionHandler(//
			SyncableKey<VALUE> key, //
			ThrottledLoopExecutor sendExecutor, //
			List<EdgeClient> clients //
	) {
		this.key = key;
		this.clients = clients;

		this.value.subscribe(newEntry -> {
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

	protected final List<EdgeClient> getClients() {
		return this.clients;
	}
}
