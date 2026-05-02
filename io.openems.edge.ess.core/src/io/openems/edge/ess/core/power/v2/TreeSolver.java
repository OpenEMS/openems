package io.openems.edge.ess.core.power.v2;

import static io.openems.common.utils.IntUtils.fitWithin;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import io.openems.edge.ess.core.power.v2.PowerDistribution.Entry;

/**
 * Stateless tree solver for {@link PowerDistribution}.
 *
 * <p>
 * Solves power distribution using a two-phase recursive approach:
 *
 * <ul>
 * <li><b>Bottom-up</b> (already computed by {@link Entry#ownActiveMin()} /
 * {@link Entry#ownActiveMax()}): each Virtual aggregates its children's
 * achievable bounds. Infeasibility is detected before distribution begins.
 * <li><b>Top-down</b>: starting from each root Virtual, distribute a target to
 * direct children only, then recurse into sub-clusters. Each level solves
 * independently.
 * </ul>
 *
 * <p>
 * Within each level, distribution is two-pass:
 * <ol>
 * <li>Pin fixed entries (ownMin == ownMax) — set their setpoint and subtract
 * from target.
 * <li>Distribute remainder proportionally among free entries.
 * </ol>
 *
 * <p>
 * Performance: single DFS traversal — each node visited exactly once → O(n).
 */
public final class TreeSolver {

	private TreeSolver() {
	}

	/**
	 * Solves active and reactive power distribution for all entries.
	 *
	 * @param entries the full entry map
	 */
	public static void solve(ImmutableMap<String, Entry> entries) {

		// Active Power pass — root Virtuals
		for (var entry : entries.values()) {
			if (entry instanceof Entry.Virtual v && v.getParent() == null) {
				solveActiveNode(v, resolveTarget(v.limitActivePowerMin, v.limitActivePowerMax));
			}
		}

		// Active pass — standalone Actuals (not inside any cluster)
		for (var entry : entries.values()) {
			if (entry instanceof Entry.Actual a && a.getParent() == null) {
				a.activePowerSetPoint = fitWithin(a.ownActiveMin(), a.ownActiveMax(), 0);
			}
		}

		// Tighten reactive power bounds using apparent power: Qmax = sqrt(S² - P²)
		tightenReactiveBounds(entries);

		// Reactive power pass — root Virtuals
		for (var entry : entries.values()) {
			if (entry instanceof Entry.Virtual v && v.getParent() == null) {
				solveReactiveNode(v, resolveTarget(v.limitReactivePowerMin, v.limitReactivePowerMax));
			}
		}

		// Reactive power pass — standalone Actuals
		for (var entry : entries.values()) {
			if (entry instanceof Entry.Actual a && a.getParent() == null) {
				a.reactivePowerSetPoint = fitWithin(a.ownReactiveMin(), a.ownReactiveMax(), 0);
			}
		}
	}

	/**
	 * Solves active power for a Virtual node top-down.
	 *
	 * <p>
	 * The raw target (from explicit cluster constraint) is clamped to
	 * {@code [ownActiveMin..ownActiveMax]} before distribution. Since
	 * {@link Entry.Virtual#ownActiveMin()} already aggregates children's
	 * constraints bottom-up, infeasible targets are corrected here naturally.
	 * 
	 * @param virtual   the virtual
	 * @param rawTarget the rawTarget
	 */
	private static void solveActiveNode(Entry.Virtual virtual, int rawTarget) {
		// No constraint on a root Virtual → solve each child independently.
		// Only applies to roots (parent==null) — sub-cluster targets passed from
		// distribution must be honored even when the sub-cluster has no own constraint.
		if (virtual.getParent() == null //
				&& virtual.limitActivePowerMin == null //
				&& virtual.limitActivePowerMax == null) {
			for (var child : virtual.children) {
				if (child instanceof Entry.Actual a) {
					a.activePowerSetPoint = fitWithin(a.ownActiveMin(), a.ownActiveMax(), 0);
				} else if (child instanceof Entry.Virtual v) {
					solveActiveNode(v, resolveTarget(v.limitActivePowerMin, v.limitActivePowerMax));
				}
			}
			return;
		}
		var min = virtual.ownActiveMin();
		var max = virtual.ownActiveMax();
		if (min > max) {
			// forced constraints on children conflict with cluster target.
			// Best effort: most achievable bound in the target direction.
			distributeToChildren(virtual.children, rawTarget >= 0 ? max : min, true);
			return;
		}
		distributeToChildren(virtual.children, fitWithin(min, max, rawTarget), true);
	}

