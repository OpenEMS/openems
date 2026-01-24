package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class OAuthRegistryGetInitMetadataResponse extends JsonrpcResponseSuccess {

	/**
	 * Creates a {@link OAuthRegistryGetInitMetadataResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 * 
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link OAuthRegistryGetInitMetadataResponse}
	 */
	public static OAuthRegistryGetInitMetadataResponse from(JsonrpcResponseSuccess r) {
		return new OAuthRegistryGetInitMetadataResponse(r.getId(),
				OAuthInitMetadata.serializer().deserialize(r.getResult()));
	}

	public record OAuthInitMetadata(String authenticationUrl, String clientId, String redirectUrl) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthInitMetadata}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthInitMetadata> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OAuthInitMetadata.class, json -> {
				return new OAuthInitMetadata(//
						json.getString("authenticationUrl"), //
						json.getString("clientId"), //
						json.getString("redirectUrl") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("authenticationUrl", obj.authenticationUrl()) //
						.addProperty("clientId", obj.clientId()) //
						.addProperty("redirectUrl", obj.redirectUrl()) //
						.build();
			});
		}

	}

	private final OAuthInitMetadata metadata;

	public OAuthRegistryGetInitMetadataResponse(UUID id, OAuthInitMetadata metadata) {
		super(id);
		this.metadata = metadata;
	}

	public OAuthInitMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public JsonObject getResult() {
		return OAuthInitMetadata.serializer() //
				.serialize(this.metadata) //
				.getAsJsonObject();
	}

}
