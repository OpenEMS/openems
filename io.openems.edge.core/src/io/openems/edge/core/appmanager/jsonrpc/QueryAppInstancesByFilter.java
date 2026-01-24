package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter.Request;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter.Response;

/**
 * Updates an {@link OpenemsAppInstance}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppConfig",
 *   "params": {
 *     "componentId": string (uuid),
 *     "properties": {}
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "instance": {@link OpenemsAppInstance#toJsonObject()}
 *     "warnings": string[]
 *   }
 * }
 * </pre>
 */
public class QueryAppInstancesByFilter implements EndpointRequestType<Request, Response> {

	public record Pagination(int limit) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryAppInstancesByFilter.Pagination}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Pagination> serializer() {
			return jsonObjectSerializer(Pagination.class, json -> {
				return new Pagination(json.getInt("limit"));
			}, obj -> {
				return JsonUtils.buildJsonObject()//
						.addPropertyIfNotNull("limit", obj.limit())//
						.build();
			});
		}

	}

	public record Filter(ComponentFilter component, Set<OpenemsAppCategory> categorys) {

		public record ComponentFilter(Set<String> componentId, Set<String> factoryId) {
			/**
			 * Returns a {@link JsonSerializer} for a {@link ComponentFilter}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<ComponentFilter> serializer() {
				return jsonObjectSerializer(ComponentFilter.class, json -> {
					return new ComponentFilter(//
							json.getOptionalSet("componentId", JsonElementPath::getAsString).orElse(null), //
							json.getOptionalSet("factoryId", JsonElementPath::getAsString).orElse(null));
				}, obj -> {
					return JsonUtils.buildJsonObject().onlyIf(obj != null, j -> {
						j.onlyIf(obj.componentId != null, t -> t.add("componentId", obj.componentId.stream() //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray())));
						j.onlyIf(obj.factoryId != null, t -> t.add("factoryId", obj.factoryId.stream() //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray())));
					}).build();
				});
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link ComponentFilter}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Filter> serializer() {
			return jsonObjectSerializer(Filter.class, json -> {
				return new Filter(//
						json.getObjectOrNull("component", ComponentFilter.serializer()), //
						json.getOptionalSet("categorys", j -> j.getAsEnum(OpenemsAppCategory.class)).orElse(null));
			}, obj -> {
				var builder = JsonUtils.buildJsonObject(); //
				if (obj.component != null) {
					builder.add("component", ComponentFilter.serializer().serialize(obj.component)); //
				}
				if (obj.categorys != null) {
					builder.add("categorys", obj.categorys.stream() //
							.map(OpenemsAppCategory::name) //
							.map(JsonPrimitive::new) //
							.collect(toJsonArray())); //
				}
				return builder.build();
			});
		}

	}

	@Override
	public String getMethod() {
		return "queryAppInstancesByFilter";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(Filter filter, Pagination pagination) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryAppInstancesByFilter.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(json.getObjectOrNull("filter", Filter.serializer()), //
						json.getObjectOrNull("pagination", Pagination.serializer()));
			}, obj -> {
				return JsonUtils.buildJsonObject()//
						.add("filter", Filter.serializer().serialize(obj.filter()))//
						.add("pagination", Pagination.serializer().serialize(obj.pagination()))//
						.build();
			});
		}

	}

	public record Response(List<OpenemsAppInstance> apps) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link QueryAppInstancesByFilter.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(json.getList("apps", j -> {
					return OpenemsAppInstance.serializer().deserialize(j.get());
				}));
			}, obj -> {
				return JsonUtils.buildJsonObject()//
						.add("apps", obj.apps.stream() //
								.map(OpenemsAppInstance.serializer()::serialize)//
								.collect(toJsonArray()))
						.build(); //
			});
		}
	}
}
