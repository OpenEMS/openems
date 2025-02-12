package io.openems.common.jsonrpc.serialization;

public interface StringPath<T> extends JsonPath {

	/**
	 * Gets the string value of the current path.
	 * 
	 * @return the value
	 */
	public String getRaw();

	/**
	 * Gets the parsed value of this string path.
	 * 
	 * @return the parsed value of this string
	 */
	public T get();

}
