package io.openems.edge.controller.api.common.handler;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.JsonApiEndpoint;
import io.openems.edge.common.jsonapi.JsonrpcEndpointGuard;
import io.openems.edge.common.jsonapi.JsonrpcRoleEndpointGuard;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.jsonapi.Tag;
import io.openems.edge.controller.api.common.handler.RoutesJsonApiHandler.Routes.Response;

@Component(//
		service = { RoutesJsonApiHandler.class, JsonApi.class }, //
		scope = ServiceScope.PROTOTYPE //
)
public class RoutesJsonApiHandler implements JsonApi {

	private JsonApiBuilder builder;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new Routes(), call -> {
			final var b = this.getBuilder();
			if (b == null) {
				throw new OpenemsException("Builder is not yet set.");
			}

			final var result = getAllRequests(emptyList(), b);

			return new Routes.Response(1, result);
		});
	}

	public JsonApiBuilder getBuilder() {
		return this.builder;
	}

	public void setBuilder(JsonApiBuilder builder) {
		this.builder = builder;
	}

	public static class Routes implements EndpointRequestType<EmptyObject, Response> {

		@Override
		public String getMethod() {
			return "routes";
		}

		@Override
		public JsonSerializer<EmptyObject> getRequestSerializer() {
			return EmptyObject.serializer();
		}

		@Override
		public JsonSerializer<Response> getResponseSerializer() {
			return Response.serializer();
		}

		public static record Response(int version, List<Endpoint> endpoints) {

			/**
			 * Returns a {@link JsonSerializer} for a
			 * {@link RoutesJsonApiHandler.Routes.Response}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<RoutesJsonApiHandler.Routes.Response> serializer() {
				return jsonObjectSerializer(RoutesJsonApiHandler.Routes.Response.class, json -> {
					return new RoutesJsonApiHandler.Routes.Response(json.getInt("version"),
							json.getList("endpoints", Endpoint.serializer()));
				}, obj -> {
					return JsonUtils.buildJsonObject() //
							.addProperty("version", obj.version()) //
							.add("endpoints", obj.endpoints().stream() //
									.map(Endpoint.serializer()::serialize) //
									.collect(toJsonArray())) //
							.build();
				});
			}

		}

	}

	private record EndpointParent(JsonApiEndpoint parentEndpoint, JsonElement baseRequest, String[] path) {

	}

	private record Endpoint(String method, String description, List<Tag> tags, List<JsonrpcEndpointGuard> guards,
			JsonArray parent, JsonObject request, JsonObject response) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link RoutesJsonApiHandler.Endpoint}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<RoutesJsonApiHandler.Endpoint> serializer() {
			return jsonObjectSerializer(RoutesJsonApiHandler.Endpoint.class, json -> {
				return new RoutesJsonApiHandler.Endpoint(//
						json.getString("method"), //
						json.getStringOrNull("description"), //
						json.getList("tags", Tag.serializer()), //
						emptyList(), //
						json.getJsonArray("parent"), //
						json.getJsonObject("request"), //
						json.getJsonObject("response"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("method", obj.method()) //
						.addPropertyIfNotNull("description", obj.description) //
						.add("tags", obj.tags().stream() //
								.map(Tag.serializer()::serialize) //
								.collect(toJsonArray()))
						.add("guards", obj.guards().stream() //
								.map(t -> switch (t) {
								case JsonrpcRoleEndpointGuard a //
									-> JsonrpcRoleEndpointGuard.serializer().serialize(a);
								default -> null;
								}) //
								.filter(Objects::nonNull) //
								.collect(toJsonArray()))
						.add("parent", obj.parent()) //
						.add("request", obj.request()) //
						.add("response", obj.response()) //
						.build();
			});
		}

	}

	private static List<Endpoint> getAllRequests(List<EndpointParent> parent, JsonApiBuilder builder) {
		final var result = new ArrayList<Endpoint>();
		for (var entry : builder.getEndpoints().entrySet()) {
			final var def = entry.getValue().getDef();

			final var parentArray = parent.stream() //
					.map(t -> JsonUtils.buildJsonObject() //
							.addProperty("method", t.parentEndpoint().getMethod()) //
							.add("request", JsonUtils.buildJsonObject() //
									.add("base", t.baseRequest()) //
									.add("pathToSubrequest", Stream.of(t.path()) //
											.map(JsonPrimitive::new) //
											.collect(toJsonArray())) //
									.build())
							.build()) //
					.collect(toJsonArray());

			final var request = def.getEndpointRequestBuilder();
			final var response = def.getEndpointResponseBuilder();

			result.add(new Endpoint(entry.getKey(), def.getDescription(), def.getTags(), def.getGuards(), parentArray,
					JsonUtils.buildJsonObject() //
							.onlyIf(request.getSerializer() != null, t -> {
								t.add("json", request.getSerializer().descriptor().toJson()) //
										.add("examples", request.createExampleArray());
							}).build(),
					JsonUtils.buildJsonObject() //
							.onlyIf(response.getSerializer() != null, t -> {
								t.add("json", response.getSerializer().descriptor().toJson()) //
										.add("examples", response.createExampleArray());
							}).build()));

			final List<Subrequest> subroutes = entry.getValue().getSubroutes() != null
					? entry.getValue().getSubroutes().get()
					: emptyList();

			for (var subroute : subroutes) {
				for (var b : subroute.getSubrouteToBuilder()) {

					if (b.endpointSupplier() != null) {
						final var endpoints = Endpoint.serializer().toListSerializer()
								.deserialize(b.endpointSupplier().get());

						final var e = endpoints.stream().map(t -> {

							final var parents = parent.stream() //
									.map(p -> JsonUtils.buildJsonObject() //
											.addProperty("method", p.parentEndpoint().getMethod()) //
											.add("request", JsonUtils.buildJsonObject() //
													.add("base", p.baseRequest()) //
													.add("pathToSubrequest", Stream.of(p.path()) //
															.map(JsonPrimitive::new) //
															.collect(toJsonArray())) //
													.build())
											.build()) //
									.collect(toJsonArray());

							parents.add(JsonUtils.buildJsonObject() //
									.addProperty("method", entry.getValue().getMethod()) //
									.add("request", JsonUtils.buildJsonObject() //
											.add("base", new JsonObject()) //
											.add("pathToSubrequest", JsonUtils.buildJsonArray() //
													.add("payload") //
													.build()) //
											.build())
									.build());

							if (!t.parent().isEmpty()) {
								t.parent().remove(0);
							}
							parents.addAll(t.parent());

							return new Endpoint(t.method(), t.description(), t.tags(), t.guards(), parents, t.request(),
									t.response());
						}).toList();

						result.addAll(e);
					}

					if (b.builder() == null) {
						continue;
					}

					var currentParent = new EndpointParent(entry.getValue(), subroute.getBaseRequest(), b.path());
					final var subparents = new ArrayList<>(parent);
					subparents.add(currentParent);
					result.addAll(getAllRequests(subparents, b.builder()));
				}
			}

		}
		return result;
	}

}