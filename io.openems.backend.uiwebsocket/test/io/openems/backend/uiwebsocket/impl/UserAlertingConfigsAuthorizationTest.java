package io.openems.backend.uiwebsocket.impl;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.backend.authentication.api.AuthUserAuthorizationCodeFlowService;
import io.openems.backend.authentication.api.model.OAuthToken;
import io.openems.backend.authentication.api.model.response.InitiateConnectResponse;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.jsonrpc.request.GetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.request.SetUserAlertingConfigsRequest;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

class UserAlertingConfigsAuthorizationTest {

	private static final String USER_ID = "user";
	private static final String OTHER_USER_ID = "other";
	private static final String EDGE_A = "edgeA";
	private static final String EDGE_B = "edgeB";

	@Test
	void getAllowsGuestToReadOwnSettingsAndHidesOtherUsers() throws Exception {
		var metadata = new AlertingMetadata();
		metadata.setRole(EDGE_B, Role.GUEST);

		var response = apply(metadata, getRequest(EDGE_B)).join().getResult();

		assertEquals(USER_ID, response.getAsJsonObject("currentUserSettings").get("userLogin").getAsString());
		assertEquals(0, response.getAsJsonArray("otherUsersSettings").size());
		assertEquals(1, metadata.userSettingsReads);
		assertEquals(0, metadata.allSettingsReads);
	}

	@Test
	void getRejectsUserWithoutRoleOnTargetEdgeBeforeReadingSettings() {
		var metadata = new AlertingMetadata();
		metadata.setRole(EDGE_A, Role.OWNER);

		assertThrows(CompletionException.class, () -> apply(metadata, getRequest(EDGE_B)).join());

		assertEquals(0, metadata.userSettingsReads);
		assertEquals(0, metadata.allSettingsReads);
	}

	@Test
	void getAllowsAdminToReadOtherUsersSettings() throws Exception {
		var metadata = new AlertingMetadata();
		metadata.setRole(EDGE_B, Role.ADMIN);

		var response = apply(metadata, getRequest(EDGE_B)).join().getResult();

		assertEquals(1, response.getAsJsonArray("otherUsersSettings").size());
		assertEquals(1, metadata.allSettingsReads);
		assertEquals(0, metadata.userSettingsReads);
	}

	@Test
	void setAllowsGuestToUpdateOwnSettings() throws Exception {
		var metadata = new AlertingMetadata();
		metadata.setRole(EDGE_B, Role.GUEST);

		apply(metadata, setRequest(EDGE_B, USER_ID)).join();

		assertEquals(1, metadata.settingsWrites);
	}

	@Test
	void setRejectsUserWithoutRoleOnTargetEdgeBeforeWritingSettings() {
		var metadata = new AlertingMetadata();
		metadata.setRole(EDGE_A, Role.OWNER);

		assertThrows(CompletionException.class, () -> apply(metadata, setRequest(EDGE_B, USER_ID)).join());

		assertEquals(0, metadata.settingsWrites);
	}

	@Test
	void setKeepsAdminBoundaryForOtherUsersSettings() throws Exception {
		var guestMetadata = new AlertingMetadata();
		guestMetadata.setRole(EDGE_B, Role.GUEST);

		assertThrows(CompletionException.class, () -> apply(guestMetadata, setRequest(EDGE_B, OTHER_USER_ID)).join());
		assertEquals(0, guestMetadata.settingsWrites);

		var adminMetadata = new AlertingMetadata();
		adminMetadata.setRole(EDGE_B, Role.ADMIN);

		apply(adminMetadata, setRequest(EDGE_B, OTHER_USER_ID)).join();

		assertEquals(1, adminMetadata.settingsWrites);
	}

	private static CompletableFuture<? extends JsonrpcResponseSuccess> apply(AlertingMetadata metadata,
			GenericJsonrpcRequest request) throws Exception {
		var parent = new UiWebsocketImpl();
		parent.metadata = metadata;
		parent.userAuthenticationService = new DummyAuthUserAuthorizationCodeFlowService();

		var wsData = new WsData(null, 10);
		wsData.setToken("token");
		wsData.setUser(new User(USER_ID, USER_ID, "token", Language.EN, Role.GUEST, false, new JsonObject()));

		var response = new OnRequest(parent).apply(wsData, request);
		return response.thenApply(JsonrpcResponseSuccess.class::cast);
	}

	private static GenericJsonrpcRequest getRequest(String edgeId) {
		return new GenericJsonrpcRequest(randomUUID(), GetUserAlertingConfigsRequest.METHOD,
				buildJsonObject().addProperty("edgeId", edgeId).build(), Optional.of(0));
	}

	private static GenericJsonrpcRequest setRequest(String edgeId, String userId) {
		var userSetting = buildJsonObject() //
				.addProperty("userLogin", userId) //
				.addProperty("offlineEdgeDelay", 15) //
				.addProperty("faultEdgeDelay", 30) //
				.addProperty("warningEdgeDelay", 60) //
				.build();
		var params = buildJsonObject() //
				.addProperty("edgeId", edgeId) //
				.add("userSettings", buildJsonArray().add(userSetting).build()) //
				.build();
		return new GenericJsonrpcRequest(randomUUID(), SetUserAlertingConfigsRequest.METHOD, params, Optional.of(0));
	}

	private static class DummyAuthUserAuthorizationCodeFlowService implements AuthUserAuthorizationCodeFlowService {

		@Override
		public CompletableFuture<InitiateConnectResponse> initiateConnect(String oem, String redirectUri) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<OAuthToken> tokenByCode(String oem, String identifier, String code) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<OAuthToken> tokenByRefreshToken(String oem, String refreshToken) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<Void> validate(String accessToken) {
			throw new UnsupportedOperationException();
		}
	}

	private static class AlertingMetadata extends DummyMetadata {
		private final Map<String, Role> roles = new HashMap<>();
		private int userSettingsReads;
		private int allSettingsReads;
		private int settingsWrites;

		@Override
		public Role getUserRole(User user, String edgeId) {
			return this.roles.get(edgeId);
		}

		@Override
		public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) {
			this.userSettingsReads++;
			return new UserAlertingSettings(userId, 15, 30, 60);
		}

		@Override
		public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
			this.allSettingsReads++;
			return new ArrayList<>(List.of(//
					new UserAlertingSettings(USER_ID, 15, 30, 60), //
					new UserAlertingSettings(OTHER_USER_ID, 0, 0, 0)));
		}

		@Override
		public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> settings) {
			this.settingsWrites++;
		}

		private void setRole(String edgeId, Role role) {
			this.roles.put(edgeId, role);
		}
	}
}
