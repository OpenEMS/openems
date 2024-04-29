package io.openems.edge.controller.api.backend.handler;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticatedRpcRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.MultipleJsonApiBinder;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.user.User;

@Component(property = { "entry=" + RootRequestHandler.ENTRY_POINT })
public class AuthenticatedRequestHandler implements JsonApi {

	public static final String ENTRY_POINT = "edge.backend.authenticated";

	private final MultipleJsonApiBinder binder = new MultipleJsonApiBinder();

	@Reference(//
			target = "(entry=" + ENTRY_POINT + ")", //
			bind = "bindHandler", unbind = "unbindHandler", //
			policyOption = ReferencePolicyOption.GREEDY, //
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	protected void bindHandler(JsonApi handler) {
		this.binder.bindJsonApi(handler);
	}

	protected void unbindHandler(JsonApi handler) {
		this.binder.unbindJsonApi(handler);
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder b) {
		b.delegate(AuthenticatedRpcRequest.METHOD, endpoint -> {

		}, t -> {
			final var authenticatedRpcRequest = AuthenticatedRpcRequest.from(t.getRequest(), User::from);
			t.put(EdgeKeys.USER_KEY, authenticatedRpcRequest.getUser());
			return authenticatedRpcRequest.getPayload();
		}, c -> this.binder.getJsonApiBuilder(), response -> {
			// wrap response in a AuthenticatedRpcResponse if successful
			if (response instanceof JsonrpcResponseSuccess success) {
				return new AuthenticatedRpcResponse(response.getId(), success);
			}
			return response;
		}, () -> {
			final var subrequest = new Subrequest(JsonUtils.buildJsonObject().build());
			subrequest.addRpcBuilderFor(this.binder.getJsonApiBuilder(), "payload");
			return List.of(subrequest);
		});
	}

}
