package io.openems.backend.uiwebsocket.impl;

import static io.openems.backend.common.test.DummyUser.DUMMY_INSTALLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;

import io.openems.backend.authentication.api.AuthUserAuthorizationCodeFlowService;
import io.openems.backend.authentication.api.AuthUserRegistrationService;
import io.openems.backend.common.jsonrpc.request.SubmitSetupProtocolRequest;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

class OnRequestTest {

	@Test
	void submitSetupProtocolAllowsInstallerOnTargetEdge() throws Exception {
		var fixture = new SubmitFixture();
		var request = createSubmitRequest("edgeA");

		var response = fixture.sut.apply(fixture.websocket, request).join();

		assertEquals(42, JsonUtils.getAsInt(response.getResult(), "setupProtocolId"));
		var order = inOrder(fixture.metadata, fixture.registration);
		order.verify(fixture.metadata).assertCanSubmitSetupProtocol(DUMMY_INSTALLER,
				JsonUtils.getAsJsonObject(request.getParams(), "protocol"));
		order.verify(fixture.registration).registerUserIfNotExist(any());
		order.verify(fixture.metadata).submitSetupProtocol(DUMMY_INSTALLER,
				JsonUtils.getAsJsonObject(request.getParams(), "protocol"));
	}

	@Test
	void submitSetupProtocolRejectsUnsupportedMetadataWithoutSideEffects() throws Exception {
		var fixture = new SubmitFixture();
		doCallRealMethod().when(fixture.metadata).assertCanSubmitSetupProtocol(any(), any());

		var exception = assertThrows(CompletionException.class,
				() -> fixture.sut.apply(fixture.websocket, createSubmitRequest("edgeA")).join());

		assertInstanceOf(OpenemsNamedException.class, exception.getCause());
		verifyNoInteractions(fixture.registration);
		verify(fixture.metadata, never()).submitSetupProtocol(any(), any());
	}

