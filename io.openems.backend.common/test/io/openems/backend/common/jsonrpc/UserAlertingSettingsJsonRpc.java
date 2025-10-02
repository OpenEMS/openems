package io.openems.backend.common.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.jsonrpc.request.GetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.request.SetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.response.GetUserAlertingConfigsResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

public class UserAlertingSettingsJsonRpc {

	@Test
	public void testGetUserAlertingConfigsRequest() throws OpenemsNamedException {
		assertEquals("getUserAlertingConfigs", GetUserAlertingConfigsRequest.METHOD);

		final var id = randomUUID();
		final var edgeId = "edge4";
		final var params = buildJsonObject() //
				.addProperty("edgeId", edgeId).build();

		final var json = new JsonrpcRequest(id, GetUserAlertingConfigsRequest.METHOD, 0) {
			@Override
			public JsonObject getParams() {
				return params;
			}
		};
		final var request = GetUserAlertingConfigsRequest.from(json);

		assertEquals(id, request.id);
		assertEquals(edgeId, request.getEdgeId());
		assertEquals(params, request.getParams());
	}

	@Test
	public void testSetUserAlertingConfigsRequest() throws OpenemsNamedException {
		assertEquals("setUserAlertingConfigs", SetUserAlertingConfigsRequest.METHOD);

		final var user1Setting = new UserAlertingSettings("user1", 15, 30, 60);
		final var user2Setting = new UserAlertingSettings("user2", 0, 0, 0);

		final var user1SettingJson = buildJsonObject() //
				.addProperty("userLogin", user1Setting.userLogin()) //
				.addProperty("offlineEdgeDelay", user1Setting.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", user1Setting.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", user1Setting.edgeWarningDelay()) //
				.build();
		final var user2SettingJson = buildJsonObject() //
				.addProperty("userLogin", user2Setting.userLogin()) //
				.addProperty("offlineEdgeDelay", user2Setting.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", user2Setting.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", user2Setting.edgeWarningDelay()) //
				.build();

		final var id = randomUUID();
		final var edgeId = "edge4";
		final var params = buildJsonObject() //
				.addProperty("edgeId", edgeId) //
				.add("userSettings", buildJsonArray() //
						.add(user1SettingJson) //
						.add(user2SettingJson) //
						.build()) //
				.build();

		final var request = SetUserAlertingConfigsRequest.from(//
				new GenericJsonrpcRequest(id, SetUserAlertingConfigsRequest.METHOD, params, Optional.of(0))//
		);

		assertEquals(id, request.id);
		assertEquals(edgeId, request.getEdgeId());

		final var expected = buildJsonObject() //
				.addProperty("edgeId", edgeId) //
				.add("userSettings", buildJsonArray() //
						.add(user1SettingJson) //
						.add(user2SettingJson).build()) //
				.build();
		assertEquals(expected, request.getParams());

		final var settings = request.getUserSettings();
		assertEquals(2, settings.size());
		assertEquals(user1Setting, settings.get(0));
		assertEquals(user2Setting, settings.get(1));
	}

	@Test
	public void testInvalidSetUserAlertingConfigsRequest() throws OpenemsNamedException {
		assertEquals("setUserAlertingConfigs", SetUserAlertingConfigsRequest.METHOD);

		final var illegalSettingJson = buildJsonObject() //
				.addProperty("err", "wrong") //
				.build();

		final var id = randomUUID();
		final var edgeId = "edge4";
		final var params = buildJsonObject() //
				.addProperty("edgeId", edgeId) //
				.add("userSettings", buildJsonArray() //
						.add(illegalSettingJson) //
						.build()) //
				.build();

		final var json = new JsonrpcRequest(id, SetUserAlertingConfigsRequest.METHOD, 0) {
			@Override
			public JsonObject getParams() {
				return params;
			}
		};

		assertThrows(OpenemsNamedException.class, () -> { //
			SetUserAlertingConfigsRequest.from(json); //
		});
	}

	@Test
	public void testGetUserAlertingConfigsResponse() {
		final var sett1 = new UserAlertingSettings("edge1", "user1", 0, 15, 30, null, null);
		final var sett2 = new UserAlertingSettings("edge2", "user2", 10, 10, 10, null, null);

		final var id = randomUUID();

		final var response = new GetUserAlertingConfigsResponse(id, sett1, List.of(sett2));

		final var sett1Json = buildJsonObject() //
				.addProperty("userLogin", sett1.userLogin()) //
				.addProperty("offlineEdgeDelay", sett1.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", sett1.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", sett1.edgeWarningDelay()) //
				.build();
		final var sett2Json = buildJsonObject() //
				.addProperty("userLogin", sett2.userLogin()) //
				.addProperty("offlineEdgeDelay", sett2.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", sett2.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", sett2.edgeWarningDelay()) //
				.build();
		final var settArrJson = buildJsonObject() //
				.add("currentUserSettings", sett1Json)
				.add("otherUsersSettings", buildJsonArray().add(sett2Json).build()) //
				.build();

		final var jsonObj = response.getResult();

		assertEquals(settArrJson, jsonObj);
	}
}
