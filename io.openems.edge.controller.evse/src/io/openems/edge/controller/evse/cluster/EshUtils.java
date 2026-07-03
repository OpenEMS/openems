package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.openems.common.utils.IntUtils.fitWithin;
import static io.openems.common.utils.IntUtils.minInt;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.Tuple2;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterEshConfig;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleScheduleContext;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.controller.evse.single.Types.Payload.Smart;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes.JointMode;
import io.openems.edge.energy.api.simulation.EnergyFlow.Model;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.meter.api.ElectricityMeter;

public class EshUtils {

	private EshUtils() {
	}

	/**
	 * Holds temporary calculations and power distribution among
	 * {@link ControllerEvseSingle}s.
	 */
	public static class EnergyDistribution {

		protected static EnergyDistribution fromSimulator(Period period, OptimizationContext clusterCoc,
				ClusterScheduleContext clusterCsc, JointMode<Mode> mode) {
			final int surplusEnergy = period.data().consumption()//
					.map(c -> period.data().production() - c.actual())//
					.orElse(0);

			final var entries = clusterCoc.clusterConfig().singleParams().values().stream() //
					.map(p -> {
						final var csc = clusterCsc.getCsc(p.ctrlSingleId());
						final var scheduledMode = mode.getMode(p.ctrlSingleId());
						final var abilities = p.combinedAbilities();

						// Evaluate Energy limit
						final var energyLimit = minInt(//
								p.combinedAbilities().electricVehicleAbilities().capacity(), //
								p.sessionEnergyLimit());
						final var remainingSessionEnergy = max(0, energyLimit - csc.getSessionEnergy());
						final int maxEnergy = min(remainingSessionEnergy,
								period.duration().convertPowerToEnergy(abilities.applySetPoint().max()));

						final int energyInModeMinimum = period.duration()
								.convertPowerToEnergy(abilities.applySetPoint().min());
						final var actualMode = abilities.isReadyForCharging() && !p.appearsToBeFullyCharged() //
								? scheduledMode //
								: Mode.ZERO;
						return new EnergyDistribution.Entry(p.ctrlSingleId(), csc, scheduledMode, actualMode,
								energyInModeMinimum, maxEnergy);
					}) //
					.collect(toImmutableList());

			return new EnergyDistribution(surplusEnergy, entries);
		}

		/**
		 * Holds {@link EnergyDistribution} for one single {@link ControllerEvseSingle}.
		 */
		public static class Entry {
			public final String componentId;
			public final Mode scheduledMode;
			public final Mode actualMode;
			public final int energyInModeMinimum;
			public final int maxEnergy;

			private final SingleScheduleContext csc;

			protected int actualEnergy;

			public Entry(String componentId, SingleScheduleContext csc, Mode scheduledMode, Mode actualMode,
					int energyInModeMinimum, int maxEnergy) {
				this.componentId = componentId;
				this.csc = csc;
				this.scheduledMode = scheduledMode;
				this.actualMode = actualMode;
				this.energyInModeMinimum = min(energyInModeMinimum, maxEnergy);
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
				e.actualEnergy = switch (e.actualMode) {
				case MINIMUM -> e.energyInModeMinimum;
				case FORCE -> e.maxEnergy;
				case SURPLUS, ZERO -> 0;
				};
			});
		}

		protected void distributeSurplusEnergy(DistributionStrategy distributionStrategy) {
			var totalExcessEnergy = max(0, this.surplusEnergy - this.sumActualEnergies());

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
					.filter(e -> e.actualMode == Mode.SURPLUS) //
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
				var after = fitWithin(0, e.maxEnergy, before + min(remaining, equalEnergy));
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

	protected static Tuple2<ImmutableTable<String, ZonedDateTime, Mode>, ImmutableTable<String, ZonedDateTime, Smart>> parseTasks(
			GlobalOptimizationContext goc, ClusterEshConfig clusterConfig) {
		final var firstTime = goc.periods().getFirst().time();
		final var lastTime = goc.periods().getLast().time();

		final var manualModes = ImmutableTable.<String, ZonedDateTime, Mode>builder();
		final var smartPayloads = ImmutableTable.<String, ZonedDateTime, Payload.Smart>builder();
		for (var p : clusterConfig.singleParams().values()) {
			for (var ot : p.tasks().getOneTasksBetween(firstTime, lastTime)) {
				for (var t = ot.start(); t.isBefore(lastTime) && t.isBefore(ot.end()); t = t.plusMinutes(15)) {
					switch (ot.payload()) {
					case Payload.Manual m -> manualModes.put(p.ctrlSingleId(), t, m.mode());
					case Payload.Smart s -> smartPayloads.put(p.ctrlSingleId(), t, s);
					case null -> System.out.println("Task has no payload: " + ot.toString());
					}
				}
			}
		}
		return Tuple2.of(manualModes.build(), smartPayloads.build());
	}

	protected static JointModes<Mode> generateModes(ClusterEshConfig clusterConfig,
			ImmutableTable<String, ZonedDateTime, Smart> smartPayloads) {
		final var addToOptimizers = clusterConfig.singleParams().values().stream() //
				.filter(p -> {
					if (smartPayloads.row(p.ctrlSingleId()).isEmpty()) {
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
				.map(p -> p.ctrlSingleId()) //
				.collect(toImmutableSet());

		// Make sure SURPLUS is the default/fallback mode
		final var singleModes = Stream.concat(Stream.of(Mode.SURPLUS), Stream.of(Mode.values())) //
				.collect(toImmutableSet());

		final var allModes = Lists.cartesianProduct(//
				clusterConfig.singleParams().values().stream() //
						.map(p -> singleModes.stream() //
								.map(mode -> new Tuple2<String, Mode>(p.ctrlSingleId(), mode)) //
								.toList()) //
						.toList()) //
				.stream() //
				.map(l -> {
					var addToOptimizer = l.stream().anyMatch(sm -> addToOptimizers.contains(sm.a() /* Component-ID */));
					return new JointMode<Mode>(//
							l.stream() //
									.collect(toImmutableMap(Tuple2::a, Tuple2::b)), //
							addToOptimizer, //
							null); // TODO
				}) //
				.collect(toImmutableList());

		final var channels = clusterConfig.singleParams().values().stream()//
				.collect(ImmutableMap.toImmutableMap(//
						p -> p.ctrlSingleId(), //
						p -> new Modes.Channels(
								new ChannelAddress(p.ctrlSingleId(), ControllerEvseSingle.ChannelId.ACTUAL_MODE.id()),
								new ChannelAddress(p.chargePointId(), ElectricityMeter.ChannelId.ACTIVE_POWER.id()))));

		return new JointModes<Mode>(channels, allModes);
	}

	protected static Mode getSingleMode(Period period, OptimizationContext clusterCoc, JointMode<Mode> simulatedMode,
			Params p) {
		// TODO 1st Priority: One-Shot
		// 2nd Priority: Manual Mode
		final var fromManualMode = clusterCoc.manualModes().get(p.ctrlSingleId(), period.time());
		if (fromManualMode != null) {
			return fromManualMode;
		}
		// 3rd Priority: Simulated SingleMode
		if (simulatedMode != null) {
			final var fromSimulationSchedule = simulatedMode.getMode(p.ctrlSingleId());
			if (fromSimulationSchedule != null) {
				return fromSimulationSchedule;
			}
		}
		// 4th Priority: fallback to configured mode
		return p.mode();
	}
}
