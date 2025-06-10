package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.function.Supplier;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
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
				.setSerializer(OptimizationContext.serializer(), cocSupplier) //

				.setOptimizationContext(() -> cocSupplier.get()) //

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					if (coc != null) {
						var energy = period.duration().convertPowerToEnergy(coc.power);
						switch (coc.relationship) {
						case EQUALS -> ef.setEss(energy);
						case GREATER_OR_EQUALS -> ef.setEssMaxCharge(-energy);
						case LESS_OR_EQUALS -> ef.setEssMaxDischarge(energy);
						}
					}
				}) //

				.build();
	}

	public static record OptimizationContext(int power, Relationship relationship) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OptimizationContext}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OptimizationContext> serializer() {
			return jsonObjectSerializer(OptimizationContext.class, json -> {
				return new OptimizationContext(//
						json.getInt("power"), //
						json.getEnum("relationship", Relationship.class));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("power", obj.power()) //
						.addProperty("relationship", obj.relationship()) //
						.build();
			});
		}
	}
}