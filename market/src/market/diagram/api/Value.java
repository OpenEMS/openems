package market.diagram.api;

public interface Value<T> extends Cloneable {

	/**
	 * Adds this to v and returns the result
	 * 
	 * @param v
	 *            value to add; of type T (specific Value implementation)
	 * @return result; of type T (specific Value implementation)
	 */
	T add(T v);

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
	Value<T> clone() throws CloneNotSupportedException;

}
