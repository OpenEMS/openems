package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public class OAuthRegistryGetInitMetadataRequest extends JsonrpcRequest {

	public static final String METHOD = "getInitMetadata";

	/**
	 * Creates a {@link OAuthRegistryGetInitMetadataRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link OAuthRegistryGetInitMetadataRequest}
	 */
	public static OAuthRegistryGetInitMetadataRequest from(JsonrpcRequest r) {
		return new OAuthRegistryGetInitMetadataRequest(r, OAuthInitRequest.serializer().deserialize(r.getParams()));
	}

	public record OAuthInitRequest(String identifier) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthInitRequest}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthInitRequest> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(OAuthInitRequest.class, json -> {
				return new OAuthInitRequest(//
						json.getString("identifier") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.build();
			});
		}

	}

	private final OAuthInitRequest metadata;

	public OAuthRegistryGetInitMetadataRequest(OAuthInitRequest metadata) {
		super(OAuthRegistryGetInitMetadataRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthRegistryGetInitMetadataRequest(JsonrpcRequest request, OAuthInitRequest metadata) {
		super(request, OAuthRegistryGetInitMetadataRequest.METHOD);
		this.metadata = metadata;
	}

	public OAuthInitRequest getMetadata() {
		return this.metadata;
	}

	@Override
	public JsonObject getParams() {
		return OAuthInitRequest.serializer() //
				.serialize(this.metadata) //
				.getAsJsonObject();
	}
}
