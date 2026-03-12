package io.openems.edge.ess.core.power.v2;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

/**
 * Holds temporary calculations and power distribution among
 * {@link ManagedSymmetricEss}s.
 */
public class PowerDistribution {

	/**
	 * Holds {@link PowerDistribution} for one single {@link ManagedSymmetricEss}.
	 */
	public abstract static sealed class Entry {

		public final String essId;

		protected Integer limitActivePowerMin;
		protected Integer limitActivePowerMax;
		protected Integer limitReactivePowerMin;
		protected Integer limitReactivePowerMax;

		protected Entry(String essId, Integer limitActivePowerMin, Integer limitActivePowerMax) {
			this.essId = essId;
			this.limitActivePowerMin = limitActivePowerMin;
			this.limitActivePowerMax = limitActivePowerMax;
		}

		synchronized void limitActivePowerMin(Integer newMin) {
			if (newMin == null) {
				return;
			}
			if (this.limitActivePowerMin != null && newMin < this.limitActivePowerMin) {
				return;
			}
			this.limitActivePowerMin = TypeUtils.min(newMin, this.limitActivePowerMax);
		}

		synchronized void limitActivePowerMax(Integer newMax) {
			if (newMax == null) {
				return;
			}
			if (this.limitActivePowerMax != null && newMax > this.limitActivePowerMax) {
				return;
			}
			this.limitActivePowerMax = TypeUtils.max(newMax, this.limitActivePowerMin);
		}

		synchronized void limitReactivePowerMin(Integer newMin) {
			if (newMin == null) {
				return;
			}
			if (this.limitReactivePowerMin != null && newMin < this.limitReactivePowerMin) {
				return;
			}
			this.limitReactivePowerMin = TypeUtils.min(newMin, this.limitReactivePowerMax);
		}

		synchronized void limitReactivePowerMax(Integer newMax) {
			if (newMax == null) {
				return;
			}
			if (this.limitReactivePowerMax != null && newMax > this.limitReactivePowerMax) {
				return;
			}
			this.limitReactivePowerMax = TypeUtils.max(newMax, this.limitReactivePowerMin);
		}

		protected ToStringHelper toStringHelper(Class<?> clazz) {
			return MoreObjects.toStringHelper(clazz) //
					.add("essId", this.essId) //
					.add("limitActivePowerMin", this.limitActivePowerMin) //
					.add("limitActivePowerMax", this.limitActivePowerMax) //
					.add("limitReactivePowerMin", this.limitReactivePowerMin) //
					.add("limitReactivePowerMax", this.limitReactivePowerMax);
		}

		/**
		 * A {@link Virtual} represents a {@link MetaEss}.
		 */
		public static final class Virtual extends Entry {

			protected ImmutableList<String> children;

			protected Virtual(String essId, ImmutableList<String> children) {
				super(essId, null, null);
				this.children = children;
			}

			@Override
			public final String toString() {
				return super.toStringHelper(Virtual.class) //
						.toString();
			}
		}

		/**
		 * A {@link Actual} represents a physical {@link ManagedSymmetricEss}.
		 */
		public static final class Actual extends Entry {

			final int maxApparentPower;

			protected int activePowerSetPoint;
			protected int reactivePowerSetPoint;

			private Actual(String essId, Integer maxApparentPower, Integer limitActivePowerMin,
					Integer limitActivePowerMax) {
				super(essId, limitActivePowerMin, limitActivePowerMax);
				this.maxApparentPower = maxApparentPower == null ? 0 : maxApparentPower;
			}

			@Override
			public final String toString() {
				return super.toStringHelper(Entry.class) //
						.add("maxApparentPower", this.maxApparentPower) //
						.toString();
			}
		}
	}

	/**
	 * Creates {@link PowerDistribution} from a List of
	 * {@link ManagedSymmetricEss}s.
	 * 
	 * @param esss the {@link ManagedSymmetricEss}s
	 * @return the {@link PowerDistribution}
	 */
	public static synchronized PowerDistribution from(List<ManagedSymmetricEss> esss) {
		var entries = esss.stream() //
				.collect(toImmutableMap(//
						ManagedSymmetricEss::id, //
						ess -> {
							if (ess instanceof MetaEss me) {
								return new Entry.Virtual(ess.id(), //
										Arrays.stream(me.getEssIds()) //
												.collect(toImmutableList()));
							} else {
								return new Entry.Actual(ess.id(), //
										ess.getMaxApparentPower().orElse(0), //
										ess.getAllowedChargePower().orElse(0), //
										ess.getAllowedDischargePower().orElse(0));
							}
						}));

		return new PowerDistribution(entries);
	}

	final ImmutableMap<String, Entry> entries;

	private PowerDistribution(ImmutableMap<String, Entry> entries) {
		this.entries = entries;
	}

	public void setEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMax(value);
		entry.limitActivePowerMin(value);
	}

	public void setLessOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMax(value);
	}

	public void setGreaterOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitActivePowerMin(value);
	}

	public void setReactiveEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMax(value);
		entry.limitReactivePowerMin(value);
	}

	public void setReactiveLessOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMax(value);
	}

	public void setReactiveGreaterOrEquals(String essId, int value) {
		var entry = this.entries.get(essId);
		if (entry == null) {
			return;
		}
		entry.limitReactivePowerMin(value);
	}

	/**
	 * Solve power distribution.
	 *
	 * <ul>
	 * <li>Virtual (cluster): resolve target from bounds, distribute proportionally
	 * to children
	 * <li>Standalone Actual: midpoint of bounds
	 * </ul>
	 */
	public void solve() {
		// TODO
	}

	/**
	 * Applies this entry's configuration or values to a list of ESS instances.
	 *
	 * @param esss the list of {@link ManagedSymmetricEss} instances to which this
	 *             entry should be applied; must not be {@code null} but may be
	 *             empty
	 */
	public void applyToEsss(List<ManagedSymmetricEss> esss) {
		for (var ess : esss) {
			var entry = this.entries.get(ess.id());
			if (!(entry instanceof Entry.Actual ea)) {
				continue; // skip MetaEss / Virtual entries
			}

			var activePowerSetPoint = ea.activePowerSetPoint;
			var reactivePowerSetPoint = ea.reactivePowerSetPoint;

			ess._setDebugSetActivePower(activePowerSetPoint);
			ess._setDebugSetReactivePower(reactivePowerSetPoint);

			try {
				ess.applyPower(activePowerSetPoint, reactivePowerSetPoint);
				ess._setApplyPowerFailed(false);

			} catch (Exception e) {
				ess._setApplyPowerFailed(true);
				e.printStackTrace();
			}
		}
	}

	@Override
	public final String toString() {
		return toStringHelper(PowerDistribution.class) //
				.add("entries", "\n" + this.entries.values().stream() //
						.map(Entry::toString) //
						.collect(joining("\n"))) //
				.toString();
	}
}