	@Test
	void submitSetupProtocolRejectsInstallerOnOtherEdgeWithoutSideEffects() throws Exception {
		var fixture = new SubmitFixture();
		var request = createSubmitRequest("edgeB");
		JsonUtils.getAsJsonObject(request.getParams(), "protocol") //
				.add("other", JsonUtils.buildJsonObject() //
						.addProperty("id", "edgeA") //
						.build());

		var exception = assertThrows(CompletionException.class,
				() -> fixture.sut.apply(fixture.websocket, request).join());

		assertEquals(OpenemsError.COMMON_ROLE_UNDEFINED,
				assertInstanceOf(OpenemsNamedException.class, exception.getCause()).getError());
		verifyNoInteractions(fixture.registration);
		verify(fixture.metadata, never()).submitSetupProtocol(any(), any());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OWNER", "GUEST" })
	void submitSetupProtocolRejectsInsufficientEdgeRoleWithoutSideEffects(Role role) throws Exception {
		var fixture = new SubmitFixture();
		when(fixture.metadata.getUserRole(DUMMY_INSTALLER, "edgeB")).thenReturn(role);
		var request = createSubmitRequest("edgeB");

		var exception = assertThrows(CompletionException.class,
				() -> fixture.sut.apply(fixture.websocket, request).join());

		assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED,
				assertInstanceOf(OpenemsNamedException.class, exception.getCause()).getError());
		verifyNoInteractions(fixture.registration);
		verify(fixture.metadata, never()).submitSetupProtocol(any(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"edge\": null}", "{\"edge\": {}}" })
	void submitSetupProtocolRejectsMissingTargetEdgeWithoutSideEffects(String protocol) throws Exception {
		var fixture = new SubmitFixture();
		var request = new GenericJsonrpcRequest(SubmitSetupProtocolRequest.METHOD, JsonUtils.buildJsonObject() //
				.add("protocol", JsonUtils.parseToJsonObject(protocol)) //
				.build());

		var exception = assertThrows(CompletionException.class,
				() -> fixture.sut.apply(fixture.websocket, request).join());

		assertInstanceOf(OpenemsNamedException.class, exception.getCause());
		verifyNoInteractions(fixture.registration);
		verify(fixture.metadata, never()).submitSetupProtocol(any(), any());
	}

	private static class DummyUiWebsocketImpl extends UiWebsocketImpl {
		private final Set<String> removed = new HashSet<>();
		private final Set<String> added = new HashSet<>();

		@Override
		public void removeEdgeSubscriptions(WsData wsData) {
			this.removed.addAll(wsData.getSubscribedEdges());
		}

		@Override
		public void removeEdgeSubscriptions(WsData wsData, Set<String> edgeIds) {
			this.removed.addAll(edgeIds);
		}

		@Override
		public void addEdgeSubscriptions(WsData wsData, Set<String> edgeIds) {
			this.added.addAll(edgeIds);
		}
	}

	@Test
	void updateEdgeSubscriptionsTest_Update() {
		var parent = new DummyUiWebsocketImpl();
		var sut = new OnRequest(parent);
		var wsData = new WsData(null, 10);
		wsData.addSubscribedEdges(Set.of("edge0", "edge1", "edge2"));

		sut.updateEdgeSubscriptions(wsData, Set.of("edge1", "edge3"));

		assertEquals(Set.of("edge0", "edge2"), parent.removed);
		assertEquals(Set.of("edge3"), parent.added);

		assertEquals(Set.of("edge1", "edge3"), wsData.getSubscribedEdges());
	}

	@Test
	void updateEdgeSubscriptionsTest_Add() {
		var parent = new DummyUiWebsocketImpl();
		var sut = new OnRequest(parent);
		var wsData = new WsData(null, 10);

		sut.updateEdgeSubscriptions(wsData, Set.of("edge1", "edge3"));

		assertEquals(Set.of(), parent.removed);
		assertEquals(Set.of("edge1", "edge3"), parent.added);

		assertEquals(Set.of("edge1", "edge3"), wsData.getSubscribedEdges());
	}

	@Test
	void updateEdgeSubscriptionsTest_Clear() {
		var parent = new DummyUiWebsocketImpl();
		var sut = new OnRequest(parent);
		var wsData = new WsData(null, 10);
		wsData.addSubscribedEdges(Set.of("edge0", "edge1", "edge2"));

		sut.updateEdgeSubscriptions(wsData, Set.of());

		assertEquals(Set.of("edge0", "edge1", "edge2"), parent.removed);
		assertEquals(Set.of(), parent.added);

		assertEquals(Set.of(), wsData.getSubscribedEdges());
	}

	private static class SubmitFixture {
		private final Metadata metadata = mock(Metadata.class, CALLS_REAL_METHODS);
		private final AuthUserRegistrationService registration = mock(AuthUserRegistrationService.class);
		private final WebSocket websocket = mock(WebSocket.class);
		private final OnRequest sut;

		private SubmitFixture() throws OpenemsNamedException {
			var parent = new UiWebsocketImpl();
			parent.metadata = this.metadata;
			parent.userRegistrationService = this.registration;
			parent.userAuthenticationService = mock(AuthUserAuthorizationCodeFlowService.class);
			this.sut = new OnRequest(parent);

			var wsData = new WsData(this.websocket, 10);
			wsData.setUser(DUMMY_INSTALLER);
			wsData.setToken("test-token");
			when(this.websocket.getAttachment()).thenReturn(wsData);
			when(this.metadata.getUserRole(DUMMY_INSTALLER, "edgeA")).thenReturn(Role.INSTALLER);
			doAnswer(invocation -> {
				var protocol = invocation.<JsonObject>getArgument(1);
				var edgeId = JsonUtils.getAsString(JsonUtils.getAsJsonObject(protocol, "edge"), "id");
				this.metadata.assertRoleIsAtLeast(invocation.getArgument(0), edgeId, Role.INSTALLER,
						SubmitSetupProtocolRequest.METHOD);
				return null;
			}).when(this.metadata).assertCanSubmitSetupProtocol(any(), any());
			when(this.registration.registerUserIfNotExist(any())).thenReturn(CompletableFuture.completedFuture(null));
			when(this.metadata.submitSetupProtocol(any(), any())).thenReturn(42);
		}
	}

	private static GenericJsonrpcRequest createSubmitRequest(String edgeId) throws OpenemsNamedException {
		var protocol = JsonUtils.parseToJsonObject("""
				{
				  "customer": {
				    "firstname": "Test",
				    "lastname": "Customer",
				    "phone": "123456",
				    "email": "customer@example.com",
				    "address": {
				      "street": "Test Street",
				      "zip": "12345",
				      "city": "Test City",
				      "country": "DE"
				    }
				  }
				}
				""");
		protocol.add("edge", JsonUtils.buildJsonObject() //
				.addProperty("id", edgeId) //
				.build());
		return new GenericJsonrpcRequest(SubmitSetupProtocolRequest.METHOD, JsonUtils.buildJsonObject() //
				.add("protocol", protocol) //
				.build());
	}
}