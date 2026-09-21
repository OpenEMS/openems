package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.function.Supplier;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

public class EnergyScheduler {

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(//
			OpenemsComponent parent, //
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, ScheduleContext>create(parent)//

				.setSerializer(Config.serializer(), configSupplier)//

				.setOptimizationContext(goc -> OptimizationContext.from(goc, configSupplier.get()))//

				.setScheduleContext(ScheduleContext::new)//

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					if (coc == null) {
						return;
					}

					final int energy = period.duration().convertPowerToEnergy(coc.power());

					switch (coc.mode()) {
					case MANUAL_ON -> ef.setEss(energy);
					case CHARGE_ONCE -> {
						if (csc.isTargetReached()) {
							return;
						}

						final int remainingEnergy = coc.targetEssEnergy() - gsc.ess.getInitialEnergy();
						if (remainingEnergy <= 0) {
							csc.markTargetReached();
							return;
						}

						final int chargeEnergy = Math.min(Math.abs(energy), remainingEnergy);
						ef.setEss(-chargeEnergy);
					}
					case DISCHARGE_ONCE -> {
						if (csc.isTargetReached()) {
							return;
						}

						final int remainingEnergy = gsc.ess.getInitialEnergy() - coc.targetEssEnergy();
						if (remainingEnergy <= 0) {
							csc.markTargetReached();
							return;
						}

						final int dischargeEnergy = Math.min(Math.abs(energy), remainingEnergy);
						ef.setEss(dischargeEnergy);
					}
					case MANUAL_OFF ->
						throw new IllegalStateException("Mode MANUAL_OFF must not reach EnergyScheduler simulator");
					}
				})//

				.build();
	}

	public record Config(//
			Mode mode, //
			int power, //
			Integer targetSoc) {

		public Config {
			if (targetSoc != null && (targetSoc < 0 || targetSoc > 100)) {
				throw new IllegalArgumentException("targetSoc must be between 0 and 100");
			}
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, json -> {
				return new Config(//
						json.getEnum("mode", Mode.class), //
						json.getInt("power"), //
						json.getOptionalInt("targetSoc").orElse(null));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("mode", obj.mode()) //
						.addProperty("power", obj.power()) //
						.addPropertyIfNotNull("targetSoc", obj.targetSoc()) //
						.build();
			});
		}
	}

	public record OptimizationContext(//
			Mode mode, //
			int power, //
			Integer targetEssEnergy) {

		static OptimizationContext from(//
				GlobalOptimizationContext goc, //
				Config config) {
			if (config == null) {
				return null;
			}

			return switch (config.mode()) {
			case MANUAL_ON -> new OptimizationContext(config.mode(), config.power(), null);
			case CHARGE_ONCE, DISCHARGE_ONCE -> {
				final int capacity = goc.ess().totalEnergy();
				yield new OptimizationContext(config.mode(), config.power(),
						resolveTargetEssEnergy(config.mode(), capacity, config.targetSoc()));
			}
			case MANUAL_OFF ->
				throw new IllegalArgumentException("Mode MANUAL_OFF is not supported by EnergyScheduler");
			};
		}

		private static int resolveTargetEssEnergy(//
				Mode mode, //
				int capacity, //
				Integer targetSoc) {
			if (targetSoc != null) {
				return (int) Math.round(targetSoc / 100.0 * capacity);
			}
			return mode == Mode.CHARGE_ONCE ? capacity : 0;
		}
	}

	static final class ScheduleContext {

		private boolean targetReached = false;

		void markTargetReached() {
			this.targetReached = true;
		}

		boolean isTargetReached() {
			return this.targetReached;
		}
	}
}