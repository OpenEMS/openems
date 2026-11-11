package io.openems.edge.controller.evse.cluster.powerdistribute;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;

import io.openems.edge.controller.evse.single.ControllerEvseSingle;

/**
 * Holds temporary calculations and power distribution among
 * {@link ControllerEvseSingle}s.
 */
public class PowerDistributionEntries {
	private final ImmutableList<PowerDistributionEntry> allEntries;
	private final ImmutableList<PowerDistributionEntry> activeEntries;

	public PowerDistributionEntries(ImmutableList<PowerDistributionEntry> entries) {
		this.allEntries = entries;

		var activeEntriesBuilder = ImmutableList.<PowerDistributionEntry>builderWithExpectedSize(entries.size());
		for (var entry : this.allEntries) {
			if (entry.status == PowerDistributionEntry.Status.ACTIVE) {
				activeEntriesBuilder.add(entry);
			}
		}
		this.activeEntries = activeEntriesBuilder.build();
	}

	/**
	 * Returns the cumulated active power of all active entries.
	 * 
	 * @return cumulated power
	 */
	public int getTotalActivePower() {
		int totalActivePower = 0;
		for (var entry : this.getActiveEntries()) {
			if (entry.activePower != null) {
				totalActivePower += entry.activePower;
			}
		}

		return totalActivePower;
	}

	public ImmutableList<PowerDistributionEntry> getAllEntries() {
		return this.allEntries;
	}

	public ImmutableList<PowerDistributionEntry> getActiveEntries() {
		return this.activeEntries;
	}

	/**
	 * Checks which entries have a surplus greater than zero and returns them as a
	 * list.
	 *
	 * @return List
	 */
	public ImmutableList<PowerDistributionEntry> computeEntriesWithSurplusGreaterZero() {
		var builder = ImmutableList.<PowerDistributionEntry>builderWithExpectedSize(this.activeEntries.size());
		for (var entry : this.getActiveEntries()) {
			if (entry.hasSurplusOrMinimum() && entry.setPointInWatt > 0) {
				builder.add(entry);
			}
		}
		return builder.build();
	}

	@Override
	public final String toString() {
		return toStringHelper(PowerDistributionEntries.class) //
				.add("totalActivePower", this.getTotalActivePower()) //
				.add("entries", "\n" + this.allEntries.stream() //
						.map(PowerDistributionEntry::toString) //
						.collect(joining("\n"))) //
				.toString();
	}
}
