package io.openems.edge.controller.api.websocket.handler;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.MultipleJsonApiBinder;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.controller.api.websocket.ControllerApiWebsocket;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class EdgeRpcRequestHandler implements JsonApi {

	public static final String ENTRY_POINT = "edge.websocket.edgeRpc";

	private final MultipleJsonApiBinder binder = new MultipleJsonApiBinder();

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			target = "(entry=" + ENTRY_POINT + ")" //
	)
	protected void bindJsonApi(JsonApi jsonApi) {
		this.binder.bindJsonApi(jsonApi);
	}

	protected void unbindJsonApi(JsonApi jsonApi) {
		this.binder.unbindJsonApi(jsonApi);
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.delegate(EdgeRpcRequest.METHOD, endpoint -> {

		}, call -> {
			return EdgeRpcRequest.from(call.getRequest()).getPayload();
		}, b -> {
			return this.binder.getJsonApiBuilder();
		}, response -> {
			// wrap response in a EdgeRpcResponse if successful
			if (response instanceof JsonrpcResponseSuccess success) {
				return new EdgeRpcResponse(response.getId(), success);
			}
			return response;
		}, () -> {
			final var subrequest = new Subrequest(JsonUtils.buildJsonObject() //
					.addProperty("edgeId", ControllerApiWebsocket.EDGE_ID) //
					.build());
			subrequest.addRpcBuilderFor(this.binder.getJsonApiBuilder(), "payload");
			return List.of(subrequest);
		});
	}

}
