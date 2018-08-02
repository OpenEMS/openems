package market.diagram.api;

import java.io.Serializable;
import java.util.Date;

/**
 * A diagram, that can store a timeline of values.
 * 
 * @author FENECON GmbH
 *
 * @param <T>
 *            must implement this package's Value interface.
 */
public interface Diagram<T extends Value<T>> extends Serializable {

	/**
	 * Writes the value for the given period including the milliseconds of both
	 * limits. Any entries inside this period will be overwritten or cut off. If to
	 * is before from nothing is done.
	 * 
	 * @param from
	 *            start of period
	 * @param to
	 *            end of period
	 * @param value
	 *            content to write; of type T (specific Value implementation)
	 */
	void setValue(Date from, Date to, T value);

	/**
	 * Erases all values for the given period including the milliseconds of both
	 * limits. Any entries inside this period will be overwritten or cut off. If to
	 * is before from nothing is done.
	 * 
	 * @param from
	 *            start of period
	 * @param to
	 *            end of period
	 */
	void erasePeriod(Date from, Date to);

	/**
	 * Returns the value valid at the given time
	 * 
	 * @param at
	 *            given time
	 * @return valid value at given time; of type T (specific Value implementation)
	 *         or null, if no value has been specified for the given time
	 */
	T getValue(Date at);

	/**
	 * Returns the a value object representing the average value over the given time
	 * including the milliseconds of both limits. If a sub-period contains no values
	 * nothing is added to the sum, but its duration is added to the divisor.
	 * 
	 * Since a Date addresses a specific millisecond according to unix time, if from
	 * equals to, this method returns the same as getValue(from) and not null.
	 * 
	 * @param from
	 *            start of period
	 * @param to
	 *            end of period
	 * @return value object representing the average value; of type T (specific
	 *         Value implementation) or null, if no value has been specified for the
	 *         given time or if to is before from
	 */
	T getAvg(Date from, Date to);

	/**
	 * Searches for the next period starting with the iterator's current position.
	 * If the iterator's current position is empty or at the end of the last period
	 * this method returns the next existing period in the future. It returns null,
	 * if it reaches the timeline's end. If the iterator's current position is
	 * inside a valid period this method returns a period starting at the iterator's
	 * current position.
	 * 
	 * @return maximum period from the iterator's position to the next value-change
	 */
	Period<T> getNext();

	/**
	 * Sets the iterator's position to the given date.
	 * 
	 * @param at
	 *            Date, at which the iterator's position is set
	 */
	void setIterator(Date at);

	/**
	 * Returns a deep-copy of this.
	 * 
	 * @return deep-copy of this.
	 */
	Diagram<T> getCopy();
}
