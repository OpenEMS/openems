package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.joining;

import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes.SingleMode;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergyScheduler {

	public static record OptimizationContext(ClusterEshConfig clusterConfig) {
	}

	/**
	 * Holds the combination of {@link Mode.Actual} of multiple
	 * Evse.Controller.Single.
	 */
	public static record SingleModes(ImmutableMap<String, Mode.Actual> modes) {

		protected static record SingleMode(String componentId, Mode.Actual mode) {
		}

		/**
		 * Gets the {@link Mode.Actual} of the given Component.
		 * 
		 * @param componentId the Component-ID
		 * @return the mode
		 */
		public Mode.Actual getMode(String componentId) {
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
					return new OptimizationContext(clusterConfig);
				})

				.setAvailableModes((goc, coc) -> {
					return Lists.cartesianProduct(coc.clusterConfig.singleParams.values().stream() //
							.filter(p -> switch (p.mode()) {
							case FORCE, MINIMUM, SURPLUS, ZERO -> false;
							case SMART -> true; // consider only SMART for available modes
							}) //
							.map(p -> {
								var availableModes = p.combinedAbilities().isReadyForCharging()
										&& !p.history().getAppearsToBeFullyCharged() //
												// TODO MINIMUM instead of ZERO if interrupt is not allowed
												? new Mode.Actual[] { Mode.Actual.SURPLUS, Mode.Actual.ZERO,
														Mode.Actual.FORCE } //
												: new Mode.Actual[] { Mode.Actual.ZERO }; // No choice
								return IntStream.range(0, availableModes.length) //
										.mapToObj(i -> new SingleMode(p.componentId(), availableModes[i])) //
										.toList();
							}) //
							.toList()) //
							.stream() //
							.map(l -> new SingleModes(l.stream() //
									.collect(toImmutableMap(SingleMode::componentId, SingleMode::mode)))) //
							.toArray(SingleModes[]::new);
				}) //

				.setScheduleContext(coc -> {
					return new ClusterScheduleContext(coc.clusterConfig.singleParams.entrySet().stream() //
							.collect(toImmutableMap(//
									e -> e.getKey(), // Component-ID
									e -> new SingleScheduleContext(e.getValue().sessionEnergy()))));
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