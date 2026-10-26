package io.openems.edge.controller.ess.limittotalcharge;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;

import java.util.function.Supplier;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public class EnergyScheduler {

	public static record OptimizationContext(int maxEnergy) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 *
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 *
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for the {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setSerializer(Config.serializer(), configSupplier) //

				.setOptimizationContext(gsc -> {
					var config = configSupplier.get();
					return config != null //
							? new OptimizationContext(socToEnergy(gsc.ess().totalEnergy(), config.maxSoc)) //
							: null; //
				})

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					if (coc != null) {
						ef.setEssMaxCharge(max(0, coc.maxEnergy - gsc.ess.getInitialEnergy()));
					}
				}) //

				.build();
	}

	public static record Config(Integer maxSoc) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, json -> {
				return new Config(//
						json.getOptionalInt("maxSoc").orElse(null) //
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("maxSoc", obj.maxSoc) //
						.build();
			});
		}
	}
}
