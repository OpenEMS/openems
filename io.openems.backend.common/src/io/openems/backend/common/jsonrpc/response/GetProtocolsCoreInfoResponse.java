package io.openems.backend.common.jsonrpc.response;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.GetProtocolsCoreInfoRequest;
import io.openems.backend.common.metadata.Metadata.ProtocolType;
import io.openems.backend.common.metadata.Metadata.SetupProtocolCoreInfo;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link GetProtocolsCoreInfoRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "setupProtocols": {
 *       "setupProtocolId": int,
 *       "createDate": {@link ZonedDateTime},
 *       "setupProtocolType": {@link ProtocolType}
 *     }[]
 *   }
 * }
 * </pre>
 */
public class GetProtocolsCoreInfoResponse extends JsonrpcResponseSuccess {

	private final List<SetupProtocolCoreInfo> setupProtocolCoreInfos;

	public GetProtocolsCoreInfoResponse(UUID id, List<SetupProtocolCoreInfo> setupProtocolCoreInfos) {
		super(id);
		this.setupProtocolCoreInfos = setupProtocolCoreInfos;
	}

	@Override
	public JsonObject getResult() {
		return buildJsonObject() //
				.add("setupProtocols", this.setupProtocolCoreInfos.stream() //
						.map(el -> JsonUtils.buildJsonObject() //
								.addProperty("setupProtocolId", el.setupProtocolId()) //
								.addProperty("createDate", el.createDate()) //
								.addProperty("setupProtocolType", el.type()) //
								.build()) //
						.collect(JsonUtils.toJsonArray()))
				.build();
	}
}