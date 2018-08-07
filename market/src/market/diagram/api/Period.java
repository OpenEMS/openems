package market.diagram.api;

import java.io.Serializable;
import java.util.Date;

/**
 * A section of time containing only ONE constant value.
 * 
 * @author FENECON GmbH
 *
 * @param <T>
 *            must implement this package's Value interface.
 */
public interface Period<T extends Value<T>> extends Serializable {

	/**
	 * Returns the period's starting point
	 * 
	 * @return beginning of period
	 */
	Date getStart();

	/**
	 * Returns the period's ending point
	 * 
	 * @return ending of period
	 */
	Date getEnd();

	/**
	 * Returns the period's value
	 * 
	 * @return value which is valid for this period; of type T (specific Value
	 *         implementation)
	 */
	T getValue();
}
