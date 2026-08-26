package io.openems.edge.common.update.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.update.jsonrpc.GetUpdateables.Response;

public class GetUpdateables implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getUpdateables";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(List<UpdateableMetadata> updateableMetadatas) {

		public record UpdateableMetadata(//
				String id, //
				String name, //
				String description //
		) {

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link GetUpdateables.Response.UpdateableMetadata}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetUpdateables.Response.UpdateableMetadata> serializer() {
				return jsonObjectSerializer(GetUpdateables.Response.UpdateableMetadata.class, json -> {
					return new GetUpdateables.Response.UpdateableMetadata(//
							json.getString("id"), //
							json.getString("name"), //
							json.getString("description") //
					);
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("id", obj.id()) //
							.addProperty("name", obj.name()) //
							.addProperty("description", obj.description()) //
							.build();
				});
			}

		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetUpdateables.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetUpdateables.Response> serializer() {
			return jsonObjectSerializer(json -> {
				return new GetUpdateables.Response(json.getList("updateables", UpdateableMetadata.serializer()));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("updateables",
								UpdateableMetadata.serializer().toListSerializer().serialize(obj.updateableMetadatas())) //
						.build();
			});
		}

	}

}
