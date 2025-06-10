package io.openems.edge.controller.api.rest.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.ComponentConfigRequestHandler;

/**
 * This class makes it possible to request component update, delete, ...
 * {@link ComponentConfigRequestHandler} in a websocket connection. It just
 * "binds" the component which handles the request and provides their methods
 * indirectly.
 */
@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class BindingComponentConfigRequestHandler implements JsonApi {

	private final ComponentConfigRequestHandler componentConfigRequestHandler;

	@Activate
	public BindingComponentConfigRequestHandler(//
			@Reference ComponentConfigRequestHandler componentConfigRequestHandler //
	) {
		this.componentConfigRequestHandler = componentConfigRequestHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.componentConfigRequestHandler.buildJsonApiRoutes(builder);
	}

}