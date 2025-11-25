package io.openems.edge.controller.evse.cluster;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.util.stream.Collectors.joining;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.EnergyScheduler.EshEvseSingle;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergyScheduler {

	public static record OptimizationContext(//
			ImmutableList<EshEvseSingle> eshEvseSingles, //
			ImmutableList<io.openems.edge.controller.evse.single.EnergyScheduler.Config> singleCocs) {
	}

	public static record SingleMode(EnergyScheduleHandler.WithDifferentModes esh, Mode.Actual mode) {
	}

	public static record SingleModes(ImmutableList<SingleMode> modes) {
		@Override
		public final String toString() {
			return this.modes.stream() //
					.map(SingleMode::mode) //
					.map(Mode.Actual::name) //
					.collect(joining("+"));
		}
	}

	public static record ScheduleContext(
			ImmutableList<io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext> singleCscs) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent            the parent {@link OpenemsComponent}
	 * @param eshConfigSupplier supplier for {@link EshConfig}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<SingleModes, OptimizationContext, ScheduleContext> buildEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<EshConfig> eshConfigSupplier) {
		return EnergyScheduleHandler.WithDifferentModes
				.<SingleModes, OptimizationContext, ScheduleContext>create(parent) //
				.setSerializer(EshConfig.serializer(), eshConfigSupplier) //

				.setOptimizationContext(goc -> {
					var config = eshConfigSupplier.get();

					var singleCocs = config.eshEvseSingles.stream() //
							.map(esh -> esh.initialize(goc)) // Initialize ESHs of Evse.Single
							.collect(toImmutableList());

					return new OptimizationContext(config.eshEvseSingles, singleCocs);
				})

				.setAvailableModes((goc, coc) -> {
					return Lists.cartesianProduct(coc.eshEvseSingles.stream() //
							.map(EshEvseSingle::smartEnergyScheduleHandler) //
							.filter(Objects::nonNull) //
							.map(esh -> IntStream.range(0, esh.getNumberOfAvailableModes()) //
									.mapToObj(i -> new SingleMode(esh, esh.getMode(i))) //
									// .mapToObj(i -> new SingleMode(esh, i)) //
									.toList())
							.toList()) //
							.stream() //
							.map(l -> new SingleModes(l.stream() //
									.collect(toImmutableList()))) //
							.toArray(SingleModes[]::new);
				}) //

				.setScheduleContext(coc -> {
					return new ScheduleContext(coc.eshEvseSingles.stream() //
							.map(EshEvseSingle::createScheduleContext) //
							.collect(toImmutableList()));
				}) //

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness) -> {
					var ed = EshUtils.EnergyDistribution.fromSimulator(period, coc, csc, mode);
					ed.initializeSetPoints();
					ed.distributeSurplusEnergy(DistributionStrategy.EQUAL_POWER);
					ed.applyChargeEnergy(id, ef);
				})

				.build();
	}

	public static record EshConfig(//
			DistributionStrategy distributionStrategy, //
			ImmutableList<EshEvseSingle> eshEvseSingles) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link EshConfig}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<EshConfig> serializer() {
			return jsonObjectSerializer(json -> {
				return new EshConfig(//
						json.getEnum("distributionStrategy", DistributionStrategy.class), //
						null // TODO
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("distributionStrategy", obj.distributionStrategy) //
						.add("eshEvseSingles", null) //
						.build();
			});
		}
	}
}