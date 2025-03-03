package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;
import io.openems.edge.core.appmanager.jsonrpc.GetEstimatedConfiguration.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetEstimatedConfiguration.Response;

public class GetEstimatedConfiguration implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getEstimatedConfiguration";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public static record Request(//
			String appId, //
			String alias, //
			JsonObject properties //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetEstimatedConfiguration.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("appId"), //
							json.getString("alias"), //
							json.getJsonObject("properties")),
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.addProperty("alias", obj.alias()) //
							.add("properties", obj.properties()) //
							.build());
		}

	}

	public record Response(//
			List<AggregateTask.AggregateTaskExecutionConfiguration> configurations //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetEstimatedConfiguration.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					// TODO polymorphic serializer
					json -> new Response(emptyList()), //
					obj -> JsonUtils.buildJsonObject() //
							.add("configurations", obj.configurations().stream() //
									.map(t -> {
										final var configJson = t.toJson();

										if (configJson.isJsonNull()) {
											return null;
										}

										return JsonUtils.buildJsonObject() //
												.addProperty("type", t.identifier()) //
												.add("configuration", configJson) //
												.build();
									}) //
									.filter(Objects::nonNull) //
									.collect(toJsonArray())) //
							.build());
		}
	}

	public record Component(String factoryId, String id, String alias, JsonObject properties) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetEstimatedConfiguration.Component}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetEstimatedConfiguration.Component> serializer() {
			return jsonObjectSerializer(GetEstimatedConfiguration.Component.class, json -> {
				return new GetEstimatedConfiguration.Component(//
						json.getString("factoryId"), //
						json.getString("id"), //
						json.getString("alias"), //
						json.getJsonObject("properties") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("factoryId", obj.factoryId()) //
						.addProperty("id", obj.id()) //
						.addProperty("alias", obj.alias()) //
						.add("properties", obj.properties()) //
						.build();
			});
		}

	}

}
