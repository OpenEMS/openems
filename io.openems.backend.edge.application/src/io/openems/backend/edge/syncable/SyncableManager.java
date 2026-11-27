package io.openems.backend.edge.syncable;

import static org.osgi.service.component.annotations.ServiceScope.SINGLETON;

import java.time.Duration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.openems.backend.common.edge.EdgeClient;
import io.openems.backend.common.edge.subscription.SubscriptionData;
import io.openems.backend.common.edge.subscription.SubscriptionHandler;
import io.openems.backend.common.edge.subscription.SubscriptionHandlerFactory;
import io.openems.backend.common.edge.subscription.SubscriptionKey;
import io.openems.backend.common.edge.subscription.SubscriptionManager;
import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.backend.common.jsonrpc.syncable.SyncableSubscriptionKey;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.syncable.SyncableProviderType;
import io.openems.common.utils.ThrottledLoopExecutor;

@Component(scope = SINGLETON, immediate = true, service = SyncableManager.class)
public class SyncableManager {
	private static final int MAX_UPDATE_NOTIFICATIONS_PER_SECOND = 300;

	private ThrottledLoopExecutor sendExecutor;
	private SyncableSubscriptionHandlerFactory handlerFactory;

	@Reference
	private SubscriptionManager subscriptionManager;

	@Reference
	private SyncableRepository syncableRepository;

	@Activate
	protected synchronized void activate() {
		this.sendExecutor = new ThrottledLoopExecutor("SyncableManager",
				new ThrottledLoopExecutor.MaxExecutionsPerDurationThrottler(Duration.ofSeconds(1),
						MAX_UPDATE_NOTIFICATIONS_PER_SECOND));

		this.handlerFactory = new SyncableSubscriptionHandlerFactory();
		this.subscriptionManager.registerHandlerFactory(this.handlerFactory);
	}

	@Deactivate
	protected synchronized void deactivate() {
		if (this.handlerFactory != null) {
			this.subscriptionManager.unregisterHandlerFactory(this.handlerFactory);
			this.handlerFactory = null;
		}

		if (this.sendExecutor != null) {
			this.sendExecutor.dispose();
			this.sendExecutor = null;
		}
	}

	/**
	 * Subscribes a specific Edge Client to the given Syncable key. This method
	 * should only be called for syncable keys that are provided by BACKEND_EDGE.
	 * For syncable keys with BACKEND provider, the SyncableSubscribeRequest should
	 * be forwarded to backend.
	 *
	 * @param key    the syncable key to subscribe to
	 * @param client the edge client that wants to receive subscribe
	 * @return the data about the subscription
	 */
	public SubscriptionManager.ActiveClientSubscription subscribe(SyncableKey<?> key, EdgeClient client) {
		return this.subscriptionManager.subscribe(new SyncableSubscriptionKey(key), client);
	}

	/**
	 * Removes an existing subscription of an Edge client for the given Syncable
	 * key.
	 *
	 * @param key    the syncable key to unsubscribe from
	 * @param client the edge client whose subscription should be removed
	 * @return the subscription data that was removed, or null if no subscription
	 *         existed
	 */
	public SubscriptionData unsubscribe(SyncableKey<?> key, EdgeClient client) {
		return this.subscriptionManager.unsubscribe(new SyncableSubscriptionKey(key), client);
	}

	private class SyncableSubscriptionHandlerFactory implements SubscriptionHandlerFactory {
		@Override
		public boolean isHandling(SubscriptionKey key) {
			return key instanceof SyncableSubscriptionKey(SyncableKey<?> syncableKey)
					&& syncableKey.getProviderType() == SyncableProviderType.BACKEND_EDGE_SERVER;
		}

		@Override
		public SubscriptionHandler createGlobalHandler(SubscriptionKey key, SubscriptionManager manager) {
			var syncableKey = ((SyncableSubscriptionKey) key).syncableKey();

			return new StaticSyncableSubscriptionHandler(syncableKey, SyncableManager.this.syncableRepository,
					SyncableManager.this.subscriptionManager, SyncableManager.this.sendExecutor);
		}

		@Override
		public SubscriptionHandlerFactory.ClientHandlerFactory createClientHandlerFactory(SubscriptionKey key,
				SubscriptionHandler globalHandler, SubscriptionManager subscriptionManager) {
			return null;
		}
	}

}
