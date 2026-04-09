package io.openems.edge.ess.core.power.v2;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.common.utils.IntUtils.maxInteger;
import static io.openems.common.utils.IntUtils.minInt;
import static io.openems.common.utils.IntUtils.minInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.v2.PowerDistribution.Entry.Actual;
import io.openems.edge.ess.core.power.v2.PowerDistribution.Entry.Virtual;

/**
 * Composite-based power distribution. Each {@link Entry} owns its own min/max
 * logic:
 *
 * <ul>
 * <li>{@link Actual}: returns its own constraint bounds.
 * <li>{@link Virtual}: aggregates children's bounds, then clamps to its own
 * constraint — so infeasible cluster targets are corrected before distribution.
 * </ul>
 *
 * <p>
 * {@link Entry#getActiveMax()} / {@link Entry#getActiveMin()} additionally
 * apply parent headroom, giving a correct answer for {@code getPowerExtrema()}
 * without any {@code instanceof} checks at the call site.
 */
public class PowerDistribution {

	/**
	 * Holds power distribution state for one {@link ManagedSymmetricEss}.
	 */
	public abstract static sealed class Entry {

		public final String essId;

		protected Integer limitActivePowerMin;
		protected Integer limitActivePowerMax;
		protected Integer limitReactivePowerMin;
		protected Integer limitReactivePowerMax;

		private Entry parent; // null for top-level entries

		protected Entry getParent() {
			return this.parent;
		}

		private void setParent(Entry parent) {
			this.parent = parent;
		}

		protected Entry(String essId, Integer limitActivePowerMin, Integer limitActivePowerMax) {
			this.essId = essId;
			this.limitActivePowerMin = limitActivePowerMin;
			this.limitActivePowerMax = limitActivePowerMax;
		}

		private synchronized void limitActivePowerMin(Integer newMin) {
			if (newMin == null) {
				return;
			}
			if (this.limitActivePowerMin != null && newMin < this.limitActivePowerMin) {
				return;
			}
			this.limitActivePowerMin = minInteger(newMin, this.limitActivePowerMax);
		}

		private synchronized void limitActivePowerMax(Integer newMax) {
			if (newMax == null) {
				return;
			}
			if (this.limitActivePowerMax != null && newMax > this.limitActivePowerMax) {
				return;
			}
			this.limitActivePowerMax = maxInteger(newMax, this.limitActivePowerMin);
		}

		private synchronized void limitReactivePowerMin(Integer newMin) {
			if (newMin == null) {
				return;
			}
			if (this.limitReactivePowerMin != null && newMin < this.limitReactivePowerMin) {
				return;
			}
			this.limitReactivePowerMin = minInteger(newMin, this.limitReactivePowerMax);
		}

		private synchronized void limitReactivePowerMax(Integer newMax) {
			if (newMax == null) {
				return;
			}
			if (this.limitReactivePowerMax != null && newMax > this.limitReactivePowerMax) {
				return;
			}
			this.limitReactivePowerMax = maxInteger(newMax, this.limitReactivePowerMin);
		}

		/**
		 * Own active max without parent headroom.
		 *
		 * @return the own active max in W
		 */
		public abstract int ownActiveMax();

		/**
		 * Own active min without parent headroom.
		 *
		 * @return the own active min in W
		 */
		public abstract int ownActiveMin();

		/**
		 * Own reactive max without parent headroom.
		 *
		 * @return the own reactive max in W
		 */
		public abstract int ownReactiveMax();

		/**
		 * Own reactive min without parent headroom.
		 *
		 * @return the own reactive min in W
		 */
		public abstract int ownReactiveMin();

		protected abstract String toString(String indent);

		/**
		 * Active max accounting for parent cluster constraint and sibling
		 * contributions.
		 *
		 * @return the active max in W
		 */
		public int getActiveMax() {
			return this.applyParentHeadRoom(this.ownActiveMax(), true, true);
		}

		/**
		 * Active min accounting for parent cluster constraint and sibling
		 * contributions.
		 *
		 * @return the active min in W
		 */
		public int getActiveMin() {
			return this.applyParentHeadRoom(this.ownActiveMin(), false, true);
		}

		/**
		 * Reactive max accounting for parent cluster constraint and sibling
		 * contributions.
		 *
		 * @return the reactive max in W
		 */
		public int getReactiveMax() {
			return this.applyParentHeadRoom(this.ownReactiveMax(), true, false);
		}

		/**
		 * Reactive min accounting for parent cluster constraint and sibling
		 * contributions.
		 *
		 * @return the reactive min in W
		 */
		public int getReactiveMin() {
			return this.applyParentHeadRoom(this.ownReactiveMin(), false, false);
		}

