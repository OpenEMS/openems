package io.openems.backend.common.edge.subscription;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.openems.backend.common.edge.EdgeClient;

class SubscriptionManagerTest {
	private static final TestSubscriptionKey TEST_SUBSCRIPTION_KEY = new TestSubscriptionKey("KEY-1");
	private static final TestSubscriptionKey TEST_SUBSCRIPTION_KEY_2 = new TestSubscriptionKey("KEY-2");
	private static TestableSubscriptionManagerImpl manager;

	@BeforeAll
	static void setUp() {
		manager = new TestableSubscriptionManagerImpl();
		manager.activate();
	}

	@AfterAll
	static void cleanup() {
		manager.deactivate();
		manager = null;
	}

	@Test
	void testSubscribe() {
		var edgeClient = mock(EdgeClient.class);
		when(edgeClient.getEdgeId()).thenReturn("DUMMY-1");

		assertTrue(manager.getSubscriptionsByClient(edgeClient).isEmpty());
		assertTrue(manager.getSubscribedClients(TEST_SUBSCRIPTION_KEY).isEmpty());

		manager.subscribe(TEST_SUBSCRIPTION_KEY, edgeClient);
		manager.subscribe(TEST_SUBSCRIPTION_KEY_2, edgeClient);

		var subscriptions = manager.getSubscriptionsByClient(edgeClient).stream().map(SubscriptionData::key).toArray();
		assertArrayEquals(new SubscriptionKey[] { TEST_SUBSCRIPTION_KEY, TEST_SUBSCRIPTION_KEY_2 }, subscriptions);
		assertArrayEquals(new EdgeClient[] { edgeClient },
				manager.getSubscribedClients(TEST_SUBSCRIPTION_KEY).toArray());
		assertArrayEquals(new EdgeClient[] { edgeClient },
				manager.getSubscribedClients(TEST_SUBSCRIPTION_KEY_2).toArray());

		manager.cleanupClient(edgeClient);
		assertTrue(manager.getSubscriptionsByClient(edgeClient).isEmpty());
		assertTrue(manager.getSubscribedClients(TEST_SUBSCRIPTION_KEY).isEmpty());
		assertTrue(manager.getSubscribedClients(TEST_SUBSCRIPTION_KEY_2).isEmpty());
	}

	@Test
	void testWithoutClientHandler() throws Exception {
		var edgeClient = mock(EdgeClient.class);
		when(edgeClient.getEdgeId()).thenReturn("DUMMY-1");

		var globalHandler = mock(SubscriptionHandler.class);

		var handlerFactory = mock(SubscriptionHandlerFactory.class);
		when(handlerFactory.createGlobalHandler(TEST_SUBSCRIPTION_KEY, manager)).thenReturn(globalHandler);
		when(handlerFactory.isHandling(TEST_SUBSCRIPTION_KEY)).thenReturn(true);

		manager.registerHandlerFactory(handlerFactory);

		manager.subscribe(TEST_SUBSCRIPTION_KEY, edgeClient);
		verify(handlerFactory).createGlobalHandler(TEST_SUBSCRIPTION_KEY, manager);
		verify(globalHandler).subscribe();
		verify(handlerFactory).createClientHandlerFactory(Mockito.any(), Mockito.any(), Mockito.any());

		manager.unsubscribe(TEST_SUBSCRIPTION_KEY, edgeClient);
		manager.waitForAllAsyncTasks();
		verify(globalHandler).unsubscribe();

		manager.unregisterHandlerFactory(handlerFactory);
	}