	/**
	 * Solves reactive power for a Virtual node top-down.
	 *
	 * <p>
	 * If the cluster has no reactive constraint, each child is solved independently
	 * (no cluster-level distribution).
	 * 
	 * @param virtual   the virtual
	 * @param rawTarget the rawTarget
	 */
	private static void solveReactiveNode(Entry.Virtual virtual, int rawTarget) {
		if (virtual.limitReactivePowerMin == null && virtual.limitReactivePowerMax == null) {
			for (var child : virtual.children) {
				if (child instanceof Entry.Actual a) {
					a.reactivePowerSetPoint = fitWithin(a.ownReactiveMin(), a.ownReactiveMax(), 0);
				} else if (child instanceof Entry.Virtual v) {
					solveReactiveNode(v, resolveTarget(v.limitReactivePowerMin, v.limitReactivePowerMax));
				}
			}
			return;
		}
		var min = virtual.ownReactiveMin();
		var max = virtual.ownReactiveMax();
		if (min > max) {
			// forced constraints on children conflict with cluster target.
			// Best effort: most achievable bound in the target direction.
			distributeToChildren(virtual.children, rawTarget >= 0 ? max : min, false);
			return;
		}
		distributeToChildren(virtual.children, fitWithin(min, max, rawTarget), false);
	}

	/**
	 * Distributes a target to direct children using a two-pass approach:.
	 *
	 * <ol>
	 * <li>Pin fixed entries (ownMin == ownMax), subtract their contribution.
	 * <li>Distribute remainder proportionally among free entries.
	 * </ol>
	 *
	 * <p>
	 * When a child is a Virtual, {@link #applySetpoint} recurses into it — so
	 * nested clusters are solved naturally without any special casing.
	 * 
	 * @param children the children
	 * @param target   the target
	 * @param isActive boolean true or false
	 */
	private static void distributeToChildren(List<Entry> children, int target, boolean isActive) {

		// Pass 1: identify forced entries — either pinned (ownMin==ownMax) or whose
		// bounds are entirely opposite to the target direction:
		// charge target (isPositive=false) + ownMin > 0 → must discharge regardless
		// discharge target (isPositive=true) + ownMax < 0 → must charge regardless
		var isPositive = target >= 0;
		var remaining = target;
		var free = new ArrayList<Entry>(children.size());

		for (var child : children) {
			var min = isActive ? child.ownActiveMin() : child.ownReactiveMin();
			var max = isActive ? child.ownActiveMax() : child.ownReactiveMax();
			var isForced = (min == max) //
					|| (!isPositive && min > 0) // charge target, child must discharge
					|| (isPositive && max < 0); // discharge target, child must charge
			if (isForced) {
				var forced = isPositive ? max : min;
				applySetpoint(child, forced, isActive);
				remaining -= forced;
			} else {
				free.add(child);
			}
		}

		// Pass 2: proportional among free entries
		distributeProportional(free, remaining, isActive);
	}

