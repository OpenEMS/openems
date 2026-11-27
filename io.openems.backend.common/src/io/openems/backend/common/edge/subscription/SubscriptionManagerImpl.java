package io.openems.backend.common.edge.subscription;

import static org.osgi.service.component.annotations.ServiceScope.SINGLETON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.backend.common.edge.EdgeClient;

@Component(scope = SINGLETON)
public class SubscriptionManagerImpl implements SubscriptionManager {

	private static final int SECONDS_BEFORE_STORE_UNLOAD = 300;

	private final Logger log = LoggerFactory.getLogger(SubscriptionManagerImpl.class);
	private final ConcurrentMap<SubscriptionKey, SubscriptionStore> storesByKey = new ConcurrentHashMap<>();
	private final List<SubscriptionHandlerFactory> handlerFactories = new ArrayList<>();
	private final ReadWriteLock handlersModifyLock = new ReentrantReadWriteLock();

	protected ScheduledExecutorService removeStoreExecutor;

	@Activate
	protected void activate() {
		this.removeStoreExecutor = Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder().setNameFormat("SubscriptionManager.RemoveStore").build());
	}

	@Deactivate
	protected void deactivate() {
		this.removeStoreExecutor.shutdownNow();
		this.removeStoreExecutor = null;

		this.handlersModifyLock.writeLock().lock();
		try {
			for (var store : this.storesByKey.values()) {
				this.unloadStore(store);
			}
			this.storesByKey.clear();
		} finally {
			this.handlersModifyLock.writeLock().unlock();
		}
	}

	@Override
	public void registerHandlerFactory(SubscriptionHandlerFactory factory) {
		this.log.info("Registered new handler factory {}", factory.getClass().getName());

		this.handlersModifyLock.writeLock().lock();
		try {
			this.handlerFactories.add(factory);

			for (var storeEntry : this.storesByKey.entrySet()) {
				var store = storeEntry.getValue();
				if (store.handlerFactory == null && factory.isHandling(storeEntry.getKey())) {
					this.attachHandlerFactoryToStore(store, storeEntry.getKey(), factory);
					this.log.info("Attached new handler factory {} to already existing subscription store {}",
							factory.getClass().getSimpleName(), storeEntry.getKey());
				}
			}
		} finally {
			this.handlersModifyLock.writeLock().unlock();
		}
	}

	@Override
	public void unregisterHandlerFactory(SubscriptionHandlerFactory factory) {
		this.log.info("Unregistered handler factory {}", factory.getClass().getName());

		this.handlersModifyLock.writeLock().lock();
		try {
			for (var store : this.storesByKey.values()) {
				if (store.handlerFactory == factory) {
					this.detachHandlerFromStore(store);
				}
			}

			this.handlerFactories.remove(factory);
		} finally {
			this.handlersModifyLock.writeLock().unlock();
		}
	}

	private void attachHandlerFactoryToStore(SubscriptionStore store, SubscriptionKey key,
			SubscriptionHandlerFactory factory) {
		store.handlerFactory = factory;

		try {
			store.handler = factory.createGlobalHandler(key, this);
			store.handler.subscribe();
		} catch (Exception ex) {
			this.log.error("Failed to initialize global handler with factory {} for subscription {}", factory, key, ex);
		}

		try {
			store.clientHandlerFactory = factory.createClientHandlerFactory(key, store.handler, this);
		} catch (Exception ex) {
			this.log.error("Failed to create client handler factory. Factory is {} and subscription {}", factory, key,
					ex);
		}

		if (store.clientHandlerFactory != null) {
			for (var subscriptionEntry : store.subscriptionByClient.entrySet()) {
				this.attachClientHandlerFactoryToSubscription(subscriptionEntry.getValue(), subscriptionEntry.getKey(),
						store.clientHandlerFactory);
			}
		}
	}

	private void attachClientHandlerFactoryToSubscription(Subscription subscription, EdgeClient client,
			SubscriptionHandlerFactory.ClientHandlerFactory clientHandlerFactory) {
		try {
			subscription.clientHandler = clientHandlerFactory.createClientSpecificHandler(client);
			subscription.clientHandler.subscribe();
		} catch (Exception ex) {
			this.log.error("Failed to subscribe client handler for client {} and subscription {}",
					client.getEdgeIdString(), subscription.subscriptionData, ex);
		}
	}

	private void detachHandlerFromStore(SubscriptionStore store) {
		for (var subscription : store.subscriptionByClient.values()) {
			this.detachClientHandlerFactory(subscription);
		}

		try {
			store.handler.unsubscribe();
		} catch (Exception ex) {
			this.log.error("Failed to unsubscribe global handler for subscription {}", store.key, ex);
		}
		store.handler = null;
		store.handlerFactory = null;
		store.clientHandlerFactory = null;
	}

	private void detachClientHandlerFactory(Subscription subscription) {
		if (subscription.clientHandler != null) {
			try {
				subscription.clientHandler.unsubscribe();
			} catch (Exception ex) {
				this.log.error("Failed to unsubscribe client handler for subscription {}",
						subscription.subscriptionData, ex);
			}
			subscription.clientHandler = null;
		}
	}

	private SubscriptionStore createNewStore(SubscriptionKey key) {
		var store = new SubscriptionStore(key);

		for (var handlerFactory : this.handlerFactories) {
			if (handlerFactory.isHandling(key)) {
				this.attachHandlerFactoryToStore(store, key, handlerFactory);
			}
		}

		return store;
	}

	private SubscriptionStore getOrCreateStore(SubscriptionKey key) {
		return this.storesByKey.computeIfAbsent(key, this::createNewStore);
	}

	private SubscriptionStore getStore(SubscriptionKey key) {
		return this.storesByKey.get(key);
	}

	protected void removeStoreIfPossible(SubscriptionStore store) {
		this.handlersModifyLock.writeLock().lock();
		try {
			if (!store.subscriptionByClient.isEmpty()) {
				return;
			}

			this.storesByKey.remove(store.key, store);
		} finally {
			this.handlersModifyLock.writeLock().unlock();
		}

		this.unloadStore(store);
	}

	private void unloadStore(SubscriptionStore store) {
		for (var subscription : store.subscriptionByClient.values()) {
			this.detachClientHandlerFactory(subscription);
		}

		store.subscriptionByClient.clear();
		this.detachHandlerFromStore(store);
	}

	@Override
	public List<EdgeClient> getSubscribedClients(SubscriptionKey key) {
		var store = this.getStore(key);
		if (store == null) {
			return Collections.emptyList();
		}

		return store.getAllClients();
	}

	@Override
	public ActiveClientSubscription subscribe(SubscriptionKey key, EdgeClient client) {
		this.handlersModifyLock.readLock().lock();
		try {
			var store = this.getOrCreateStore(key);

			var existingSubscription = store.getByClient(client);
			if (existingSubscription != null) {
				return mapToActiveClientSubscription(existingSubscription, store);
			}

			var subscription = this.createNewSubscription(store, client);
			store.setClientSubscription(client, subscription);

			return mapToActiveClientSubscription(subscription, store);
		} finally {
			this.handlersModifyLock.readLock().unlock();
		}
	}

	private static ActiveClientSubscription mapToActiveClientSubscription(Subscription subscription,
			SubscriptionStore store) {
		return new ActiveClientSubscription(subscription.subscriptionData, store.handler, subscription.clientHandler);
	}

	@Override
	public SubscriptionData unsubscribe(SubscriptionKey key, EdgeClient client) {
		this.handlersModifyLock.readLock().lock();
		try {
			var store = this.getStore(key);
			if (store == null) {
				return null;
			}

			var existingSubscription = store.getByClient(client);
			if (existingSubscription == null) {
				return null;
			}

			this.detachClientHandlerFactory(existingSubscription);
			store.removeClient(client);

			if (store.subscriptionByClient.isEmpty()) {
				this.scheduleStoreRemove(store);
			}

			return existingSubscription.subscriptionData;
		} finally {
			this.handlersModifyLock.readLock().unlock();
		}
	}

	protected void scheduleStoreRemove(SubscriptionStore store) {
		this.removeStoreExecutor.schedule(() -> this.removeStoreIfPossible(store), SECONDS_BEFORE_STORE_UNLOAD,
				TimeUnit.SECONDS);
	}

	@Override
	public List<SubscriptionData> getSubscriptionsByClient(EdgeClient client) {
		return this.storesByKey.values().stream() //
				.map(store -> store.getByClient(client)) //
				.filter(Objects::nonNull) //
				.map(subscription -> subscription.subscriptionData) //
				.toList();
	}

	@Override
	public void cleanupClient(EdgeClient client) {
		var subscriptions = this.getSubscriptionsByClient(client);
		for (var subscription : subscriptions) {
			this.unsubscribe(subscription.key(), client);
		}
	}

	private Subscription createNewSubscription(SubscriptionStore store, EdgeClient client) {
		var data = new SubscriptionData(UUID.randomUUID(), store.key, Instant.now());
		var clientSubscription = new Subscription(data);

		if (store.clientHandlerFactory != null) {
			this.attachClientHandlerFactoryToSubscription(clientSubscription, client, store.clientHandlerFactory);
		}

		return clientSubscription;
	}

	protected static class SubscriptionStore {
		private final SubscriptionKey key;
		private final ConcurrentMap<EdgeClient, Subscription> subscriptionByClient;
		private SubscriptionHandlerFactory handlerFactory;
		private SubscriptionHandlerFactory.ClientHandlerFactory clientHandlerFactory;
		private SubscriptionHandler handler;

		public SubscriptionStore(SubscriptionKey key) {
			this.key = key;
			this.subscriptionByClient = new MapMaker().weakKeys().makeMap();
		}

		public List<EdgeClient> getAllClients() {
			return List.copyOf(this.subscriptionByClient.keySet());
		}

		public Subscription getByClient(EdgeClient client) {
			return this.subscriptionByClient.get(client);
		}

		public void setClientSubscription(EdgeClient client, Subscription data) {
			this.subscriptionByClient.put(client, data);
		}

		public void removeClient(EdgeClient client) {
			this.subscriptionByClient.remove(client);
		}
	}

	protected static class Subscription {
		private final SubscriptionData subscriptionData;
		private SubscriptionHandler clientHandler;

		public Subscription(SubscriptionData subscriptionData) {
			this.subscriptionData = subscriptionData;
		}

		@Override
		public String toString() {
			return this.subscriptionData.toString();
		}
	}

}
