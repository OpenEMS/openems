package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;

/**
 * Updates the User Language.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "updateUserLanguage",
 *   "params": {
 *      "language": {@link Language}
 *   }
 * }
 * </pre>
 */
public class UpdateUserLanguageRequest extends JsonrpcRequest {

	public static final String METHOD = "updateUserLanguage";

	/**
	 * Create {@link UpdateUserLanguageRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link UpdateUserLanguageRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static UpdateUserLanguageRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var language = JsonUtils.getAsString(params, "language");
		return new UpdateUserLanguageRequest(request, Language.from(language));
	}

	private final Language language;

	private UpdateUserLanguageRequest(JsonrpcRequest request, Language language) throws OpenemsException {
		super(request, UpdateUserLanguageRequest.METHOD);
		this.language = language;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("language", this.language.name()) //
				.build();
	}

	public Language getLanguage() {
		return this.language;
	}

}
