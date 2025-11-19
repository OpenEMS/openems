package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PredefinedOrder<T> implements Comparator<T> {

	private static final int[] EMPTY_INT_ARRAY = new int[0];

	private sealed interface PositionReturnType {

		static PositionReturnType right(int... index) {
			return new PositionReturnType.Right(index);
		}

		static PositionReturnType remove() {
			return PositionReturnType.Remove.INSTANCE;
		}

		static PositionReturnType next() {
			return PositionReturnType.Next.INSTANCE;
		}

		record Right(int[] index) implements PositionReturnType {
		}

		final class Remove implements PositionReturnType {
			public static final PositionReturnType.Remove INSTANCE = new PositionReturnType.Remove();
		}

		final class Next implements PositionReturnType {
			public static final PositionReturnType.Next INSTANCE = new PositionReturnType.Next();
		}

	}

	private final List<Function<T, PositionReturnType>> predicates = new LinkedList<>();

	/**
	 * Adds a filter to the order queue which sorts out every
	 * {@link SchedulerByCentralOrderConfiguration.SchedulerComponent} which does
	 * not match the {@link Predicate}.
	 *
	 * @param matcher the predicate to determine if the current
	 *                {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 *                should be filtered out or not; true for continuing to next
	 *                check; false for removing the
	 *                {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 * @return this
	 */
	public PredefinedOrder<T> filterBy(Predicate<T> matcher) {
		return this.thenByFunction(t -> matcher.test(t) ? PositionReturnType.next() : PositionReturnType.remove());
	}

	private PredefinedOrder<T> thenByFunction(Function<T, PositionReturnType> matcher) {
		this.predicates.add(matcher);
		return this;
	}

	/**
	 * Adds a matching function if the current
	 * {@link SchedulerByCentralOrderConfiguration.SchedulerComponent} is at the
	 * right position.
	 *
	 * @param matcher the predicate if the current
	 *                {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 *                is at the right position; true if the
	 *                {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 *                is at the right position; false if the
	 *                {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 *                should continue to the next function
	 * @return this
	 */
	public PredefinedOrder<T> thenBy(Predicate<T> matcher) {
		return this.thenByFunction(t -> matcher.test(t) ? PositionReturnType.right(1) : PositionReturnType.next());
	}

	/**
	 * Adds a Sub-Order at the current position.
	 *
	 * @param order the Sub-Order
	 * @return this
	 */
	public PredefinedOrder<T> thenBy(PredefinedOrder<T> order) {
		return this.thenByFunction(t -> {
			final var result = order.indexOfMatch(t);
			if (result.length == 0) {
				return PositionReturnType.next();
			}
			return PositionReturnType.right(result);
		});
	}

	/**
	 * Adds the rest of the
	 * {@link SchedulerByCentralOrderConfiguration.SchedulerComponent} at this
	 * position which got not filtered out before.
	 *
	 * @return this
	 */
	public PredefinedOrder<T> rest() {
		return this.thenBy(t -> true);
	}

	@Override
	public int compare(T o1, T o2) {
		final var index1 = this.indexOfMatch(o1);
		final var index2 = this.indexOfMatch(o2);

		for (int i = 0; i < Math.max(index1.length, index2.length); i++) {
			if (i >= index1.length) {
				return 1;
			}
			if (i >= index2.length) {
				return -1;
			}
			final var i1Value = index1[i];
			final var i2Value = index2[i];
			if (i1Value < i2Value) {
				return -1;
			}
			if (i1Value > i2Value) {
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Checks if the given
	 * {@link SchedulerByCentralOrderConfiguration.SchedulerComponent} is handled by
	 * this
	 * {@link SchedulerByCentralOrderAggregateTaskImpl.SchedulerOrderDefinition}.
	 *
	 * @param o the {@link SchedulerByCentralOrderConfiguration.SchedulerComponent}
	 * @return true if the
	 *         {@link SchedulerByCentralOrderConfiguration.SchedulerComponent} is
	 *         handled by this order
	 */
	public boolean contains(T o) {
		return this.indexOfMatch(o).length != 0;
	}

	private final int[] indexOfMatch(T o) {
		final var iterator = this.predicates.listIterator();
		while (iterator.hasNext()) {
			final var index = iterator.nextIndex();
			final var predicate = iterator.next();
			final PositionReturnType result = predicate.apply(o);

			switch (result) {
			case PositionReturnType.Next next -> doNothing();

			case PositionReturnType.Remove remove -> {
				return EMPTY_INT_ARRAY;
			}
			case PositionReturnType.Right right -> {
				final var array = new int[right.index.length + 1];
				array[0] = index;
				System.arraycopy(right.index, 0, array, 1, right.index.length);
				return array;
			}
			}
		}
		return EMPTY_INT_ARRAY;
	}

}
