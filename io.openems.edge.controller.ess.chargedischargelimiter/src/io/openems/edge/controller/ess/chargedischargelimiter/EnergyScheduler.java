package io.openems.edge.controller.ess.chargedischargelimiter;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;
import java.util.function.Supplier;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public class EnergyScheduler {

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

		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent)
				.setSerializer(Config.serializer(), configSupplier)

				.setOptimizationContext(gsc -> {
					var config = configSupplier.get();
					if (config == null || config.minSoc == null || config.maxSoc == null) {
						return null;
					}

					var totalEnergy = gsc.ess().totalEnergy();
					var minEnergy = socToEnergy(totalEnergy, config.minSoc());
					var maxEnergy = socToEnergy(totalEnergy, config.maxSoc());
					return new OptimizationContext(minEnergy, maxEnergy);
				})

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					if (coc == null) {
						return;
					}

					var currentEnergy = gsc.ess.getInitialEnergy();

					// limit discharge
					var allowedDischarge = max(0, currentEnergy - coc.minEnergy());
					ef.setEssMaxDischarge(allowedDischarge);

					// limit charge
					var allowedCharge = max(0, coc.maxEnergy() - currentEnergy);
					ef.setEssMaxCharge(allowedCharge);
				})

				.build();
	}

	private static record OptimizationContext(int minEnergy, int maxEnergy) {
	}

	public static record Config(Integer minSoc, Integer maxSoc) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, json -> {
				return new Config(json.getOptionalInt("minSoc").orElse(null),
						json.getOptionalInt("maxSoc").orElse(null));
			}, obj -> {
				return buildJsonObject().addProperty("minSoc", obj.minSoc).addProperty("maxSoc", obj.maxSoc).build();
			});
		}
	}
}
