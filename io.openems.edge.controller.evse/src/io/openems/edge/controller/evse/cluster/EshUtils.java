package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleScheduleContext;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.energy.api.simulation.EnergyFlow.Model;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EshUtils {

	private EshUtils() {
	}

	/**
	 * Holds temporary calculations and power distribution among
	 * {@link ControllerEvseSingle}s.
	 */
	public static class EnergyDistribution {

		protected static EnergyDistribution fromSimulator(Period period, OptimizationContext clusterCoc,
				ClusterScheduleContext clusterCsc, SingleModes mode) {
			final var surplusEnergy = period.production() - period.consumption();

			final var entries = clusterCoc.clusterConfig().singleParams().values().stream() //
					.map(p -> {
						final var csc = clusterCsc.getCsc(p.componentId());
						final var singleMode = Optional.ofNullable(mode.getMode(p.componentId())) //
								.orElse(p.mode()); // fallback to constant mode

						final var remainingSessionEnergy = p.sessionEnergyLimit() > 0 //
								? Math.max(0, p.sessionEnergyLimit() - csc.getSessionEnergy()) //
								: null;

						final var abilities = p.combinedAbilities();
						final int maxEnergy;
						final int energyInModeMinimum;
						if (abilities.isReadyForCharging()) {
							energyInModeMinimum = period.duration()
									.convertPowerToEnergy(abilities.applySetPoint().min());
							maxEnergy = TypeUtils.min(remainingSessionEnergy,
									period.duration().convertPowerToEnergy(abilities.applySetPoint().max()));
						} else {
							energyInModeMinimum = 0;
							maxEnergy = 0;
						}

						return new EnergyDistribution.Entry(p.componentId(), csc, singleMode, energyInModeMinimum,
								maxEnergy);
					}) //
					.collect(toImmutableList());

			return new EnergyDistribution(surplusEnergy, entries);
		}

		/**
		 * Holds {@link EnergyDistribution} for one single {@link ControllerEvseSingle}.
		 */
		public static class Entry {
			public final String componentId;
			public final Mode mode;
			public final int energyInModeMinimum;
			public final int maxEnergy;

			private final SingleScheduleContext csc;

			protected int actualEnergy;

			public Entry(String componentId, SingleScheduleContext csc, Mode mode, int energyInModeMinimum,
					int maxEnergy) {
				this.componentId = componentId;
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
					.filter(e -> e.mode == Mode.SURPLUS) //
					// Only entries that do not already apply max set-point
					.filter(e -> e.actualEnergy < e.maxEnergy) //
					.toList();
			if (entries.size() == 0) {
				return; // avoid divide by zero
			}

			final var equalEnergy = Math.ceilDiv(initialDistributableEnergy, entries.size());
			var remaining = initialDistributableEnergy;
			for (var e : entries) {
				var before = e.actualEnergy;
				var after = TypeUtils.fitWithin(0, e.maxEnergy, before + Math.min(remaining, equalEnergy));
				remaining -= after - before;

				e.actualEnergy = after;
			}

			if (initialDistributableEnergy != remaining) {
				// Recursive call to distribute remaining energy
				this.distributeEnergyEqual(remaining);
			}
		}

		protected void applyChargeEnergy(Model ef) {
			this.entries.forEach(e -> {
				var actualManagedConsumption = ef.addManagedConsumption(e.componentId, e.actualEnergy);
				e.csc.applyCharge(actualManagedConsumption);
			});
		}
	}

}
