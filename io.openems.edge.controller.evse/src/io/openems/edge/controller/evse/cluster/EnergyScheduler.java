package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.controller.evse.cluster.EshUtils.generateModes;
import static io.openems.edge.controller.evse.cluster.EshUtils.parseTasks;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes.JointMode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;

public class EnergyScheduler {

	public static record OptimizationContext(//
			ClusterEshConfig clusterConfig, //
			JointModes<Mode> modes, //
			ImmutableTable<String, ZonedDateTime, Mode> manualModes, //
			ImmutableTable<String, ZonedDateTime, Payload.Smart> smartPayloads) {
	}

	public static record ClusterScheduleContext(ImmutableMap<String, SingleScheduleContext> singleCscs) {

		/**
		 * Gets the {@link SingleScheduleContext} of the given Component.
		 * 
		 * @param componentId the Component-ID
		 * @return the csc
		 */
		public SingleScheduleContext getCsc(String componentId) {
			return this.singleCscs.get(componentId);
		}
	}

	public static class SingleScheduleContext {
		private int sessionEnergy;

		public SingleScheduleContext(int initialSessionEnergy) {
			this.sessionEnergy = initialSessionEnergy;
		}

		/**
		 * Applies the charge energy per period.
		 * 
		 * @param chargeEnergy the energy
		 */
		public void applyCharge(int chargeEnergy) {
			this.sessionEnergy += chargeEnergy;
		}

		public int getSessionEnergy() {
			return this.sessionEnergy;
		}
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent                the parent {@link OpenemsComponent}
	 * @param clockProvider         a {@link ClockProvider}
	 * @param clusterConfigSupplier supplier for {@link ClusterEshConfig}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<JointMode<Mode>, OptimizationContext, ClusterScheduleContext> buildEnergyScheduleHandler(
			OpenemsComponent parent, ClockProvider clockProvider, Supplier<ClusterEshConfig> clusterConfigSupplier) {
		return EnergyScheduleHandler.WithDifferentModes
				.<JointMode<Mode>, OptimizationContext, ClusterScheduleContext>create(parent) //
				.setSerializer(ClusterEshConfig.serializer(clockProvider.getClock()), clusterConfigSupplier) //

				.setOptimizationContext(goc -> {
					final var clusterConfig = clusterConfigSupplier.get();

					// Parse OneTasks with Payload.Manual, i.e. Periods with predefined Mode
					final var t = parseTasks(goc, clusterConfig);
					final var manualModes = t.a();
					final var smartPayloads = t.b();

					// Generate Modes
					final var modes = generateModes(clusterConfig, smartPayloads);

					return new OptimizationContext(clusterConfig, modes, manualModes, smartPayloads);
				})

				.setModes((goc, coc) -> coc.modes()) //

				.setScheduleContext(coc -> {
					return new ClusterScheduleContext(coc.clusterConfig.singleParams.entrySet().stream() //
							.collect(toImmutableMap(//
									e -> e.getKey(), // Component-ID
									e -> new SingleScheduleContext(e.getValue().sessionEnergy()))));
				}) //

				.setPreProcessor((period, csc, mode) -> {
					// Find actual Mode per Single-Controller
					final var actualModesPerComponent = csc.clusterConfig.singleParams.values().stream() //
							.collect(ImmutableMap.toImmutableMap(//
									p -> p.ctrlSingleId(), //
									p -> EshUtils.getSingleMode(period, csc, mode, p)));

					return csc.modes().streamAll() //
							// Find JointMode with all Modes same
							.filter(jm -> jm.mode().mode().submodes().equals(actualModesPerComponent)) //
							.findFirst().map(jm -> jm.mode().mode()) //
							.orElse(null);
				}) //

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					var ed = EshUtils.EnergyDistribution.fromSimulator(period, coc, csc, mode);
					ed.initializeSetPoints();
					ed.distributeSurplusEnergy(DistributionStrategy.EQUAL_POWER);
					ed.applyChargeEnergy(ef);
					return mode;
				})

				.build();
	}

	public static record ClusterEshConfig(//
			DistributionStrategy distributionStrategy, //
			ImmutableMap<String, Params> singleParams) {

		protected static ClusterEshConfig from(//
				DistributionStrategy distributionStrategy, //
				ImmutableList<Params> singleParams) {
			return new ClusterEshConfig(distributionStrategy, singleParams.stream() //
					.collect(toImmutableMap(p -> p.ctrlSingleId(), p -> p)));
		}

		/**
		 * Gets the {@link Params} of the given Component.
		 * 
		 * @param componentId the Component-ID
		 * @return the Params
		 */
		public Params getSingleParams(String componentId) {
			return this.singleParams.get(componentId);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link EshConfig}.
		 *
		 * @param clock the {@link Clock}
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ClusterEshConfig> serializer(Clock clock) {
			return JsonSerializerUtil.jsonObjectSerializer(json -> {
				return ClusterEshConfig.from(//
						json.getEnum("distributionStrategy", DistributionStrategy.class), //
						json.getImmutableList("params", Params.serializer(clock)) //
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("distributionStrategy", obj.distributionStrategy) //
						.add("params", obj.singleParams.values().stream() //
								.map(Params.serializer(clock)::serialize) //
								.collect(toJsonArray())) //
						.build();
			});
		}
	}

}