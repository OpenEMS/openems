package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class OAuthRegistryTokenResponse extends JsonrpcResponseSuccess {

	/**
	 * Creates a {@link OAuthRegistryTokenResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 * 
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link OAuthRegistryTokenResponse}
	 */
	public static OAuthRegistryTokenResponse from(JsonrpcResponseSuccess r) {
		return new OAuthRegistryTokenResponse(r.getId(), OAuthToken.serializer().deserialize(r.getResult()));
	}

	public record OAuthToken(String accessToken, String refreshToken) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthToken}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthToken> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OAuthToken.class, json -> {
				return new OAuthToken(//
						json.getString("accessToken"), //
						json.getString("refreshToken") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("accessToken", obj.accessToken()) //
						.addProperty("refreshToken", obj.refreshToken()) //
						.build();
			});
		}

	}

	private final OAuthToken metadata;

	public OAuthRegistryTokenResponse(UUID id, OAuthToken metadata) {
		super(id);
		this.metadata = metadata;
	}

	public OAuthToken getMetadata() {
		return this.metadata;
	}

	@Override
	public JsonObject getResult() {
		return OAuthToken.serializer() //
				.serialize(this.metadata) //
				.getAsJsonObject();
	}

}
