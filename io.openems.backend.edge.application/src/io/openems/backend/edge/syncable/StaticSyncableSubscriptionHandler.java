package io.openems.backend.edge.syncable;

import java.util.Objects;
import java.util.function.Consumer;

import io.openems.backend.common.edge.subscription.SubscriptionHandler;
import io.openems.backend.common.edge.subscription.SubscriptionKey;
import io.openems.backend.common.edge.subscription.SubscriptionManager;
import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.backend.common.jsonrpc.syncable.SyncableSubscriptionKey;
import io.openems.common.jsonrpc.notification.SyncableChangeNotification;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.ThrottledLoopExecutor;

public class StaticSyncableSubscriptionHandler implements SubscriptionHandler {
	private final SyncableRepository repository;
	private final SyncableKey<?> key;

	private final SubscriptionManager subscriptionManager;
	private final ThrottledLoopExecutor sendExecutor;

	private final Consumer<SyncableRepository.ChangeEvent> repositoryChangeEventFunc = this::onRepositoryChange;

	private SyncableEntry<?> trackedEntry;

	public StaticSyncableSubscriptionHandler(SyncableKey<?> key, SyncableRepository repository,
			SubscriptionManager subscriptionManager, ThrottledLoopExecutor sendExecutor) {
		this.key = key;
		this.repository = repository;
		this.subscriptionManager = subscriptionManager;
		this.sendExecutor = sendExecutor;
	}

	@Override
	public void subscribe() {
		this.trackedEntry = this.repository.getStaticEntry(this.key);
		this.repository.getChangeEvent().subscribe(this.repositoryChangeEventFunc);
	}

	@Override
	public void unsubscribe() {
		this.repository.getChangeEvent().unsubscribe(this.repositoryChangeEventFunc);
	}

	private synchronized void onRepositoryChange(SyncableRepository.ChangeEvent event) {
		var newEntry = this.repository.getStaticEntry(this.key);
		if (!Objects.equals(this.trackedEntry, newEntry)) {
			this.trackedEntry = newEntry;
			this.sendUpdates(newEntry);
		}
	}

	protected SubscriptionKey getSubscriptionKey() {
		return new SyncableSubscriptionKey(this.key);
	}

	public SyncableEntry<?> getTrackedEntry() {
		return this.trackedEntry;
	}

	private synchronized void sendUpdates(SyncableEntry<?> entry) {
		final var clients = this.subscriptionManager.getSubscribedClients(this.getSubscriptionKey());
		final var notification = new SyncableChangeNotification(entry);

		this.sendExecutor.add(clients, client -> client.send(notification));
	}

}
