package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to schedule updates.
 *
 * <p>
 * Set 'subscribe' param to 'true' to start the subscription and 'false' to
 * unsubscribe.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "subscribeSchedule",
 *   "params": {
 *     "subscribe": boolean
 *   }
 * }
 * </pre>
 */
public class SubscribeScheduleRequest extends JsonrpcRequest {

	public static final String METHOD = "subscribeSchedule";

	/**
	 * Creates a {@link SubscribeScheduleRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link SubscribeScheduleRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SubscribeScheduleRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		final var params = request.getParams();
		final var subscribe = JsonUtils.getAsBoolean(params, "subscribe");
		return new SubscribeScheduleRequest(request, subscribe);
	}

	/**
	 * Creates a JSON-RPC Request to subscribe to schedule updates.
	 *
	 * @return the {@link SubscribeScheduleRequest}
	 */
	public static SubscribeScheduleRequest subscribe() {
		return new SubscribeScheduleRequest(true);
	}

	/**
	 * Creates a JSON-RPC Request to unsubscribe from schedule updates.
	 *
	 * @return the {@link SubscribeScheduleRequest}
	 */
	public static SubscribeScheduleRequest unsubscribe() {
		return new SubscribeScheduleRequest(false);
	}

	private final boolean subscribe;

	private SubscribeScheduleRequest(JsonrpcRequest request, boolean subscribe) {
		super(request, SubscribeScheduleRequest.METHOD);
		this.subscribe = subscribe;
	}

	public SubscribeScheduleRequest(boolean subscribe) {
		super(SubscribeScheduleRequest.METHOD);
		this.subscribe = subscribe;
	}

	/**
	 * Whether to subscribe or unsubscribe.
	 *
	 * @return true to subscribe; false to unsubscribe
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
