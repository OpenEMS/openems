package io.openems.common.jsonrpc.serialization;

import java.util.UUID;

public interface StringPath extends JsonPath {

	/**
	 * Gets the string value of the current path.
	 * 
	 * @return the value
	 */
	public String get();

	/**
	 * Gets the value as a {@link UUID}.
	 * 
	 * @return the {@link UUID}
	 */
	public UUID getAsUuid();

}