	/**
	 * Applies a setpoint to an entry.
	 *
	 * <ul>
	 * <li>Actual: writes the setpoint directly (clamped to own bounds).
	 * <li>Virtual: recurses into {@link #solveActiveNode} /
	 * {@link #solveReactiveNode} with the given value as sub-target.
	 * </ul>
	 * 
	 * @param entry    the Entry
	 * @param value    the int value
	 * @param isActive boolean true or false
	 */
	private static void applySetpoint(Entry entry, int value, boolean isActive) {
		if (entry instanceof Entry.Actual a) {
			var min = isActive ? a.ownActiveMin() : a.ownReactiveMin();
			var max = isActive ? a.ownActiveMax() : a.ownReactiveMax();
			if (isActive) {
				a.activePowerSetPoint = fitWithin(min, max, value);
			} else {
				a.reactivePowerSetPoint = fitWithin(min, max, value);
			}
		} else if (entry instanceof Entry.Virtual v) {
			if (isActive) {
				solveActiveNode(v, value);
			} else {
				solveReactiveNode(v, value);
			}
		}
	}

	/**
	 * Distributes target proportionally among free (non-pinned) entries.
	 *
	 * <p>
	 * Capacity weight = charge/discharge capacity in the target direction. The last
	 * entry absorbs any integer rounding remainder.
	 * 
	 * @param free     the list Entry
	 * @param target   the int target
	 * @param isActive boolean true or false
	 */
	private static void distributeProportional(List<Entry> free, int target, boolean isActive) {
		if (free.isEmpty()) {
			return;
		}

		var isPositive = target >= 0;
		var totalCapacity = 0;
		for (var e : free) {
			totalCapacity += isPositive //
					? Math.max(isActive ? e.ownActiveMax() : e.ownReactiveMax(), 0)
					: Math.abs(Math.min(isActive ? e.ownActiveMin() : e.ownReactiveMin(), 0));
		}

		// Cap target to total available capacity
		target = isPositive //
				? Math.min(target, totalCapacity) //
				: Math.max(target, -totalCapacity);

		var remaining = target;
		for (int i = 0; i < free.size(); i++) {
			var e = free.get(i);
			int power;

			if (i == free.size() - 1) {
				power = remaining; // last entry absorbs rounding remainder
			} else if (totalCapacity == 0) {
				power = target / free.size();
			} else {
				var allowed = isPositive //
						? Math.max(isActive ? e.ownActiveMax() : e.ownReactiveMax(), 0)
						: Math.abs(Math.min(isActive ? e.ownActiveMin() : e.ownReactiveMin(), 0));
				power = (int) ((long) target * allowed / totalCapacity);
			}

			var min = isActive ? e.ownActiveMin() : e.ownReactiveMin();
			var max = isActive ? e.ownActiveMax() : e.ownReactiveMax();
			power = fitWithin(min, max, power);
			remaining -= power;
			applySetpoint(e, power, isActive);
		}
	}

	private static void tightenReactiveBounds(ImmutableMap<String, Entry> entries) {
		for (var entry : entries.values()) {
			if (!(entry instanceof Entry.Actual a)) {
				continue;
			}
			var qMax = (int) Math.sqrt(//
					(long) Math.pow(a.maxApparentPower, 2) - (long) Math.pow(a.activePowerSetPoint, 2));
			var effMin = a.limitReactivePowerMin != null ? a.limitReactivePowerMin : -qMax;
			var effMax = a.limitReactivePowerMax != null ? a.limitReactivePowerMax : qMax;
			a.limitReactivePowerMin = Math.max(Math.min(effMin, qMax), -qMax);
			a.limitReactivePowerMax = Math.max(Math.min(effMax, qMax), -qMax);
		}
	}

	/**
	 * Resolves a target from nullable bounds. Returns 0 if no constraint is set.
	 * 
	 * @param min the minimum
	 * @param max the maximum
	 * @return resolve target
	 */
	static int resolveTarget(Integer min, Integer max) {
		if (min == null && max == null) {
			return 0;
		}
		// Clamp 0 to the allowed range — limits constrain the range but don't mandate
		// a specific target. Only an EQUALS constraint (min==max) forces a hard target.
		var target = 0;
		if (min != null) {
			target = Math.max(target, min);
		}
		if (max != null) {
			target = Math.min(target, max);
		}
		return target;
	}

}
