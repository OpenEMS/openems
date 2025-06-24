package io.openems.common.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ComparatorUtils {

	/**
	 * Creates a {@link Comparator} which sorts items based on the order in the
	 * provided list.
	 * 
	 * @param <C>      the type of the items
	 * @param order    the first order of the items
	 * @param obtainId the function to get the id from the object
	 * @return the {@link Comparator}
	 */
	public static <C> Comparator<C> comparatorIdList(//
			final List<String> order, //
			final Function<C, String> obtainId //
	) {
		Objects.requireNonNull(order);
		Objects.requireNonNull(obtainId);

		return (o1, o2) -> {
			final var ido1 = obtainId.apply(o1);
			final var ido2 = obtainId.apply(o2);
			if (ido1 == null || ido2 == null) {
				if (ido1 != null) {
					return -1;
				}
				if (ido2 != null) {
					return 1;
				}
				// None is configured; ids are not available
				return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
			}

			var idxO1 = order.indexOf(ido1);
			var idxO2 = order.indexOf(ido2);
			if (idxO1 != -1 && idxO2 != -1) {
				// Both services are mentioned in config: sort as defined
				return Integer.compare(idxO1, idxO2);
			}
			if (idxO1 != -1) {
				// Only t1 is configured
				return -1;
			}
			if (idxO2 != -1) {
				// Only t2 is configured
				return 1;
			}
			// None is configured; ids are available
			var result = ido1.compareTo(ido2);
			if (result != 0) {
				// ids are different
				return result;
			}
			return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
		};
	}

	private ComparatorUtils() {
	}

}
