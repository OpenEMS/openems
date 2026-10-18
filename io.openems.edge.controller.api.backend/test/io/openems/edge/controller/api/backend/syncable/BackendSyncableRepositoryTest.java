package io.openems.edge.controller.api.backend.syncable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.SyncableChangeNotification;
import io.openems.common.jsonrpc.request.SyncableSubscribeRequest;
import io.openems.common.jsonrpc.request.SyncableUnsubscribeRequest;
import io.openems.common.jsonrpc.response.SyncableSubscribeResponse;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.edge.controller.api.backend.dummy.DummyControllerApiBackend;

class BackendSyncableRepositoryTest {
	private static DummyControllerApiBackend backend;
	private static MockBackendSyncableRepository syncableRepository;
	private static boolean receivedUnsubscribe = false;

	private static final SyncableKey<Boolean> syncableKey = new SyncableKey.TimeDataTransferAllowed();

	private static SyncableEntry<Boolean> createTestEntry(Boolean value) {
		return new SyncableEntry<>(syncableKey, value, Instant.now());
	}

	@BeforeAll
	static void setUp() {
		backend = new DummyControllerApiBackend("backend0");
		backend.setConnected(true);
		backend.setRequestHandler(BackendSyncableRepositoryTest::handleRequestToBackend);

		syncableRepository = new MockBackendSyncableRepository(backend);
		syncableRepository.activate();
	}

	@AfterAll
	static void destroy() {
		syncableRepository.deactivate();
	}

	@Test
	void testSubscribe() throws Exception {
		var subscription = syncableRepository.subscribe(syncableKey);
		var entry = subscription.getEntry().getValue();
		assertTrue(entry.value());

		backend.simulateNotificationReceive(new SyncableChangeNotification(createTestEntry(false)));
		syncableRepository.ensureThatAllAsyncTasksAreExecuted();
		entry = subscription.getEntry().getValue();
		assertFalse(entry.value());

		receivedUnsubscribe = false;
		assertEquals(1, syncableRepository.getSubscribedSyncableKeys().size());
		subscription.dispose();
		assertEquals(0, syncableRepository.getSubscribedSyncableKeys().size());
		assertTrue(receivedUnsubscribe);
	}

	static CompletableFuture<? extends JsonrpcResponseSuccess> handleRequestToBackend(JsonrpcRequest request) {
		switch (request) {
		case SyncableSubscribeRequest req -> {
			if (!req.getKey().equals(syncableKey)) {
				throw new RuntimeException(
						"Received invalid syncable key. Expected " + syncableKey + ", got " + req.getKey());
			}
			var response = new SyncableSubscribeResponse(req.getId(), createTestEntry(true));
			return CompletableFuture.completedFuture(response);
		}
		case SyncableUnsubscribeRequest req -> {
			receivedUnsubscribe = true;
			return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(req.getId()));
		}
		default -> throw new RuntimeException("Unexpected request type: " + request.getClass());
		}

	}
}
