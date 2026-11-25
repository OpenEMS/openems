package io.openems.backend.metadata.odoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.backend.common.metadata.User;
import io.openems.backend.common.test.DummyUser;
import io.openems.backend.metrics.prometheus.DebugExecutor;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.session.Role;

class MetadataOdooSuppliableKeyTest {

	private static final String EDGE_ID = "edge0";
	private static final String APP_ID = "app0";
	private static final String MASTER_KEY = "master-key";

	private ExecutorService executor;
	private TestMetadataOdoo metadata;
	private OpenemsBackendOem oem;

	@BeforeEach
	void setUp() {
		this.executor = Executors.newSingleThreadExecutor();
		this.metadata = new TestMetadataOdoo();
		this.oem = mock(OpenemsBackendOem.class);
		when(this.oem.getAppCenterMasterKey()).thenReturn(MASTER_KEY);
		this.metadata.requestExecutor = new DebugExecutor("test", this.executor) {
			@Override
			public <T, E extends Exception> CompletableFuture<T> submit(String id, ThrowingSupplier<T, E> command) {
				try {
					return CompletableFuture.completedFuture(command.get());
				} catch (Exception e) {
					return CompletableFuture.failedFuture(e);
				}
			}
		};
		this.metadata.oem = this.oem;
	}

	@AfterEach
	void tearDown() {
		this.executor.shutdownNow();
	}

	@ParameterizedTest
	@MethodSource("usersAllowedToReceiveMasterKey")
	void allowedUserCanReceiveMasterKey(boolean isFreeApp, Role edgeRole, User user) {
		this.metadata.isFreeApp = isFreeApp;
		this.metadata.edgeRole = edgeRole;

		assertEquals(MASTER_KEY, this.metadata.getSuppliableKey(user, EDGE_ID, APP_ID).join());

		verify(this.oem).getAppCenterMasterKey();
	}

	private static Stream<Arguments> usersAllowedToReceiveMasterKey() {
		return Stream.of(//
				Arguments.of(true, Role.OWNER, DummyUser.DUMMY_OWNER), //
				Arguments.of(false, Role.INSTALLER, DummyUser.DUMMY_INSTALLER));
	}

	@Test
	void ownerCannotReceiveMasterKeyForNonFreeApp() {
		this.metadata.isFreeApp = false;
		this.metadata.edgeRole = Role.OWNER;

		var exception = assertThrows(CompletionException.class,
				() -> this.metadata.getSuppliableKey(DummyUser.DUMMY_OWNER, EDGE_ID, APP_ID).join());

		assertInstanceOf(OpenemsNamedException.class, exception.getCause());
		verify(this.oem, never()).getAppCenterMasterKey();
	}

	private static final class TestMetadataOdoo extends MetadataOdoo {

		private Role edgeRole;
		private boolean isFreeApp;

		@Override
		public Role getUserRole(User user, String edgeId) {
			return this.edgeRole;
		}

		@Override
		public CompletableFuture<Boolean> isAppFree(User user, String appId) {
			return CompletableFuture.completedFuture(this.isFreeApp);
		}
	}
}