	@Test
	void testWithTwoClientsAndClientHandler() throws Exception {
		var edgeClient1 = mock(EdgeClient.class);
		when(edgeClient1.getEdgeId()).thenReturn("DUMMY-1");
		var clientHandler1 = mock(SubscriptionHandler.class);

		var edgeClient2 = mock(EdgeClient.class);
		when(edgeClient2.getEdgeId()).thenReturn("DUMMY-2");
		var clientHandler2 = mock(SubscriptionHandler.class);

		var globalHandler = mock(SubscriptionHandler.class);

		var clientHandlerFactory = mock(SubscriptionHandlerFactory.ClientHandlerFactory.class);
		when(clientHandlerFactory.createClientSpecificHandler(edgeClient1)).thenReturn(clientHandler1);
		when(clientHandlerFactory.createClientSpecificHandler(edgeClient2)).thenReturn(clientHandler2);

		var handlerFactory = mock(SubscriptionHandlerFactory.class);
		when(handlerFactory.createGlobalHandler(TEST_SUBSCRIPTION_KEY, manager)).thenReturn(globalHandler);
		when(handlerFactory.createClientHandlerFactory(TEST_SUBSCRIPTION_KEY, globalHandler, manager))
				.thenReturn(clientHandlerFactory);
		when(handlerFactory.isHandling(TEST_SUBSCRIPTION_KEY)).thenReturn(true);

		manager.registerHandlerFactory(handlerFactory);

		manager.subscribe(TEST_SUBSCRIPTION_KEY, edgeClient1);
		verify(clientHandler1).subscribe();
		manager.subscribe(TEST_SUBSCRIPTION_KEY, edgeClient2);
		verify(clientHandler2).subscribe();

		verify(clientHandlerFactory, Mockito.times(2)).createClientSpecificHandler(Mockito.any());

		manager.unsubscribe(TEST_SUBSCRIPTION_KEY, edgeClient1);
		manager.waitForAllAsyncTasks();
		verify(clientHandler1).unsubscribe();
		verify(globalHandler, Mockito.never()).unsubscribe();

		manager.unsubscribe(TEST_SUBSCRIPTION_KEY, edgeClient2);
		manager.waitForAllAsyncTasks();
		verify(clientHandler2).unsubscribe();
		verify(globalHandler).unsubscribe();

		manager.unregisterHandlerFactory(handlerFactory);
	}

	@Test
	void testHandlerReplace() {
		var edgeClient = mock(EdgeClient.class);
		when(edgeClient.getEdgeId()).thenReturn("DUMMY-1");

		manager.subscribe(TEST_SUBSCRIPTION_KEY, edgeClient);

		var globalHandler = mock(SubscriptionHandler.class);
		var handlerFactory = mock(SubscriptionHandlerFactory.class);
		when(handlerFactory.createGlobalHandler(TEST_SUBSCRIPTION_KEY, manager)).thenReturn(globalHandler);
		when(handlerFactory.isHandling(TEST_SUBSCRIPTION_KEY)).thenReturn(true);

		manager.registerHandlerFactory(handlerFactory);
		verify(globalHandler).subscribe();

		manager.unregisterHandlerFactory(handlerFactory);
		verify(globalHandler).unsubscribe();

		var newGlobalHandler = mock(SubscriptionHandler.class);
		var newHandlerFactory = mock(SubscriptionHandlerFactory.class);
		when(newHandlerFactory.createGlobalHandler(TEST_SUBSCRIPTION_KEY, manager)).thenReturn(newGlobalHandler);
		when(newHandlerFactory.isHandling(TEST_SUBSCRIPTION_KEY)).thenReturn(true);

		manager.registerHandlerFactory(newHandlerFactory);
		verify(newGlobalHandler).subscribe();

		manager.unregisterHandlerFactory(newHandlerFactory);
		manager.unsubscribe(TEST_SUBSCRIPTION_KEY, edgeClient);
	}

	private static record TestSubscriptionKey(String key) implements SubscriptionKey {
	}

	private static class TestableSubscriptionManagerImpl
			extends io.openems.backend.common.edge.subscription.SubscriptionManagerImpl {
		private final ConcurrentLinkedDeque<Future<?>> futures = new ConcurrentLinkedDeque<>();

		@Override
		protected void scheduleStoreRemove(SubscriptionStore store) {
			this.futures.add(this.removeStoreExecutor.submit(() -> this.removeStoreIfPossible(store)));
		}

		protected void waitForAllAsyncTasks() throws ExecutionException, InterruptedException, TimeoutException {
			while (!this.futures.isEmpty()) {
				var future = this.futures.pop();
				future.get(10, TimeUnit.SECONDS);
			}
		}
	}
}
