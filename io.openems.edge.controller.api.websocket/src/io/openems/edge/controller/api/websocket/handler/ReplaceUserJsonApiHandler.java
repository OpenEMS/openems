package io.openems.edge.controller.api.websocket.handler;

import java.util.Collections;
import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.ReplaceUser;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.websocket.ControllerApiWebsocket;

@Component(//
		property = "entry=" + EdgeRpcRequestHandler.ENTRY_POINT, //
		service = { ReplaceUserJsonApiHandler.class, JsonApi.class }, //
		scope = ServiceScope.SINGLETON //
)
public class ReplaceUserJsonApiHandler implements JsonApi {

	private JsonApiBuilder builder;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.delegate(new ReplaceUser(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var request = ReplaceUser.Request.serializer().deserialize(call.getRequest().getParams());

			// Replace user
			final var user = call.get(EdgeKeys.USER_KEY);
			call.put(EdgeKeys.USER_KEY, new User(user.getId(), user.getName(), request.language(), request.role()));

			return new EdgeRpcRequest(ControllerApiWebsocket.EDGE_ID,
					GenericJsonrpcRequest.from(request.request().getAsJsonObject()));
		}, b -> this.builder, Function.identity(), Collections::emptyList);
	}

	public void setBuilder(JsonApiBuilder builder) {
		this.builder = builder;
	}

}