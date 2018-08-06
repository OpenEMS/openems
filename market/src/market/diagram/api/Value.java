package market.diagram.api;

import java.io.Serializable;

/**
 * A value format, that can be handled by this package's Diagram interface. It
 * allows to store any data in such a time-based diagram, that supports addition
 * and subtraction of its own, as well as multiplication and division by time.
 * 
 * @author FENECON GmbH
 *
 * @param <T>
 *            must implement this Value interface.
 */
public interface Value<T> extends Cloneable, Serializable {

	/**
	 * Adds this to v and returns the result
	 * 
	 * @param v
	 *            value to add; of type T (specific Value implementation)
	 * @return result; of type T (specific Value implementation)
	 */
	T add(T v);

	/**
	 * Subtracts v from this and returns the result
	 * 
	 * @param v
	 *            value to subtract; of type T (specific Value implementation)
	 * @return result; of type T (specific Value implementation)
	 */
	T subtract(T v);

	/**
	 * Multiplies a copy of this by v and returns the result
	 * 
	 * @param v
	 *            value to multiply by; of type T (specific Value implementation)
	 * @return result; of type T (specific Value implementation)
	 */
	T multiply(long v);

	/**
	 * Divides a copy of this by v and returns the result
	 * 
	 * @param v
	 *            value to divide by; of type T (specific Value implementation)
	 * @return result; of type T (specific Value implementation)
	 */
	T divide(long v);

	/**
	 * Creates a copy of this and returns it
	 * 
	 * @return copy; of type T (specific Value implementation)
	 */
	T clone() throws CloneNotSupportedException;

}
