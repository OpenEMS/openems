package io.openems.backend.common.jsonrpc.response;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.GetLatestSetupProtocolCoreInfoRequest;
import io.openems.backend.common.metadata.Metadata.SetupProtocolCoreInfo;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Represents a JSON-RPC Response for {@link GetLatestSetupProtocolCoreInfoRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "setupProtocolId": int,
 *     "createDate": {@link ZonedDateTime}
 *   }
 * }
 * </pre>
 */
public class GetLatestSetupProtocolCoreInfoResponse extends JsonrpcResponseSuccess {

	private final SetupProtocolCoreInfo setupProtocolData;

	public GetLatestSetupProtocolCoreInfoResponse(UUID id, SetupProtocolCoreInfo setupProtocolData) {
		super(id);
		this.setupProtocolData = setupProtocolData;
	}

	public SetupProtocolCoreInfo getSetupProtocolData() {
		return this.setupProtocolData;
	}

	@Override
	public JsonObject getResult() {
		return buildJsonObject() //
				.onlyIf(this.setupProtocolData != null, t -> {
					t.addProperty("setupProtocolId", this.setupProtocolData.setupProtocolId()) //
							.addProperty("createDate", this.setupProtocolData.createDate()) //
							.addProperty("setupProtocolType", this.setupProtocolData.type()); //
				}) //
				.build();
	}
}