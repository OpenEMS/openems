package market.diagram.api;

import java.io.Serializable;

/**
 * Creates objects of implementations of this packages Value interface.
 * 
 * @author FENECON GmbH
 *
 * @param <T>
 *            must implement this package's Value interface.
 */

public interface ValueFactory<T> extends Serializable {

	/**
	 * Returns a new value-object representing the zero-value. This MUST NOT be
	 * null.
	 * 
	 * @return zero-value; NOT null
	 */
	T NewZero();

	/**
	 * Returns a new value-object representing the maximum-value. This MUST NOT be
	 * null.
	 * 
	 * @return maximum-value; NOT null
	 */
	T NewMax();

}
