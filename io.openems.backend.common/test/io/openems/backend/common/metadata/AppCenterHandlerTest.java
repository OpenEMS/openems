package io.openems.backend.common.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.common.test.DummyUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AddAppInstanceRequest;
import io.openems.common.jsonrpc.request.AppCenterAddRegisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterAddUnregisterKeyHistoryRequest;
import io.openems.common.jsonrpc.request.AppCenterGetPossibleAppsRequest;
import io.openems.common.jsonrpc.request.AppCenterGetRegisteredKeysRequest;
import io.openems.common.jsonrpc.request.AppCenterInstallAppWithSuppliedKeyRequest;
import io.openems.common.jsonrpc.request.AppCenterIsKeyApplicableRequest;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

class AppCenterHandlerTest {

	private static final String EDGE_ID = "edge0";
	private static final String APP_ID = "app0";
	private static final String MASTER_KEY = "master-key";

	@ParameterizedTest
	@MethodSource("ownerOnlyUserRequests")
	void guestIsRejectedBeforeMetadataOrInstallDelegation(AppCenterRequest request) {
		var metadata = mock(AppCenterMetadata.UiData.class);
		var delegatedRequestCalled = new AtomicReference<>(false);

		assertThrows(OpenemsNamedException.class, () -> AppCenterHandler.handleUserRequest(//
				metadata, //
				r -> {
					delegatedRequestCalled.set(true);
					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(r.getId()));
				}, //
				request, //
				DummyUser.DUMMY_GUEST, //
				Role.GUEST, //
				EDGE_ID));

