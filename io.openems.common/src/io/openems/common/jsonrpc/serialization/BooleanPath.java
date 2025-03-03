package io.openems.common.jsonrpc.serialization;

public interface BooleanPath extends JsonPath {

	/**
	 * Gets the boolean value of the current path.
	 * 
	 * @return the value
	 */
	public boolean get();

}
