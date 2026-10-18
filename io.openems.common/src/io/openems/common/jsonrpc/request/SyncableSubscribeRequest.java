package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.utils.JsonUtils;

/**
 * JSON-RPC Request for subscribing to a specific syncable key.
 *
 * <p>
 * Can be sent from EDGE -> BACKEND_EDGE or EDGE -> BACKEND.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "syncable/subscribe",
 *   "params": {
 *     "key": {
 *        "type": "CurrentTimeInUnixTimestamp"
 *     }
 *   }
 * }
 * </pre>
 */
public class SyncableSubscribeRequest extends JsonrpcRequest {

	public static final String METHOD = "syncable/subscribe";

	/**
	 * Creates a {@link SyncableSubscribeRequest} from a generic
	 * {@link JsonrpcRequest} by deserializing the payload.
	 *
	 * @param request the incoming JsonrpcRequest
	 * @return the parsed SyncableSubscribeRequest
	 * @throws OpenemsError.OpenemsNamedException if required params are missing or
	 *                                            the entry cannot be deserialized
	 */
	public static SyncableSubscribeRequest from(JsonrpcRequest request) throws OpenemsError.OpenemsNamedException {
		var p = request.getParams();
		var key = SyncableKey.keySerializer().deserialize(JsonUtils.getAsJsonObject(p, "key"));
		return new SyncableSubscribeRequest(key);
	}

	private final SyncableKey<?> key;

	public SyncableSubscribeRequest(SyncableKey<?> key) {
		super(METHOD);
		this.key = key;
	}

	public SyncableKey<?> getKey() {
		return this.key;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject().add("key", this.key, SyncableKey.keySerializer()).build();
	}
}
