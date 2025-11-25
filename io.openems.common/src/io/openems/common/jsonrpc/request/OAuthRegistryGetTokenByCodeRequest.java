package io.openems.common.jsonrpc.request;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class OAuthRegistryGetTokenByCodeRequest extends JsonrpcRequest {

	public static final String METHOD = "getTokenByCode";

	/**
	 * Creates a {@link OAuthRegistryGetTokenByCodeRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link OAuthRegistryGetTokenByCodeRequest}
	 */
	public static OAuthRegistryGetTokenByCodeRequest from(JsonrpcRequest r) {
		return new OAuthRegistryGetTokenByCodeRequest(r,
				OAuthGetTokenByCodeRequest.serializer().deserialize(r.getParams()));
	}

	public record OAuthGetTokenByCodeRequest(String identifier, String code, List<String> scopes, String codeVerifier) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthGetTokenByCodeRequest}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthGetTokenByCodeRequest> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OAuthGetTokenByCodeRequest.class, json -> {
				return new OAuthGetTokenByCodeRequest(//
						json.getString("identifier"), //
						json.getString("code"), //
						json.getList("scopes", JsonElementPath::getAsString), //
						json.getStringOrNull("codeVerifier") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.addProperty("code", obj.code()) //
						.add("scopes", obj.scopes().stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.addPropertyIfNotNull("codeVerifier", obj.codeVerifier()) //
						.build();
			});
		}

	}

	private final OAuthGetTokenByCodeRequest metadata;

	public OAuthRegistryGetTokenByCodeRequest(OAuthGetTokenByCodeRequest metadata) {
		super(OAuthRegistryGetTokenByCodeRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthRegistryGetTokenByCodeRequest(JsonrpcRequest request, OAuthGetTokenByCodeRequest metadata) {
		super(request, OAuthRegistryGetTokenByCodeRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthGetTokenByCodeRequest getMetadata() {
		return this.metadata;
	}

	@Override
	public JsonObject getParams() {
		return OAuthGetTokenByCodeRequest.serializer() //
				.serialize(this.metadata) //
				.getAsJsonObject();
	}
}
