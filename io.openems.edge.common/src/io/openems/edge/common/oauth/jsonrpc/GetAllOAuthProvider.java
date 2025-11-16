package io.openems.edge.common.oauth.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.oauth.OAuthCore.OAuthMetaInfo;

public class GetAllOAuthProvider implements EndpointRequestType<EmptyObject, GetAllOAuthProvider.Response> {

	@Override
	public String getMethod() {
		return "getAllOAuthProvider";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(List<OAuthMetaInfo> metaInfos) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(//
						json.getList("metaInfos", OAuthMetaInfo.serializer()) //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("metaInfos", OAuthMetaInfo.serializer().toListSerializer().serialize(obj.metaInfos()))
						.build();
			});
		}

	}

}
