package io.openems.edge.ess.core.power.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.openems.edge.ess.core.power.v2.PowerDistribution.Entry;

/**
 * Stateless solver that distributes active and reactive power across
 * {@link Entry} instances.
 *
 * <ul>
 * <li>Virtual (cluster): resolve target from bounds, distribute proportionally
 * to children
 * <li>Standalone Actual: clamp 0 to bounds
 * </ul>
 */
public final class FlatSolver {

	private FlatSolver() {
	}

	/**
	 * Abstracts field access for active vs reactive power dimensions.
	 */
	private record Dimension(//
			Function<Entry, Integer> getMin, //
			Function<Entry, Integer> getMax, //
			ObjIntConsumer<Entry.Actual> setSetPoint) {
	}

	private static final Dimension ACTIVE = new Dimension(//
			e -> e.limitActivePowerMin, //
			e -> e.limitActivePowerMax, //
			(a, v) -> a.activePowerSetPoint = v);

	private static final Dimension REACTIVE = new Dimension(//
			e -> e.limitReactivePowerMin, //
			e -> e.limitReactivePowerMax, //
			(a, v) -> a.reactivePowerSetPoint = v);

	/**
	 * Solve power distribution for all entries.
	 *
	 * @param entries the map of ESS-id to {@link Entry}
	 */
	public static void solve(ImmutableMap<String, Entry> entries) {
		var childIds = getChildIds(entries);

		// Active power pass
		solvePass(entries, childIds, ACTIVE);

		// Tighten reactive bounds by apparent power
		tightenReactiveBounds(entries);

		// Reactive power pass
		solveReactivePass(entries, childIds);
	}

	private static ImmutableSet<String> getChildIds(ImmutableMap<String, Entry> entries) {
		return entries.values().stream() //
				.filter(Entry.Virtual.class::isInstance) //
				.map(Entry.Virtual.class::cast) //
				.flatMap(v -> v.children.stream()) //
				.collect(ImmutableSet.toImmutableSet());
	}

	private static void solvePass(ImmutableMap<String, Entry> entries, ImmutableSet<String> childIds, Dimension dim) {
		entries.values().stream() //
				.filter(Entry.Virtual.class::isInstance) //
				.map(Entry.Virtual.class::cast) //
				.filter(v -> !childIds.contains(v.essId)) //
				.forEach(v -> solveVirtual(entries, v, dim));

		// for Ess which are not part of the Cluster or any child
		entries.values().stream() //
				.filter(Entry.Actual.class::isInstance) //
				.map(Entry.Actual.class::cast) //
				.filter(a -> !childIds.contains(a.essId)) //
				.forEach(a -> dim.setSetPoint().accept(a, clamp(0, dim.getMin().apply(a), dim.getMax().apply(a))));
	}

	private static void tightenReactiveBounds(ImmutableMap<String, Entry> entries) {
		entries.values().stream() //
				.filter(Entry.Actual.class::isInstance) //
				.map(Entry.Actual.class::cast) //
				.forEach(a -> {

					// S = sqrt(P² + Q²)
					// Qmax = sqrt(S² - P²)

					var qMax = (int) Math.sqrt(//
							(long) Math.pow(a.maxApparentPower, 2) - (long) Math.pow(a.activePowerSetPoint, 2)//
					);
					var effMin = a.limitReactivePowerMin != null ? a.limitReactivePowerMin : -qMax;
					var effMax = a.limitReactivePowerMax != null ? a.limitReactivePowerMax : qMax;
					a.limitReactivePowerMin = Math.max(Math.min(effMin, qMax), -qMax);
					a.limitReactivePowerMax = Math.max(Math.min(effMax, qMax), -qMax);
				});
	}

	/**
	 * Reactive pass: handles the special case where virtuals without cluster-level
	 * reactive constraints solve descendants individually.
	 * 
	 * @param entries  the full entry map
	 * @param childIds the id's of children
	 */
	private static void solveReactivePass(ImmutableMap<String, Entry> entries, ImmutableSet<String> childIds) {
		entries.values().stream() //
				.filter(Entry.Virtual.class::isInstance) //
				.map(Entry.Virtual.class::cast) //
				.filter(v -> !childIds.contains(v.essId)) //
				.forEach(v -> solveVirtualReactive(entries, v));

		// for Ess which are not part of the Cluster or any child
		entries.values().stream() //
				.filter(Entry.Actual.class::isInstance) //
				.map(Entry.Actual.class::cast) //
				.filter(a -> !childIds.contains(a.essId)) //
				.forEach(a -> a.reactivePowerSetPoint = clamp(0, a.limitReactivePowerMin, a.limitReactivePowerMax));
	}

