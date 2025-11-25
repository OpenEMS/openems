package io.openems.edge.controller.evse.cluster;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.energy.api.simulation.EnergyFlow.Model;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;

public class EshUtils {

	private EshUtils() {
	}

	/**
	 * Holds temporary calculations and power distribution among
	 * {@link ControllerEvseSingle}s.
	 */
	public static class EnergyDistribution {

		protected static EnergyDistribution fromSimulator(Period period, OptimizationContext clusterCoc,
				ScheduleContext clusterCsc, SingleModes mode) {
			final var surplusEnergy = period.duration()
					.convertPowerToEnergy(period.production() - period.consumption());

			var b = ImmutableList.<EnergyDistribution.Entry>builder();
			for (int i = 0, eshWithDifferentModesIndex = 0; i < clusterCoc.singleCocs().size(); i++) {
				final var coc = clusterCoc.singleCocs().get(i);
				final var abilities = coc.combinedAbilities();
				final var csc = clusterCsc.singleCscs().get(i);

				final Mode.Actual singleMode;
				final Integer remainingSessionEnergy;
				if (coc instanceof ManualOptimizationContext moc) {
					singleMode = moc.mode();
					remainingSessionEnergy = moc.sessionEnergyLimit() > 0 //
							? Math.max(0, moc.sessionEnergyLimit() - csc.getSessionEnergy()) //
							: null;
				} else {
					singleMode = mode.modes().get(eshWithDifferentModesIndex).mode();
					remainingSessionEnergy = null;
					eshWithDifferentModesIndex++;
				}

				final int maxEnergy;
				final int energyInModeMinimum;
				if (abilities.isReadyForCharging()) {
					energyInModeMinimum = period.duration().convertPowerToEnergy(abilities.applySetPoint().min());
					maxEnergy = TypeUtils.min(remainingSessionEnergy,
							period.duration().convertPowerToEnergy(abilities.applySetPoint().max()));
				} else {
					energyInModeMinimum = 0;
					maxEnergy = 0;
				}

				b.add(new EnergyDistribution.Entry(csc, singleMode, energyInModeMinimum, maxEnergy));
			}
			return new EnergyDistribution(surplusEnergy, b.build());
		}

		/**
		 * Holds {@link EnergyDistribution} for one single {@link ControllerEvseSingle}.
		 */
		public static class Entry {
			public final Mode.Actual mode;
			public final int energyInModeMinimum;
			public final int maxEnergy;

			private final io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext csc;

			protected int actualEnergy;

			public Entry(io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext csc, Mode.Actual mode,
					int energyInModeMinimum, int maxEnergy) {
				this.csc = csc;
				this.mode = mode;
				this.energyInModeMinimum = Math.min(energyInModeMinimum, maxEnergy);
				this.maxEnergy = maxEnergy;
			}
		}

		public final int surplusEnergy;
		public final ImmutableList<Entry> entries;

		public EnergyDistribution(int surplusEnergy, ImmutableList<Entry> entries) {
			this.surplusEnergy = surplusEnergy;
			this.entries = entries;
		}

		protected void initializeSetPoints() {
			this.entries.stream().forEach(e -> {
				e.actualEnergy = switch (e.mode) {
				case MINIMUM -> e.energyInModeMinimum;
				case FORCE -> e.maxEnergy;
				case SURPLUS, ZERO -> 0;
				};
			});
		}

		protected void distributeSurplusEnergy(DistributionStrategy distributionStrategy) {
			var totalExcessEnergy = Math.max(0, this.surplusEnergy - this.sumActualEnergies());

			// TODO consider distributionStrategy
			this.distributeEnergyEqual(totalExcessEnergy);
		}

		protected int sumActualEnergies() {
			return this.entries.stream() //
					.mapToInt(e -> e.actualEnergy) //
					.sum();
		}

		private void distributeEnergyEqual(int initialDistributableEnergy) {
			var entries = this.entries.stream() //
					.filter(e -> e.mode == Actual.SURPLUS) //
					// Only entries that do not already apply max set-point
					.filter(e -> e.actualEnergy < e.maxEnergy) //
					.toList();
			if (entries.size() == 0) {
				return; // avoid divide by zero
			}

			final var equalEnergy = initialDistributableEnergy / entries.size();
			var remaining = initialDistributableEnergy;
			for (var e : entries) {
				var before = e.actualEnergy;
				var after = TypeUtils.fitWithin(0, e.maxEnergy, before + equalEnergy);
				remaining -= after - before;

				e.actualEnergy = after;
			}

			if (initialDistributableEnergy != remaining) {
				// Recursive call to distribute remaining power
				this.distributeEnergyEqual(remaining);
			}
		}

		protected void applyChargeEnergy(String id, Model ef) {
			// TODO what if addConsumption returns a smaller value than we want to apply?
			ef.addManagedConsumption(id, this.sumActualEnergies());
			this.entries.forEach(e -> {
				e.csc.applyCharge(e.actualEnergy);
			});
		}
	}

}
