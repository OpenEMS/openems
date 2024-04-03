package io.openems.edge.common.jsonapi;

/**
 * Declares a class as being able to handle JSON-RPC Requests.
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