package io.openems.backend.common.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.jsonrpc.request.GetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.request.SetUserAlertingConfigsRequest;
import io.openems.backend.common.jsonrpc.response.GetUserAlertingConfigsResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class UserAlertingSettingsJsonRpc {

	@Test
	public void testGetUserAlertingConfigsRequest() {
		assertEquals("getUserAlertingConfigs", GetUserAlertingConfigsRequest.METHOD);

		var id = UUID.randomUUID();
		var edgeId = "edge4";
		var params = JsonUtils.buildJsonObject().addProperty("edgeId", edgeId).build();

		var json = new JsonrpcRequest(id, GetUserAlertingConfigsRequest.METHOD, 0) {
			@Override
			public JsonObject getParams() {
				return params;
			}
		};
		try {
			var request = GetUserAlertingConfigsRequest.from(json);

			assertEquals(id, request.id);
			assertEquals(edgeId, request.getEdgeId());
			assertEquals(params, request.getParams());
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSetUserAlertingConfigsRequest() {
		assertEquals("setUserAlertingConfigs", SetUserAlertingConfigsRequest.METHOD);

		var userSettingJson = JsonUtils.buildJsonObject() //
				.addProperty("userLogin", "user1") //
				.addProperty("offlineEdgeDelay", 15) //
				.addProperty("faultEdgeDelay", 30) //
				.addProperty("warningEdgeDelay", 60) //
				.build();
		var illegalSettingJson = JsonUtils.buildJsonObject() //
				.addProperty("err", "wrong") //
				.build();

		var id = UUID.randomUUID();
		var edgeId = "edge4";
		var params = JsonUtils.buildJsonObject() //
				.addProperty("edgeId", edgeId) //
				.add("userSettings", JsonUtils.buildJsonArray().add(userSettingJson) //
						.add(illegalSettingJson) //
						.build()) //
				.build();

		var userSetting = new UserAlertingSettings("user1", 15, 30, 60);

		var json = new JsonrpcRequest(id, SetUserAlertingConfigsRequest.METHOD, 0) {
			@Override
			public JsonObject getParams() {
				return params;
			}
		};
		try {
			var request = SetUserAlertingConfigsRequest.from(json);

			assertEquals(id, request.id);
			assertEquals(edgeId, request.getEdgeId());

			var expected = JsonUtils.buildJsonObject() //
					.addProperty("edgeId", edgeId) //
					.add("userSettings", JsonUtils.buildJsonArray().add(userSettingJson).build()) //
					.build();
			assertEquals(expected, request.getParams());

			var settings = request.getUserSettings();
			assertEquals(1, settings.size());
			assertEquals(userSetting, settings.get(0));
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetUserAlertingConfigsResponse() {
		var sett1 = new UserAlertingSettings("edge1", "user1", 0, 15, 30, null, null);
		var sett2 = new UserAlertingSettings("edge2", "user2", 10, 10, 10, null, null);

		var id = UUID.randomUUID();

		var response = new GetUserAlertingConfigsResponse(id, sett1, List.of(sett2));

		var sett1Json = JsonUtils.buildJsonObject() //
				.addProperty("userLogin", sett1.userLogin()) //
				.addProperty("offlineEdgeDelay", sett1.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", sett1.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", sett1.edgeWarningDelay()) //
				.build();
		var sett2Json = JsonUtils.buildJsonObject() //
				.addProperty("userLogin", sett2.userLogin()) //
				.addProperty("offlineEdgeDelay", sett2.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", sett2.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", sett2.edgeWarningDelay()) //
				.build();
		var settArrJson = JsonUtils.buildJsonObject() //
				.add("currentUserSettings", sett1Json)
				.add("otherUsersSettings", JsonUtils.buildJsonArray().add(sett2Json).build()) //
				.build();

		var jsonObj = response.getResult();

		assertEquals(settArrJson, jsonObj);
	}
}
