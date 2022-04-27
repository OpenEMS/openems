package io.openems.backend.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getAlertingConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     timeToWait: int
 *   }
 * }
 * </pre>
 */
public class GetAlertingConfigResponse extends JsonrpcResponseSuccess {

	private final int timeToWait;

	public GetAlertingConfigResponse(int timeToWait) {
		this(UUID.randomUUID(), timeToWait);
	}

	public GetAlertingConfigResponse(UUID id, int timeToWait) {
		super(id);
		this.timeToWait = timeToWait;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("timeToWait", this.timeToWait) //
				.build();
	}

}
