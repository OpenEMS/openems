package io.openems.edge.controller.api.backend.syncable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.notification.SyncableChangeNotification;
import io.openems.common.jsonrpc.request.SyncableSubscribeRequest;
import io.openems.common.jsonrpc.request.SyncableUnsubscribeRequest;
import io.openems.common.jsonrpc.response.SyncableSubscribeResponse;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;

public class SubscriptionHandler<T> {
	private final Logger log = LoggerFactory.getLogger(SubscriptionHandler.class);

	private static final int SECONDS_TO_WAIT_BEFORE_RETRY_AFTER_SUBSCRIBE_ERROR = 30;

	private final SyncableKey<T> key;
	private final BackendSyncableRepository repository;
	private final BehaviorSubject<SyncableEntry<T>> entry = new BehaviorSubject<>(null);
	private final Set<BackendSyncableSubscription<?>> attachedSubscriptions = Collections
			.newSetFromMap(new WeakHashMap<>());

	private Object subscribeToken;
	private CompletableFuture<SyncableEntry<T>> subscribeTask;

	/**
	 * Creates a handler for one Syncable key and immediately starts the initial
	 * subscribe flow.
	 *
	 * @param key        the syncable key to subscribe to
	 * @param repository the backend repository used for request transport
	 */
	public SubscriptionHandler(SyncableKey<T> key, BackendSyncableRepository repository) {
		this.key = key;
		this.repository = repository;
		this.resendSubscription();
	}

	/**
	 * Sends a subscribe request for the configured key.
	 *
	 * <p>
	 * Any previous in-flight subscribe task is canceled. On success, the latest
	 * entry is published and logged. On failure or timeout, a delayed retry is
	 * scheduled.
	 */
	public synchronized void resendSubscription() {
		var subscribeToken = this.setNewSubscribeToken();

		if (this.subscribeTask != null) {
			this.subscribeTask.cancel(true);
		}

		this.subscribeTask = this.sendSubscribeRequest() //
				.orTimeout(60, TimeUnit.SECONDS) //
				.whenComplete((entry, ex) -> {
					this.entry.setValue(entry);

					if (subscribeToken == this.subscribeToken) {
						if (ex == null) {
							this.log.info("Subscribed to syncable key {}.", this.key);
						} else {
							var errorMessage = ex instanceof TimeoutException ? "Timeout" : ex.getMessage();
							this.log.warn("Failed to subscribe syncable key {}, will retry after some time: {}",
									this.key, errorMessage);
							this.resendSubscriptionAfterSomeTime();
						}
					}
				});
	}

	/**
	 * Schedules a delayed re-subscription attempt.
	 *
	 * <p>
	 * The token guard ensures only the latest retry schedule can trigger a new
	 * subscribe attempt.
	 */
	private void resendSubscriptionAfterSomeTime() {
		var subscribeToken = this.setNewSubscribeToken();
		CompletableFuture.delayedExecutor(SECONDS_TO_WAIT_BEFORE_RETRY_AFTER_SUBSCRIBE_ERROR, TimeUnit.SECONDS)
				.execute(() -> {
					if (subscribeToken == this.subscribeToken) {
						this.resendSubscription();
					}
				});
	}

	/**
	 * Sends an unsubscribe request.
	 *
	 * <p>
	 * If another handler is already active for the same key, no unsubscribe request
	 * is sent to avoid unsubscribing a still-used backend subscription.
	 */
	public void unsubscribe() {
		this.repository.getBackend() //
				.thenCompose(backend -> {
					if (this.repository.getHandler(this.key) != null) {
						// Another handler is already registered - we should not unsubscribe.
						return CompletableFuture.completedFuture(null);
					}
					return backend.sendRequest(null, new SyncableUnsubscribeRequest(this.key));
				}) //
				.whenComplete((result, ex) -> {
					if (ex != null) {
						this.log.warn("Failed to unsubscribe syncable key {}.", this.key, ex);
					} else if (result != null) {
						this.log.info("Unsubscribed syncable key {}.", this.key);
					}
				});
	}

	private Object setNewSubscribeToken() {
		var subscribeToken = new Object();
		this.subscribeToken = subscribeToken;
		return subscribeToken;
	}

	private CompletableFuture<SyncableEntry<T>> sendSubscribeRequest() {
		return this.repository.getBackend() //
				.thenCompose(backend -> backend.sendRequest(null, new SyncableSubscribeRequest(this.key)))
				.thenApply(SyncableSubscribeResponse::from) //
				.thenApply(response -> {
					@SuppressWarnings("unchecked")
					var entry = (SyncableEntry<T>) response.getEntry();
					return entry;
				});
	}

	public SyncableKey<T> getKey() {
		return this.key;
	}

	public BehaviorSubject<SyncableEntry<T>> getEntry() {
		return this.entry;
	}

	/**
	 * Applies an incoming syncable change notification to the local entry subject.
	 *
	 * @param notification the notification carrying the updated entry
	 */
	public void handleUpdateNotification(SyncableChangeNotification notification) {
		@SuppressWarnings("unchecked")
		var typedEntry = (SyncableEntry<T>) notification.getEntry();
		this.entry.setValue(typedEntry);
	}

	/**
	 * Registers a subscription that depends on this handler.
	 *
	 * @param subscription the subscription to attach
	 */
	public void attachSubscription(BackendSyncableSubscription<?> subscription) {
		synchronized (this.attachedSubscriptions) {
			this.attachedSubscriptions.add(subscription);
		}
	}

	/**
	 * Unregisters a previously attached dependent subscription.
	 *
	 * @param subscription the subscription to detach
	 */
	public void detachSubscription(BackendSyncableSubscription<?> subscription) {
		synchronized (this.attachedSubscriptions) {
			this.attachedSubscriptions.remove(subscription);
		}
	}

	/**
	 * Gets the number of currently attached dependent subscriptions.
	 *
	 * @return number of attached subscriptions
	 */
	public int getAttachedSubscriptionCount() {
		synchronized (this.attachedSubscriptions) {
			return this.attachedSubscriptions.size();
		}
	}
}