		/**
		 * Clamps own bound by how much headroom the parent constraint leaves after
		 * siblings take their minimum share.
		 *
		 * @param own      the own bound value
		 * @param isMax    true for max bound, false for min bound
		 * @param isActive true for active power, false for reactive power
		 * @return the clamped bound
		 */
		private int applyParentHeadRoom(int own, boolean isMax, boolean isActive) {
			if (this.parent == null) {
				return own;
			}
			var parentConstraint = isMax //
					? (isActive ? this.parent.limitActivePowerMax : this.parent.limitReactivePowerMax)
					: (isActive ? this.parent.limitActivePowerMin : this.parent.limitReactivePowerMin);
			if (parentConstraint == null) {
				return own;
			}
			var parentVirtual = (Virtual) this.parent;
			var siblingContrib = parentVirtual.children.stream() //
					.filter(c -> c != this) //
					.mapToInt(c -> isMax //
							? (isActive ? c.ownActiveMin() : c.ownReactiveMin())
							: (isActive ? c.ownActiveMax() : c.ownReactiveMax())) //
					.sum();
			var headroom = isMax //
					? Math.min(own, parentConstraint - siblingContrib)
					: Math.max(own, parentConstraint - siblingContrib);
			// Clamp back to own bounds — headroom can exceed hardware limits in infeasible
			// scenarios (e.g. siblings force-discharge while cluster wants to charge)
			var ownMin = isActive ? this.ownActiveMin() : this.ownReactiveMin();
			var ownMax = isActive ? this.ownActiveMax() : this.ownReactiveMax();
			return isMax //
					? Math.max(headroom, ownMin)
					: Math.min(headroom, ownMax);
		}

		/**
		 * A {@link Virtual} represents a {@link MetaEss} (cluster). Its own bounds are
		 * the aggregate of its children's bounds, clamped to any explicit cluster
		 * constraint. This means infeasible cluster targets are automatically corrected
		 * before distribution.
		 */
		public static final class Virtual extends Entry {

			protected final List<Entry> children = new ArrayList<>();

			private final ImmutableList<String> childIds; // for two-pass construction in from()

			private Virtual(String essId, ImmutableList<String> childIds) {
				super(essId, null, null);
				this.childIds = childIds;
			}

			@Override
			public String toString(String indent) {
				var sb = new StringBuilder();
				sb.append(indent) //
						.append(this.essId).append(" [Virtual]") //
						.append(" P=[").append(this.limitActivePowerMin) //
						.append("..").append(this.limitActivePowerMax).append("]") //
						.append(" own=[").append(this.ownActiveMin()) //
						.append("..").append(this.ownActiveMax()).append("]") //
						.append("\n");
				for (var child : this.children) {
					sb.append(child.toString(indent + "  "));
				}
				return sb.toString();
			}

			@Override
			public int ownActiveMax() {
				var childSum = this.children.stream() //
						.mapToInt(Entry::ownActiveMax) //
						.sum();
				return minInt(childSum, this.limitActivePowerMax);
			}

			@Override
			public int ownActiveMin() {
				var childSum = this.children.stream() //
						.mapToInt(Entry::ownActiveMin) //
						.sum();
				return maxInt(childSum, this.limitActivePowerMin);
			}

			@Override
			public int ownReactiveMax() {
				var childSum = this.children.stream() //
						.mapToInt(Entry::ownReactiveMax) //
						.sum();
				return minInt(childSum, this.limitReactivePowerMax);
			}

			@Override
			public int ownReactiveMin() {
				var childSum = this.children.stream() //
						.mapToInt(Entry::ownReactiveMin) //
						.sum();
				return maxInt(childSum, this.limitReactivePowerMin);
			}
		}

		/**
		 * An {@link Actual} represents a physical {@link ManagedSymmetricEss}.
		 */
		public static final class Actual extends Entry {

			protected final int maxApparentPower;

			protected int activePowerSetPoint;
			protected int reactivePowerSetPoint;

			private Actual(String essId, int maxApparentPower, Integer limitActivePowerMin,
					Integer limitActivePowerMax) {
				super(essId, limitActivePowerMin, limitActivePowerMax);
				this.maxApparentPower = maxApparentPower;
			}

			@Override
			public String toString(String indent) {
				return new StringBuilder() //
						.append(indent) //
						.append(this.essId).append(" [Actual]") //
						.append(" P=[").append(this.limitActivePowerMin) //
						.append("..").append(this.limitActivePowerMax).append("]") //
						.append(" own=[").append(this.ownActiveMin()) //
						.append("..").append(this.ownActiveMax()).append("]") //
						.append(" get=[").append(this.getActiveMin()) //
						.append("..").append(this.getActiveMax()).append("]") //
						.append(" -> ").append(this.activePowerSetPoint).append("W") //
						.append("\n") //
						.toString();
			}

			@Override
			public int ownActiveMax() {
				return this.limitActivePowerMax != null ? this.limitActivePowerMax : 0;
			}

			@Override
			public int ownActiveMin() {
				return this.limitActivePowerMin != null ? this.limitActivePowerMin : 0;
			}

			@Override
			public int ownReactiveMax() {
				return this.limitReactivePowerMax != null ? this.limitReactivePowerMax : 0;
			}

			@Override
			public int ownReactiveMin() {
				return this.limitReactivePowerMin != null ? this.limitReactivePowerMin : 0;
			}
		}
	}