	/**
	 * Solve a virtual entry for the given dimension (active or reactive power).
	 *
	 * @param entries the full entry map
	 * @param virtual the virtual entry to solve
	 * @param dim     the dimension to solve for
	 */
	private static void solveVirtual(ImmutableMap<String, Entry> entries, Entry.Virtual virtual, Dimension dim) {
		var actuals = collectActualDescendants(entries, virtual);
		var target = resolveTarget(dim.getMin().apply(virtual), dim.getMax().apply(virtual));

		if (target == 0 || actuals.isEmpty()) {
			for (var a : actuals) {
				dim.setSetPoint().accept(a, clamp(0, dim.getMin().apply(a), dim.getMax().apply(a)));
			}
			return;
		}

		if (actuals.size() == 1) {
			var a = actuals.get(0);
			dim.setSetPoint().accept(a, clamp(target, dim.getMin().apply(a), dim.getMax().apply(a)));
			return;
		}

		distributeProportional(actuals, target, dim);
	}

	/**
	 * Reactive-specific wrapper: if no cluster-level reactive constraint exists,
	 * solve each descendant individually; otherwise delegate to
	 * {@link #solveVirtual}.
	 *
	 * @param entries the full entry map
	 * @param virtual the virtual entry to solve
	 */
	private static void solveVirtualReactive(ImmutableMap<String, Entry> entries, Entry.Virtual virtual) {
		if (virtual.limitReactivePowerMin == null && virtual.limitReactivePowerMax == null) {
			for (var a : collectActualDescendants(entries, virtual)) {
				a.reactivePowerSetPoint = clamp(0, a.limitReactivePowerMin, a.limitReactivePowerMax);
			}
			return;
		}
		solveVirtual(entries, virtual, REACTIVE);
	}

	/**
	 * Recursively collects all {@link Entry.Actual} descendants of the given
	 * {@link Entry.Virtual}.
	 *
	 * @param entries the full entry map
	 * @param virtual the virtual entry whose actual descendants to collect
	 * @return list of all {@link Entry.Actual} descendants
	 */
	private static List<Entry.Actual> collectActualDescendants(ImmutableMap<String, Entry> entries,
			Entry.Virtual virtual) {
		var result = new ArrayList<Entry.Actual>();
		for (var childId : virtual.children) {
			var child = entries.get(childId);
			if (child instanceof Entry.Actual actual) {
				result.add(actual);
			} else if (child instanceof Entry.Virtual v) {
				result.addAll(collectActualDescendants(entries, v));
			}
		}
		return result;
	}

	/**
	 * Resolve target from nullable bounds. Returns 0 if no constraint is set.
	 *
	 * @param min the optional minimum bound (may be {@code null})
	 * @param max the optional maximum bound (may be {@code null})
	 * @return the resolved target value
	 */
	private static int resolveTarget(Integer min, Integer max) {
		if (min == null && max == null) {
			return 0;
		}
		if (min == null) {
			return max;
		}
		if (max == null) {
			return min;
		}
		return (min + max) / 2;
	}

	/**
	 * Distribute power proportionally to each child's capacity.
	 *
	 * @param children the actual entries to distribute among
	 * @param target   the total power to distribute
	 * @param dim      the dimension to distribute for
	 */
	private static void distributeProportional(List<Entry.Actual> children, int target, Dimension dim) {
		var isPositive = target >= 0;

		var totalCapacity = 0;
		for (var e : children) {
			totalCapacity += isPositive //
					? (dim.getMax().apply(e) != null ? dim.getMax().apply(e) : 0) //
					: (dim.getMin().apply(e) != null ? Math.abs(dim.getMin().apply(e)) : 0);
		}

		target = isPositive //
				? Math.min(target, totalCapacity) //
				: Math.max(target, -totalCapacity);

		var remaining = target;
		for (int i = 0; i < children.size(); i++) {
			var e = children.get(i);
			int power;

			if (i == children.size() - 1) {
				power = remaining;
			} else if (totalCapacity == 0) {
				power = target / children.size();
			} else {
				var allowed = isPositive //
						? (dim.getMax().apply(e) != null ? dim.getMax().apply(e) : 0) //
						: (dim.getMin().apply(e) != null ? Math.abs(dim.getMin().apply(e)) : 0);
				power = (int) ((long) target * allowed / totalCapacity);
			}

			power = clamp(power, dim.getMin().apply(e), dim.getMax().apply(e));
			remaining -= power;
			dim.setSetPoint().accept(e, power);
		}
	}

	/**
	 * Clamps a value within optional bounds (null-safe).
	 *
	 * @param value the value to clamp
	 * @param min   the optional minimum bound (may be {@code null})
	 * @param max   the optional maximum bound (may be {@code null})
	 * @return the clamped value
	 */
	static int clamp(int value, Integer min, Integer max) {
		if (min != null) {
			value = Math.max(min, value);
		}
		if (max != null) {
			value = Math.min(max, value);
		}
		return value;
	}
}
