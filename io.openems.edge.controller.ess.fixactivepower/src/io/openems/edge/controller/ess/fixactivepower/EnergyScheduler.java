package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsInt;

import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.ess.power.api.Relationship;

public class EnergyScheduler {

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent      the parent {@link OpenemsComponent}
	 * @param cocSupplier supplier for {@link OptimizationContext}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<OptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setSerializer(() -> OptimizationContext.toJson(cocSupplier.get())) //

				.setOptimizationContext(() -> cocSupplier.get()) //

				.setSimulator((gsc, coc, ef) -> {
					if (coc != null) {
						switch (coc.relationship) {
						case EQUALS -> ef.setEss(coc.energy);
						case GREATER_OR_EQUALS -> ef.setEssMaxCharge(-coc.energy);
						case LESS_OR_EQUALS -> ef.setEssMaxDischarge(coc.energy);
						}
					}
				}) //

				.build();
	}

	public static record OptimizationContext(int energy, Relationship relationship) {

		/**
		 * Serialize.
		 * 
		 * @param coc the {@link OptimizationContext}, possibly null
		 * @return the {@link JsonElement}
		 */
		private static JsonElement toJson(OptimizationContext coc) {
			if (coc == null) {
				return JsonNull.INSTANCE;
			}
			return buildJsonObject() //
					.addProperty("energy", coc.energy()) //
					.addProperty("relationship", coc.relationship()) //
					.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonElement}
		 * @return the {@link OptimizationContext}
		 */
		public static OptimizationContext fromJson(JsonElement j) {
			if (j.isJsonNull()) {
				return null;
			}
			try {
				return new OptimizationContext(//
						getAsInt(j, "energy"), //
						getAsEnum(Relationship.class, j, "relationship"));
			} catch (OpenemsNamedException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

}