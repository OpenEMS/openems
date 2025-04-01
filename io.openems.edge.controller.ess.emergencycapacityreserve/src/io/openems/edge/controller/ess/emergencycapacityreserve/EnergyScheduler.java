package io.openems.edge.controller.ess.emergencycapacityreserve;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;

import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setSerializer(() -> Config.toJson(configSupplier.get())) //

				.setOptimizationContext(gsc -> {
					var config = configSupplier.get();
					return config != null //
							? new OptimizationContext(socToEnergy(gsc.ess().totalEnergy(), config.minSoc)) //
							: null; //
				})

				.setSimulator((gsc, coc, ef) -> {
					if (coc != null) {
						ef.setEssMaxDischarge(max(0, gsc.ess.getInitialEnergy() - coc.minEnergy));
					}
				}) //

				.build();
	}

	private static record OptimizationContext(int minEnergy) {
	}

	public static record Config(int minSoc) {

		/**
		 * Serialize.
		 * 
		 * @param config the {@link Config}, possibly null
		 * @return the {@link JsonElement}
		 */
		private static JsonElement toJson(Config config) {
			if (config == null) {
				return JsonNull.INSTANCE;
			}
			return buildJsonObject() //
					.addProperty("minSoc", config.minSoc()) //
					.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonElement}
		 * @return the {@link Config}
		 * @throws OpenemsNamedException on error
		 */
		public static Config fromJson(JsonElement j) {
			if (j.isJsonNull()) {
				return null;
			}
			try {
				return new Config(//
						getAsInt(j, "minSoc"));
			} catch (OpenemsNamedException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
}