		verifyNoInteractions(metadata);
		assertFalse(delegatedRequestCalled.get());
	}

	@ParameterizedTest
	@MethodSource("ownerOnlyUserRequests")
	void ownerCanUseOwnerOnlyUserRequests(AppCenterRequest request) throws Exception {
		var metadata = mock(AppCenterMetadata.UiData.class);
		when(metadata.sendAddRegisterKeyHistory(EDGE_ID, APP_ID, "key", DummyUser.DUMMY_OWNER))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(metadata.sendAddUnregisterKeyHistory(EDGE_ID, APP_ID, "key", DummyUser.DUMMY_OWNER))
				.thenReturn(CompletableFuture.completedFuture(null));
		when(metadata.sendGetRegisteredKeys(EDGE_ID, APP_ID))
				.thenReturn(CompletableFuture.completedFuture(new JsonArray()));
		when(metadata.sendIsKeyApplicable("key", EDGE_ID, APP_ID))
				.thenReturn(CompletableFuture.completedFuture(keyApplicabilityResult()));
		when(metadata.getSuppliableKey(DummyUser.DUMMY_OWNER, EDGE_ID, APP_ID))
				.thenReturn(CompletableFuture.completedFuture(MASTER_KEY));

		var delegatedRequest = new AtomicReference<EdgeRpcRequest>();
		AppCenterHandler.handleUserRequest(//
				metadata, //
				r -> {
					delegatedRequest.set(r);
					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(r.getId()));
				}, //
				request, //
				DummyUser.DUMMY_OWNER, //
				Role.OWNER, //
				EDGE_ID).join();

		switch (request.getPayload().getMethod()) {
		case AppCenterAddRegisterKeyHistoryRequest.METHOD ->
			verify(metadata).sendAddRegisterKeyHistory(EDGE_ID, APP_ID, "key", DummyUser.DUMMY_OWNER);
		case AppCenterAddUnregisterKeyHistoryRequest.METHOD ->
			verify(metadata).sendAddUnregisterKeyHistory(EDGE_ID, APP_ID, "key", DummyUser.DUMMY_OWNER);
		case AppCenterGetRegisteredKeysRequest.METHOD -> verify(metadata).sendGetRegisteredKeys(EDGE_ID, APP_ID);
		case AppCenterIsKeyApplicableRequest.METHOD -> verify(metadata).sendIsKeyApplicable("key", EDGE_ID, APP_ID);
		case AppCenterInstallAppWithSuppliedKeyRequest.METHOD -> {
			verify(metadata).getSuppliableKey(DummyUser.DUMMY_OWNER, EDGE_ID, APP_ID);
			assertEquals(MASTER_KEY, getInstallRequest(delegatedRequest.get()).key);
		}
		default -> throw new AssertionError(request.getPayload().getMethod());
		}
	}

	@Test
	void installerCanUseUserSuppliedKeyWithoutAutomaticKeyLookup() throws Exception {
		var suppliedKeyRequest = installRequest("supplied-key");
		var metadata = mock(AppCenterMetadata.UiData.class);
		var delegatedRequest = new AtomicReference<EdgeRpcRequest>();

		AppCenterHandler.handleUserRequest(//
				metadata, //
				r -> {
					delegatedRequest.set(r);
					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(r.getId()));
				}, //
				suppliedKeyRequest, //
				DummyUser.DUMMY_INSTALLER, //
				Role.INSTALLER, //
				EDGE_ID).join();

		assertEquals("supplied-key", getInstallRequest(delegatedRequest.get()).key);
		verifyNoInteractions(metadata);
	}

	@Test
	void installerCanRequestAutomaticallySuppliedKey() throws Exception {
		var metadata = mock(AppCenterMetadata.UiData.class);
		when(metadata.getSuppliableKey(DummyUser.DUMMY_INSTALLER, EDGE_ID, APP_ID))
				.thenReturn(CompletableFuture.completedFuture(MASTER_KEY));
		var delegatedRequest = new AtomicReference<EdgeRpcRequest>();

		AppCenterHandler.handleUserRequest(//
				metadata, //
				r -> {
					delegatedRequest.set(r);
					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(r.getId()));
				}, //
				installRequest(null), //
				DummyUser.DUMMY_INSTALLER, //
				Role.INSTALLER, //
				EDGE_ID).join();

		verify(metadata).getSuppliableKey(DummyUser.DUMMY_INSTALLER, EDGE_ID, APP_ID);
		assertEquals(MASTER_KEY, getInstallRequest(delegatedRequest.get()).key);
	}

	@Test
	void guestCannotForwardUserSuppliedKey() {
		var metadata = mock(AppCenterMetadata.UiData.class);
		var delegatedRequestCalled = new AtomicReference<>(false);

		assertThrows(OpenemsNamedException.class, () -> AppCenterHandler.handleUserRequest(//
				metadata, //
				r -> {
					delegatedRequestCalled.set(true);
					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(r.getId()));
				}, //
				installRequest("supplied-key"), //
				DummyUser.DUMMY_GUEST, //
				Role.GUEST, //
				EDGE_ID));

		verifyNoInteractions(metadata);
		assertFalse(delegatedRequestCalled.get());
	}

	@Test
	void guestCanUseUserGetPossibleAppsForAppDetails() throws Exception {
		var metadata = mock(AppCenterMetadata.UiData.class);
		when(metadata.sendGetPossibleApps("key", EDGE_ID))
				.thenReturn(CompletableFuture.completedFuture(new JsonArray()));

		AppCenterHandler.handleUserRequest(//
				metadata, //
				_ -> {
					throw new AssertionError("User getPossibleApps must not be delegated to the Edge");
				}, //
				new AppCenterRequest(new AppCenterGetPossibleAppsRequest("key")), //
				DummyUser.DUMMY_GUEST, //
				Role.GUEST, //
				EDGE_ID).join();

		verify(metadata).sendGetPossibleApps("key", EDGE_ID);
	}

	@Test
	void internalEdgeCanStillUseGetPossibleApps() throws Exception {
		var metadata = mock(AppCenterMetadata.EdgeData.class);
		when(metadata.sendGetPossibleApps("key", EDGE_ID))
				.thenReturn(CompletableFuture.completedFuture(new JsonArray()));

		AppCenterHandler.handleEdgeRequest(//
				metadata, //
				new AppCenterRequest(new AppCenterGetPossibleAppsRequest("key")), //
				EDGE_ID).join();

		verify(metadata).sendGetPossibleApps("key", EDGE_ID);
	}

	private static Stream<Arguments> ownerOnlyUserRequests() {
		return Stream.of(//
				Arguments.of(new AppCenterRequest(
						new GenericJsonrpcRequest(AppCenterAddRegisterKeyHistoryRequest.METHOD, keyAndAppIdParams()))), //
				Arguments.of(new AppCenterRequest(new GenericJsonrpcRequest(
						AppCenterAddUnregisterKeyHistoryRequest.METHOD, keyAndAppIdParams()))), //
				Arguments.of(new AppCenterRequest(new GenericJsonrpcRequest(AppCenterGetRegisteredKeysRequest.METHOD, //
						JsonUtils.buildJsonObject().addProperty("appId", APP_ID).build()))), //
				Arguments.of(new AppCenterRequest(new AppCenterIsKeyApplicableRequest("key", APP_ID))), //
				Arguments.of(installRequest(null)));
	}

	private static JsonObject keyAndAppIdParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", "key") //
				.addProperty("appId", APP_ID) //
				.build();
	}

	private static AppCenterRequest installRequest(String key) {
		var addAppInstance = new AddAppInstanceRequest(APP_ID, key, "alias", new JsonObject());
		var componentRequest = new ComponentJsonApiRequest("_appManager", addAppInstance);
		var params = JsonUtils.buildJsonObject() //
				.add("installRequest", componentRequest.toJsonObject()) //
				.build();
		return new AppCenterRequest(
				new GenericJsonrpcRequest(AppCenterInstallAppWithSuppliedKeyRequest.METHOD, params));
	}

	private static AddAppInstanceRequest getInstallRequest(EdgeRpcRequest edgeRpcRequest) throws Exception {
		var componentRequest = ComponentJsonApiRequest.from(edgeRpcRequest.getPayload());
		return AddAppInstanceRequest.from(componentRequest.getPayload());
	}

	private static JsonObject keyApplicabilityResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("isKeyApplicable", true) //
				.add("additionalInfo", JsonUtils.buildJsonObject() //
						.addProperty("keyId", "key") //
						.add("bundles", new JsonArray()) //
						.add("registrations", new JsonArray()) //
						.add("usages", new JsonArray()) //
						.build()) //
				.build();
	}
}
