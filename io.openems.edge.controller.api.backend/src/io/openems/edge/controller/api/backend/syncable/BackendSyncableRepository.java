package io.openems.edge.controller.api.backend.syncable;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.jsonrpc.notification.SyncableChangeNotification;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.common.services.syncable.SyncableSubscription;
import io.openems.edge.controller.api.backend.api.BackendEvent;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;
import io.openems.edge.controller.api.backend.api.NoBackendAvailableException;

@Component(immediate = true)
public class BackendSyncableRepository implements SyncableRepository {
	private static final int DELAYS_IN_SECONDS_BEFORE_UNSUBSCRIBE = 30;

	private final Logger log = LoggerFactory.getLogger(BackendSyncableRepository.class);

	private final Map<SyncableKey<?>, SubscriptionHandler<?>> handlers = new ConcurrentHashMap<>();
	private final BehaviorSubject<ControllerApiBackend> backend = new BehaviorSubject<>(null);
	private final Consumer<BackendEvent> onBackendEventFunc = event -> this.runAsync(() -> this.onBackendEvent(event));

	protected ExecutorService executor;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(&(enabled=true)(|(supportedFeatures=DATA_PROVIDER)(useDefaultFeatures=true)))")
	protected synchronized void bindBackend(ControllerApiBackend backend) {
		backend.getEventHandler().subscribe(this.onBackendEventFunc);
		this.backend.setValue(backend);

		this.runAsync(this::resendAllSubscriptions);
	}

	protected synchronized void unbindBackend(ControllerApiBackend backend) {
		if (backend != null) {
			backend.getEventHandler().unsubscribe(this.onBackendEventFunc);
		}
		this.backend.setValue(null);
	}

	@Activate
	protected synchronized void activate() {
		this.executor = Executors
				.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("BackendSyncableRepository").build());
	}

	@Deactivate
	protected synchronized void deactivate() {
		if (this.executor != null) {
			this.executor.shutdownNow();
			this.executor = null;
		}
	}

	protected synchronized void runAsync(Runnable r) {
		if (this.executor != null) {
			this.executor.submit(r);
		}
	}

	protected void resendAllSubscriptions() {
		for (var handler : this.handlers.values()) {
			handler.resendSubscription();
		}
	}

	protected void onBackendEvent(BackendEvent event) {
		switch (event) {
		case BackendEvent.BackendConnectionEstablishedEvent e -> {
			this.resendAllSubscriptions();
		}
		case BackendEvent.Notification(var notification) -> {
			if (notification.getMethod().equals(SyncableChangeNotification.METHOD)) {
				try {
					this.onChangeNotification(SyncableChangeNotification.from(notification));
				} catch (Exception ex) {
					this.log.warn("Failed to handle syncable change notification", ex);
				}
			}
		}
		}
	}

	protected void onChangeNotification(SyncableChangeNotification notification) {
		var handler = this.getHandler(notification.getEntry().key());
		if (handler != null) {
			handler.handleUpdateNotification(notification);
		}
	}

	protected CompletableFuture<ControllerApiBackend> getBackend() {
		return this.backend.waitForNonNullValue(Duration.ofSeconds(60)) //
				.exceptionally(ex -> {
					var cause = ex instanceof CompletionException ? ex.getCause() : ex;
					if (cause instanceof TimeoutException) {
						throw new NoBackendAvailableException();
					}
					throw new CompletionException(ex);
				});
	}

	protected <T> SubscriptionHandler<T> getOrCreateHandler(SyncableKey<T> key) {
		@SuppressWarnings("unchecked")
		var handler = (SubscriptionHandler<T>) this.handlers.computeIfAbsent(key,
				k -> new SubscriptionHandler<>(k, this));
		return handler;
	}

	protected <T> SubscriptionHandler<T> getHandler(SyncableKey<T> key) {
		@SuppressWarnings("unchecked")
		var handler = (SubscriptionHandler<T>) this.handlers.get(key);
		return handler;
	}

	@Override
	public Set<SyncableKey<?>> getSubscribedSyncableKeys() {
		return Set.copyOf(this.handlers.keySet());
	}

	@Override
	public <T> SyncableSubscription<T> subscribe(SyncableKey<T> key) {
		var handler = this.getOrCreateHandler(key);
		var subscription = new BackendSyncableSubscription<>(handler, this::onSubscriptionUnsubscribe);
		handler.attachSubscription(subscription);
		return subscription;
	}

	protected <T> void onSubscriptionUnsubscribe(BackendSyncableSubscription<T> subscription) {
		var handler = this.getHandler(subscription.getKey());
		if (handler == null) {
			return;
		}

		handler.detachSubscription(subscription);

		if (handler.getAttachedSubscriptionCount() == 0) {
			this.startHandlerUnsubscribeTimer(handler);
		}
	}

	protected <T> void startHandlerUnsubscribeTimer(SubscriptionHandler<T> handler) {
		CompletableFuture.delayedExecutor(DELAYS_IN_SECONDS_BEFORE_UNSUBSCRIBE, TimeUnit.SECONDS)
				.execute(() -> this.unsubscribeHandler(handler));
	}

	protected <T> void unsubscribeHandler(SubscriptionHandler<T> handler) {
		if (handler.getAttachedSubscriptionCount() == 0 && this.handlers.remove(handler.getKey(), handler)) {
			handler.unsubscribe();
		}
	}
}
