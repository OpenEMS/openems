package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;

import io.openems.common.types.Tuple;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterEshConfig;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes.SingleMode;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleScheduleContext;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.controller.evse.single.Types.Payload.Smart;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.simulation.EnergyFlow.Model;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
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
			final var surplusEnergy = period instanceof Period.WithPrediction wp //
					? wp.production() - wp.consumption() //
					: 0; // default to zero

			final var entries = clusterCoc.clusterConfig().singleParams().values().stream() //
					.map(p -> {
						final var csc = clusterCsc.getCsc(p.componentId());
						final var singleMode = mode.getMode(p.componentId());
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

	protected static Tuple<ImmutableTable<String, ZonedDateTime, Mode>, ImmutableTable<String, ZonedDateTime, Smart>> parseTasks(
			GlobalOptimizationContext goc, ClusterEshConfig clusterConfig) {
		final var firstTime = goc.periods().getFirst().time();
		final var lastTime = goc.periods().getLast().time();

		final var manualModes = ImmutableTable.<String, ZonedDateTime, Mode>builder();
		final var smartPayloads = ImmutableTable.<String, ZonedDateTime, Payload.Smart>builder();
		for (var p : clusterConfig.singleParams().values()) {
			for (var ot : p.tasks().getOneTasksBetween(firstTime, lastTime)) {
				for (var t = ot.start(); t.isBefore(lastTime) && t.isBefore(ot.end()); t = t.plusMinutes(15)) {
					switch (ot.payload()) {
					case Payload.Manual m -> manualModes.put(p.componentId(), t, m.mode());
					case Payload.Smart s -> smartPayloads.put(p.componentId(), t, s);
					case null -> System.out.println("Task has no payload: " + ot.toString());
					}
				}
			}
		}
		return Tuple.of(manualModes.build(), smartPayloads.build());
	}

	protected static Modes<SingleModes> generateModes(ClusterEshConfig clusterConfig,
			ImmutableTable<String, ZonedDateTime, Smart> smartPayloads) {
		final var addToOptimizers = clusterConfig.singleParams().values().stream() //
				.filter(p -> {
					if (smartPayloads.row(p.componentId()).isEmpty()) {
						// Consider only optimizable Single-Controllers; i.e. has "SMART"-Tasks
						// No room for optimization
						return false;
					}
					if (!p.combinedAbilities().isReadyForCharging() && p.history().getAppearsToBeFullyCharged()) {
						// No room for optimization
						return false;
					}
					return true;
				}) //
				.map(p -> p.componentId()) //
				.collect(toImmutableSet());

		// Make sure SURPLUS is the default/fallback mode
		final var singleModes = Stream.concat(Stream.of(Mode.SURPLUS), Stream.of(Mode.values())) //
				.collect(ImmutableSet.toImmutableSet());
		final var allModes = Lists.cartesianProduct(clusterConfig.singleParams().values().stream() //
				.map(p -> {
					return singleModes.stream() //
							.map(mode -> new SingleMode(p.componentId(), mode)) //
							.toList();
				}) //
				.toList()) //
				.stream() //
				.map(l -> {
					var addToOptimizer = l.stream().anyMatch(sm -> addToOptimizers.contains(sm.componentId()));
					return new Modes.Mode<SingleModes>(new SingleModes(l.stream() //
							.collect(toImmutableMap(SingleMode::componentId, SingleMode::mode))), addToOptimizer);
				}) //
				.collect(toImmutableList());
		return Modes.of(allModes);
	}

	protected static Mode getSingleMode(Period period, OptimizationContext clusterCoc, SingleModes simulatedMode,
			Params p) {
		// TODO 1st Priority: One-Shot
		// 2nd Priority: Manual Mode
		final var fromManualMode = clusterCoc.manualModes().get(p.componentId(), period.time());
		if (fromManualMode != null) {
			return fromManualMode;
		}
		// 3rd Priority: Simulated SingleMode
		if (simulatedMode != null) {
			final var fromSimulationSchedule = simulatedMode.getMode(p.componentId());
			if (fromSimulationSchedule != null) {
				return fromSimulationSchedule;
			}
		}
		// 4th Priority: fallback to configured mode
		return p.mode();
	}
}
