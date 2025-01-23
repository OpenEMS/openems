package io.openems.edge.controller.api.websocket.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.ComponentRequestHandler;

/**
 * This class makes it possible to request {@link ComponentJsonApiRequest} in a
 * websocket connection. It just "binds" the component which handles the request
 * and provides their methods indirectly.
 */
@Component(property = "entry=" + EdgeRpcRequestHandler.ENTRY_POINT)
public class BindingComponentRequestHandler implements JsonApi {

	private final ComponentRequestHandler componentRequestHandler;

	@Activate
	public BindingComponentRequestHandler(//
			@Reference ComponentRequestHandler componentRequestHandler //
	) {
		this.componentRequestHandler = componentRequestHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.componentRequestHandler.buildJsonApiRoutes(builder);
	}

}