	/**
	 * Creates a {@link PowerDistribution} from a list of
	 * {@link ManagedSymmetricEss}s. Two-pass construction:
	 * <ol>
	 * <li>First pass: create all entries.
	 * <li>Second pass: wire Virtual children + parent back-refs.
	 * </ol>
	 *
	 * @param esss the list of {@link ManagedSymmetricEss}
	 * @return the {@link PowerDistribution}
	 */
	public static PowerDistribution from(List<ManagedSymmetricEss> esss) {
		var map = new LinkedHashMap<String, Entry>();

		// First pass: create all entries
		for (var ess : esss) {
			Entry entry;
			if (ess instanceof MetaEss me) {
				entry = new Entry.Virtual(ess.id(), //
						Arrays.stream(me.getEssIds()).collect(toImmutableList()));
			} else {
				entry = new Entry.Actual(ess.id(), //
						ess.getMaxApparentPower().orElse(0), //
						ess.getAllowedChargePower().orElse(0), //
						ess.getAllowedDischargePower().orElse(0));
			}
			map.put(ess.id(), entry);
		}

		// Second pass: wire Virtual children + parent refs
		for (var entry : map.values()) {
			if (entry instanceof Entry.Virtual v) {
				for (var childId : v.childIds) {
					var child = map.get(childId);
					if (child != null) {
						v.children.add(child);
						child.setParent(v);
					}
				}
			}
		}

		return new PowerDistribution(ImmutableMap.copyOf(map));
	}

	private final ImmutableMap<String, Entry> entries;

	private PowerDistribution(ImmutableMap<String, Entry> entries) {
		this.entries = entries;
	}

	@VisibleForTesting
	ImmutableMap<String, Entry> getEntries() {
		return this.entries;
	}

	/**
	 * Sets an active power EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the target value in W
	 */
	public void setEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMax(value);
		entry.limitActivePowerMin(value);
	}

	/**
	 * Sets an active power LESS_OR_EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the upper bound in W
	 */
	public void setLessOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMax(value);
	}

	/**
	 * Sets an active power GREATER_OR_EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the lower bound in W
	 */
	public void setGreaterOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMin(value);
	}

	/**
	 * Sets a reactive power EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the target value in VAr
	 */
	public void setReactiveEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMax(value);
		entry.limitReactivePowerMin(value);
	}

	/**
	 * Sets a reactive power LESS_OR_EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the upper bound in VAr
	 */
	public void setReactiveLessOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMax(value);
	}

	/**
	 * Sets a reactive power GREATER_OR_EQUALS constraint on the given ESS.
	 *
	 * @param essId the ESS id
	 * @param value the lower bound in VAr
	 */
	public void setReactiveGreaterOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMin(value);
	}

	/**
	 * Solves power distribution for active and reactive power.
	 *
	 * <p>
	 * For each Virtual (cluster): the target is resolved from the explicit cluster
	 * constraint, then clamped to the children's aggregate achievable range
	 * ({@link Virtual#ownActiveMin()} / {@link Virtual#ownActiveMax()}). This
	 * ensures infeasible cluster constraints are handled gracefully before
	 * distribution begins.
	 *
	 * <p>
	 * For standalone Actual entries: clamp 0 to their own bounds.
	 */
	public void solve() {
		TreeSolver.solve(this.entries);
	}

	/**
	 * Returns active power extrema for the given ESS. For a child ESS inside a
	 * cluster, parent headroom and sibling contributions are automatically
	 * accounted for — no external traversal needed.
	 *
	 * @param essId the ESS ID
	 * @param goal  {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE}
	 * @return the extrema value; 0 if the ESS is unknown
	 */
	public int getActivePowerExtrema(String essId, GoalType goal) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return 0;
		}
		return goal == GoalType.MAXIMIZE //
				? entry.getActiveMax() //
				: entry.getActiveMin();
	}

	/**
	 * Returns reactive power extrema for the given ESS.
	 *
	 * @param essId the ESS ID
	 * @param goal  {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE}
	 * @return the extrema value; 0 if the ESS is unknown
	 */
	public int getReactivePowerExtrema(String essId, GoalType goal) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return 0;
		}
		return goal == GoalType.MAXIMIZE //
				? entry.getReactiveMax() //
				: entry.getReactiveMin();
	}

	/**
	 * Applies solved setpoints to the ESS instances.
	 *
	 * @param esss the list of {@link ManagedSymmetricEss}
	 */
	public void applyToEsss(List<ManagedSymmetricEss> esss) {
		for (var ess : esss) {
			var entry = this.entries.get(ess.id());
			if (!(entry instanceof Entry.Actual ea)) {
				continue;
			}

			ess._setDebugSetActivePower(ea.activePowerSetPoint);
			ess._setDebugSetReactivePower(ea.reactivePowerSetPoint);

			try {
				ess.applyPower(ea.activePowerSetPoint, ea.reactivePowerSetPoint);
				ess._setApplyPowerFailed(false);
			} catch (Exception e) {
				ess._setApplyPowerFailed(true);
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder("PowerDistribution:\n");
		for (var entry : this.entries.values()) {
			if (entry.getParent() == null) {
				sb.append(entry.toString(""));
			}
		}
		return sb.toString();
	}

}
