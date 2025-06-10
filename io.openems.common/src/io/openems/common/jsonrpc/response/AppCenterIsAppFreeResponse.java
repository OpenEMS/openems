package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response to validate if a app is free.
 *
 * <pre>
 * success:
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "isAppFree": boolean
 *   }
 * }
 * </pre>
 */
public class AppCenterIsAppFreeResponse extends JsonrpcResponseSuccess {

	public final boolean isAppFree;

	/**
	 * Creates a {@link AppCenterIsAppFreeResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link AppCenterIsAppFreeResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsAppFreeResponse from(JsonrpcResponseSuccess r) throws OpenemsNamedException {
		return AppCenterIsAppFreeResponse.from(r.getId(), r.getResult());
	}

	/**
	 * Creates a {@link AppCenterIsAppFreeResponse} from a {@link JsonObject}.
	 *
	 * @param id     the id of the request
	 * @param result the {@link JsonObject}
	 * @return a {@link AppCenterIsAppFreeResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsAppFreeResponse from(UUID id, JsonObject result) throws OpenemsNamedException {
		return new AppCenterIsAppFreeResponse(id, //
				JsonUtils.getAsBoolean(result, "isAppFree") //
		);
	}

	public AppCenterIsAppFreeResponse(UUID id, boolean isAppFree) {
		super(id);
		this.isAppFree = isAppFree;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("isAppFree", this.isAppFree) //
				.build();
	}

}
