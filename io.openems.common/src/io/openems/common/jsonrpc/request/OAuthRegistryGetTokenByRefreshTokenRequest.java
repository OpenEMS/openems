package io.openems.common.jsonrpc.request;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class OAuthRegistryGetTokenByRefreshTokenRequest extends JsonrpcRequest {

	public static final String METHOD = "getTokenByRefreshToken";

	/**
	 * Creates a {@link OAuthRegistryGetTokenByRefreshTokenRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link OAuthRegistryGetTokenByRefreshTokenRequest}
	 */
	public static OAuthRegistryGetTokenByRefreshTokenRequest from(JsonrpcRequest r) {
		return new OAuthRegistryGetTokenByRefreshTokenRequest(r,
				OAuthGetTokenByRefreshTokenRequest.serializer().deserialize(r.getParams()));
	}

	public record OAuthGetTokenByRefreshTokenRequest(String identifier, String refreshToken, List<String> scopes) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthGetTokenByRefreshTokenRequest}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthGetTokenByRefreshTokenRequest> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OAuthGetTokenByRefreshTokenRequest.class, json -> {
				return new OAuthGetTokenByRefreshTokenRequest(//
						json.getString("identifier"), //
						json.getString("refreshToken"), //
						json.getList("scopes", JsonElementPath::getAsString) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.addProperty("refreshToken", obj.refreshToken()) //
						.add("scopes", obj.scopes().stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.build();
			});
		}

	}

	private final OAuthGetTokenByRefreshTokenRequest metadata;

	public OAuthRegistryGetTokenByRefreshTokenRequest(OAuthGetTokenByRefreshTokenRequest metadata) {
		super(OAuthRegistryGetTokenByRefreshTokenRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthRegistryGetTokenByRefreshTokenRequest(JsonrpcRequest request, OAuthGetTokenByRefreshTokenRequest metadata) {
		super(request, OAuthRegistryGetTokenByRefreshTokenRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthGetTokenByRefreshTokenRequest getMetadata() {
		return this.metadata;
	}

	@Override
	public JsonObject getParams() {
		return OAuthGetTokenByRefreshTokenRequest.serializer() //
				.serialize(this.metadata) //
				.getAsJsonObject();
	}
}
