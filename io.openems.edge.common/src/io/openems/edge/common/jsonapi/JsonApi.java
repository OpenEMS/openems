package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;

/**
 * Declares a class as being able to handle JSON-RPC Requests.
 * 
 * <p>
 * NOTE: the routes are not automatically available somewhere they must have
 * some kind of entry point see {@link io.openems.edge.controller.api.backend},
 * {@link io.openems.edge.controller.api.rest},
 * {@link io.openems.edge.controller.api.websocket}.
 * 
 * <p>
 * Most of the times on edge site only {@link ComponentJsonApiRequest} are
 * required, for them use the interface {@link ComponentJsonApi}, because all
 * instances of it are automatically binded and available with this request.
 */
public interface JsonApi {

	/**
	 * Specifies routes of the current component in the given builder.
	 * 
	 * <p>
	 * Example: <br>
	 * 
	 * <pre>
	 * {@code @Override}
	 * public void buildJsonApiRoutes(JsonApiBuilder builder) {
	 *     builder.rpc("METHOD_NAME", call -> {
	 *        // handle call...
	 *     });
	 * }
	 * </pre>
	 * 
	 * @param builder the builder to add the routes
	 */
	public void buildJsonApiRoutes(JsonApiBuilder builder);

}