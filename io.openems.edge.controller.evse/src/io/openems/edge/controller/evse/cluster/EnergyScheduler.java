package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.controller.evse.cluster.EshUtils.generateModes;
import static io.openems.edge.controller.evse.cluster.EshUtils.parseTasks;
import static java.util.stream.Collectors.joining;

import java.time.ZonedDateTime;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergyScheduler {

	public static record OptimizationContext(//
			ClusterEshConfig clusterConfig, //
			Modes<SingleModes> modes, //
			ImmutableTable<String, ZonedDateTime, Mode> manualModes, //
			ImmutableTable<String, ZonedDateTime, Payload.Smart> smartPayloads) {
	}

	/**
	 * Holds the combination of {@link Mode}s of multiple Evse.Controller.Single.
	 */
	public static record SingleModes(ImmutableMap<String, Mode> modes) {

		protected static record SingleMode(String componentId, Mode mode) {
		}

		/**
		 * Gets the {@link Mode} of the given Component.
		 * 
		 * @param componentId the Component-ID
		 * @return the mode or null
		 */
		public Mode getMode(String componentId) {
			return this.modes.get(componentId);
		}

		@Override
		public final String toString() {
			return this.modes.entrySet().stream() //
					.map(e -> e.getKey() + ":" + e.getValue()) //
					.collect(joining("+"));
		}
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
	 * @param clusterConfigSupplier supplier for {@link ClusterEshConfig}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<SingleModes, OptimizationContext, ClusterScheduleContext> buildEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<ClusterEshConfig> clusterConfigSupplier) {
		return EnergyScheduleHandler.WithDifferentModes
				.<SingleModes, OptimizationContext, ClusterScheduleContext>create(parent) //
				.setSerializer(ClusterEshConfig.serializer(), clusterConfigSupplier) //

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
					final var singleModes = csc.clusterConfig.singleParams.values().stream() //
							.collect(ImmutableMap.toImmutableMap(//
									p -> p.componentId(), //
									p -> EshUtils.getSingleMode(period, csc, mode, p)));
					return csc.modes.streamAll() //
							.filter(m -> m.mode().modes.equals(singleModes)) //
							.findFirst().map(m -> m.mode()).orElse(null);
				}) //

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness) -> {
					var ed = EshUtils.EnergyDistribution.fromSimulator(period, coc, csc, mode);
					ed.initializeSetPoints();
					ed.distributeSurplusEnergy(DistributionStrategy.EQUAL_POWER);
					ed.applyChargeEnergy(ef);
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
					.collect(toImmutableMap(p -> p.componentId(), p -> p)));
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
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ClusterEshConfig> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(json -> {
				return ClusterEshConfig.from(//
						json.getEnum("distributionStrategy", DistributionStrategy.class), //
						json.getImmutableList("params", Params.serializer()) //
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("distributionStrategy", obj.distributionStrategy) //
						.add("params", obj.singleParams.values().stream() //
								.map(Params.serializer()::serialize) //
								.collect(toJsonArray())) //
						.build();
			});
		}
	}

}