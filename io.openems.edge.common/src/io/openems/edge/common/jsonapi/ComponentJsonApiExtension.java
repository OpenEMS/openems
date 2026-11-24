package io.openems.edge.common.jsonapi;

public interface ComponentJsonApiExtension<T extends ComponentJsonApi> {

	/**
	 * The class of the {@link ComponentJsonApi} that this extension extends.
	 * 
	 * @return the class of the {@link ComponentJsonApi} that this extension extends
	 */
	Class<? extends T> extendClass();

	/**
	 * Builds the JSON API routes for the given {@link ComponentJsonApi} service
	 * using the provided {@link JsonApiBuilder}.
	 * 
	 * @param service the {@link ComponentJsonApi} service to build routes for
	 * @param builder the {@link JsonApiBuilder} to use for building the routes
	 */
	void buildJsonApiRoutes(T service, JsonApiBuilder builder);

}
