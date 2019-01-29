package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to system log. The actual system
 * log is then sent as JSON-RPC Notification
 * 
 * <p>
 * Set 'subscribe' param to 'true' to start the subscription, false for
 * unsubscribe.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "subscribeSystemLog",
 *   "params": {
 *     "subscribe": boolean
 *   }
 * }
 * </pre>
 */
public class SubscribeSystemLogRequest extends JsonrpcRequest {

	public static final String METHOD = "subscribeSystemLog";

	public static SubscribeSystemLogRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		boolean subscribe = JsonUtils.getAsBoolean(p, "subscribe");
		return new SubscribeSystemLogRequest(r.getId(), subscribe);
	}

	public static SubscribeSystemLogRequest from(JsonObject j) throws OpenemsNamedException {
		return from(GenericJsonrpcRequest.from(j));
	}

	public static SubscribeSystemLogRequest subscribe() {
		return new SubscribeSystemLogRequest(true);
	}

	public static SubscribeSystemLogRequest unsubscribe() {
		return new SubscribeSystemLogRequest(false);
	}

	private final boolean subscribe;

	private SubscribeSystemLogRequest(UUID id, boolean subscribe) {
		super(id, METHOD);
		this.subscribe = subscribe;
	}

	public SubscribeSystemLogRequest(boolean subscribe) {
		this(UUID.randomUUID(), subscribe);
	}

	public boolean getSubscribe() {
		return this.subscribe;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("subscribe", this.subscribe) //
				.build();
	}
}
