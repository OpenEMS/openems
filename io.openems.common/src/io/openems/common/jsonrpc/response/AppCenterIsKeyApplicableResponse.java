package io.openems.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse.Bundle;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response to validate if a key can be used.
 *
 * <pre>
 * success:
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "isKeyApplicable": boolean
 *     "additionalInfo": {
 *          "keyId": String,
 *          "bundles": {@link Bundle}[],
 *          "registrations": {@link Registration}[],
 *          "usages": {@link Usage}[]
 *     }
 *   }
 * }
 * </pre>
 */
public class AppCenterIsKeyApplicableResponse extends JsonrpcResponseSuccess {

	public final boolean isKeyApplicable;
	public final AdditionalInfo additionalInfo;

	/**
	 * Creates a {@link AppCenterIsKeyApplicableResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link AppCenterIsKeyApplicableResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsKeyApplicableResponse from(JsonrpcResponseSuccess r) throws OpenemsNamedException {
		return AppCenterIsKeyApplicableResponse.from(r.getId(), r.getResult());
	}

	/**
	 * Creates a {@link AppCenterIsKeyApplicableResponse} from a {@link JsonObject}.
	 *
	 * @param id     the id of the request
	 * @param result the {@link JsonObject}
	 * @return a {@link AppCenterIsKeyApplicableResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsKeyApplicableResponse from(UUID id, JsonObject result) throws OpenemsNamedException {
		return new AppCenterIsKeyApplicableResponse(id, //
				JsonUtils.getAsOptionalBoolean(result, "isKeyApplicable").orElse(false), //
				AdditionalInfo.from(JsonUtils.getAsJsonObject(result, "additionalInfo")) //
		);
	}

	public AppCenterIsKeyApplicableResponse(UUID id, boolean isKeyApplicable, AdditionalInfo additionalInfo) {
		super(id);
		this.isKeyApplicable = isKeyApplicable;
		this.additionalInfo = additionalInfo;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("isKeyApplicable", this.isKeyApplicable) //
				.add("additionalInfo", this.additionalInfo.toJsonObject()) //
				.build();
	}

	public static final class AdditionalInfo {
		public final String keyId;
		public final List<Bundle> bundles;
		public final List<Registration> registrations;
		public final List<Usage> usages;

		/**
		 * Creates a {@link AdditionalInfo} from an {@link JsonObject}.
		 *
		 * @param jsonObject the {@link JsonObject} to parse
		 * @return the {@link AdditionalInfo}
		 */
		public static final AdditionalInfo from(JsonObject jsonObject) throws OpenemsNamedException {
			return new AdditionalInfo(JsonUtils.getAsString(jsonObject, "keyId"), //
					JsonUtils.stream(JsonUtils.getAsJsonArray(jsonObject, "bundles")) //
							.map(JsonElement::getAsJsonArray) //
							.map(Bundle::from) //
							.toList(), //
					JsonUtils.stream(JsonUtils.getAsJsonArray(jsonObject, "registrations")) //
							.map(JsonElement::getAsJsonObject) //
							.map(Registration::from) //
							.toList(), //
					JsonUtils.stream(JsonUtils.getAsJsonArray(jsonObject, "usages")) //
							.map(JsonElement::getAsJsonObject) //
							.map(Usage::from) //
							.toList() //
			);
		}

		public AdditionalInfo(String keyId, List<Bundle> bundles, List<Registration> registrations,
				List<Usage> usages) {
			this.keyId = keyId;
			this.bundles = bundles;
			this.registrations = registrations;
			this.usages = usages;
		}

		/**
		 * Creates a {@link JsonObject} of this object.
		 *
		 * @return the {@link JsonObject}
		 */
		public final JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("keyId", this.keyId) //
					.add("bundles", this.bundles.stream() //
							.map(Bundle::toJsonArray) //
							.collect(JsonUtils.toJsonArray())) //
					.add("registrations", this.registrations.stream() //
							.map(Registration::toJsonObject) //
							.collect(JsonUtils.toJsonArray())) //
					.add("usages", this.usages.stream() //
							.map(Usage::toJsonObject) //
							.collect(JsonUtils.toJsonArray())) //
					.build();
		}

	}

	public static final class Registration {
		public final String edgeId;
		// if appId is null the registration is for the whole system
		public final String appId;

		/**
		 * Creates a {@link Registration} from an {@link JsonObject}.
		 *
		 * @param jsonObject the {@link JsonObject} to parse
		 * @return the {@link Registration}
		 */
		public static final Registration from(JsonObject jsonObject) {
			return new Registration(//
					jsonObject.get("edgeId").getAsString(), //
					JsonUtils.getAsOptionalString(jsonObject, "appId").orElse(null) //
			);
		}

		public Registration(String edgeId, String appId) {
			this.appId = appId;
			this.edgeId = edgeId;
		}

		/**
		 * Creates a {@link JsonObject} of this object.
		 *
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("edgeId", this.edgeId) //
					.addProperty("appId", this.appId) //
					.build();
		}

	}

	public static final class Usage {
		public final String appId;
		// TODO maybe deviceIds?
		public final int installedInstances;

		/**
		 * Creates a {@link Usage} from an {@link JsonObject}.
		 *
		 * @param jsonObject the {@link JsonObject} to parse
		 * @return the {@link Usage}
		 */
		public static final Usage from(JsonObject jsonObject) {
			return new Usage(//
					jsonObject.get("appId").getAsString(), //
					jsonObject.get("installedInstances").getAsInt() //
			);
		}

		public Usage(String appId, int installedInstances) {
			this.appId = appId;
			this.installedInstances = installedInstances;
		}

		/**
		 * Creates a {@link JsonObject} of this object.
		 *
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("appId", this.appId) //
					.addProperty("installedInstances", this.installedInstances) //
					.build();
		}

	}

}
