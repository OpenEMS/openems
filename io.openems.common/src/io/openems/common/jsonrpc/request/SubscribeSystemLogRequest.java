package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	/**
	 * Create {@link SubscribeSystemLogRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SubscribeSystemLogRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SubscribeSystemLogRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var subscribe = JsonUtils.getAsBoolean(p, "subscribe");
		return new SubscribeSystemLogRequest(r, subscribe);
	}

	/**
	 * Creates a JSON-RPC Request that subscribes the System-Log.
	 *
	 * @return {@link SubscribeSystemLogRequest}
	 */
	public static SubscribeSystemLogRequest subscribe() {
		return new SubscribeSystemLogRequest(true);
	}

	/**
	 * Creates a JSON-RPC Request that unsubscribes the System-Log.
	 *
	 * @return {@link SubscribeSystemLogRequest}
	 */
	public static SubscribeSystemLogRequest unsubscribe() {
		return new SubscribeSystemLogRequest(false);
	}

	private final boolean subscribe;

	private SubscribeSystemLogRequest(JsonrpcRequest request, boolean subscribe) {
		super(request, SubscribeSystemLogRequest.METHOD);
		this.subscribe = subscribe;
	}

	public SubscribeSystemLogRequest(boolean subscribe) {
		super(SubscribeSystemLogRequest.METHOD);
		this.subscribe = subscribe;
	}

	/**
	 * Whether to subscribe or unsubscribe.
	 *
	 * @return true for subscribe, false for unsubscribe
	 */
	public boolean isSubscribe() {
		return this.subscribe;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("subscribe", this.subscribe) //
				.build();
	}
}
