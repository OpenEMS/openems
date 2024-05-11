package io.openems.edge.common.jsonapi;

public interface ComponentJsonApi extends JsonApi {

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

}
