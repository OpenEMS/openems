package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;

/**
 * Declares a class as being able to handle JSON-RPC Requests which were send in
 * a {@link ComponentJsonApiRequest}.
 */
public interface ComponentJsonApi extends JsonApi {

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

}
