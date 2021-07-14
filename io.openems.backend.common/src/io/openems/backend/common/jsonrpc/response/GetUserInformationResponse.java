package io.openems.backend.common.jsonrpc.response;

import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.GetUserInformationRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ObjectUtils;

/**
 * Represents a JSON-RPC Response for {@link GetUserInformationRequest}.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "user": {
 *       "firstname": string,
 *       "lastname": string,
 *       "email": string,
 *       "phone": string,
 *       "address": {
 *         "street": string,
 *         "city": string,
 *         "zip": string,
 *         "country": string
 *       },
 *       "company": {
 *         "name": string
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class GetUserInformationResponse extends JsonrpcResponseSuccess {

	private final Map<String, Object> userInformation;

	public GetUserInformationResponse(UUID id, Map<String, Object> userInformation) {
		super(id);
		this.userInformation = userInformation;
	}

	@Override
	public JsonObject getResult() {
		JsonObject companyJson = JsonUtils.buildJsonObject() //
				.addProperty("name", ObjectUtils.getAsString(this.userInformation.get("commercial_company_name"))) //
				.build();

		String country = null;
		Object[] array = ObjectUtils.getAsObjectArrray(this.userInformation.get("country_id"));
		if (array.length > 1) {
			country = ObjectUtils.getAsString(array[1]);
		}

		JsonObject addressJson = JsonUtils.buildJsonObject() //
				.addProperty("street", ObjectUtils.getAsString(this.userInformation.get("street"))) //
				.addProperty("zip", ObjectUtils.getAsString(this.userInformation.get("zip"))) //
				.addProperty("city", ObjectUtils.getAsString(this.userInformation.get("city"))) //
				.addProperty("country", country) //
				.build();

		JsonObject userJson = JsonUtils.buildJsonObject() //
				.addProperty("firstname", ObjectUtils.getAsString(this.userInformation.get("firstname"))) //
				.addProperty("lastname", ObjectUtils.getAsString(this.userInformation.get("lastname"))) //
				.addProperty("email", ObjectUtils.getAsString(this.userInformation.get("email"))) //
				.addProperty("phone", ObjectUtils.getAsString(this.userInformation.get("phone"))) //
				.add("address", addressJson) //
				.add("company", companyJson) //
				.build();

		return JsonUtils.buildJsonObject() //
				.add("user", userJson) //
				.build();
	}

}
