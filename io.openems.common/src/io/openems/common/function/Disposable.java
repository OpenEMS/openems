package io.openems.common.function;

/**
 * Same as AutoCloseable but without checked exception.
 */
public interface Disposable {

	/**
	 * Disposes the object.
	 */
	void dispose();

}
