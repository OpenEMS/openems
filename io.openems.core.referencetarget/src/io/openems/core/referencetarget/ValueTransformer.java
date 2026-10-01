package io.openems.core.referencetarget;

public interface ValueTransformer {

	/**
	 * Transforms a resolved value before insertion into a reference target.
	 *
	 * @param value the resolved value; never null
	 * @return the transformed value
	 * @throws IllegalArgumentException if the value cannot be transformed
	 */
	Object transform(Object value);

}