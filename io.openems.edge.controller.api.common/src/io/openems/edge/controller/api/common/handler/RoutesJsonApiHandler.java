package io.openems.edge.controller.api.common.handler;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.JsonApiEndpoint;
import io.openems.edge.common.jsonapi.JsonrpcRoleEndpointGuard;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.jsonapi.Tag;

@Component(//
		service = { RoutesJsonApiHandler.class, JsonApi.class }, //
		scope = ServiceScope.PROTOTYPE //
)
public class RoutesJsonApiHandler implements JsonApi {

	private JsonApiBuilder builder;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest("routes", call -> {
			final var b = this.getBuilder();
			if (b == null) {
				throw new OpenemsException("Builder is not yet set.");
			}

			final var result = getAllRequests(emptyList(), b);

			return new GenericJsonrpcResponseSuccess(call.getRequest().getId(), JsonUtils.buildJsonObject() //
					.addProperty("version", "1") //
					.add("endpoints", result.stream().collect(toJsonArray())) //
					.build());
		});
	}

	public JsonApiBuilder getBuilder() {
		return this.builder;
	}

	public void setBuilder(JsonApiBuilder builder) {
		this.builder = builder;
	}

	private record EndpointParent(JsonApiEndpoint parentEndpoint, JsonElement baseRequest, String[] path) {

	}

	private static final List<JsonObject> getAllRequests(List<EndpointParent> parent, JsonApiBuilder builder) {
		final var result = new ArrayList<JsonObject>();
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

			final var resultJson = JsonUtils.buildJsonObject() //
					.addProperty("method", entry.getKey()) //
					.addPropertyIfNotNull("description", def.getDescription()) //
					.add("tags", def.getTags().stream() //
							.map(Tag.serializer()::serialize) //
							.collect(toJsonArray()))
					.add("guards", def.getGuards().stream() //
							.map(t -> {
								if (t instanceof JsonrpcRoleEndpointGuard a) {
									return JsonrpcRoleEndpointGuard.serializer().serialize(a);
								}
								return null;
							}) //
							.filter(Objects::nonNull) //
							.collect(toJsonArray()))
					.add("parent", parentArray);

			def.applyRequestBuilder(request -> {
				resultJson.add("request", JsonUtils.buildJsonObject() //
						.onlyIf(request.getSerializer() != null, t -> {
							t.add("json", request.getSerializer().descriptor().toJson()) //
									.add("examples", request.createExampleArray());
						}).build());
			});

			def.applyResponseBuilder(response -> {
				resultJson.add("response", JsonUtils.buildJsonObject() //
						.onlyIf(response.getSerializer() != null, t -> {
							t.add("json", response.getSerializer().descriptor().toJson()) //
									.add("examples", response.createExampleArray());
						}).build());
			});

			result.add(resultJson.build());

			final List<Subrequest> subroutes = entry.getValue().getSubroutes() != null
					? entry.getValue().getSubroutes().get()
					: emptyList();

			for (var subroute : subroutes) {
				for (var b : subroute.getSubrouteToBuilder()) {
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