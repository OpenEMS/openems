package io.openems.edge.heat.askoma;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.lang.Math.clamp;
import java.time.Clock;
import java.util.Arrays;
import java.util.function.Supplier;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;

public class EnergyScheduler {

	/**
	 * Serializable configuration snapshot for the energy schedule handler.
	 *
	 * @param defaultMode  the fallback {@link Mode} when no task is active
	 * @param maxHeatPower the maximum heat power [W]
	 * @param tasks        the list of tasks that define the schedule
	 */
	public record Config(Mode defaultMode, int maxHeatPower, JSCalendar.Tasks<HeatAskomaPayload> tasks) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 * @param clock the {@link Clock} to use for serializing and deserializing the tasks
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer(Clock clock) {
			return jsonObjectSerializer(Config.class, //
					json -> new Config(//
							json.getEnum("defaultMode", Mode.class), //
							json.getInt("maxHeatPower"), //
							json.getObject("tasks", //
									JSCalendar.Tasks.serializer(clock, HeatAskomaPayload.serializer()))), //
					obj -> buildJsonObject() //
							.addProperty("defaultMode", obj.defaultMode()) //
							.addProperty("maxHeatPower", obj.maxHeatPower()) //
							.add("tasks", JSCalendar.Tasks.serializer(clock, HeatAskomaPayload.serializer()) //
									.serialize(obj.tasks()))
							.build());
		}
	}

	public record OptimizationContext(
			JSCalendar.OneTasks<HeatAskomaPayload> oneTasks, //
			Mode defaultMode, //
			int maxHeatPower) { //
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 *
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 *
	 * @param openemsComponent the parent {@link OpenemsComponent}
	 * @param clockProvider    the {@link ClockProvider} to use for serializing and deserializing the tasks
	 * @param configSupplier   supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<Mode, OptimizationContext, Void> buildEnergyScheduleHandler(
			OpenemsComponent openemsComponent, //
			ClockProvider clockProvider, //
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode, OptimizationContext, Void>create(openemsComponent) //
				.setSerializer(Config.serializer(clockProvider.getClock()), configSupplier) //
				.setModes(() -> //
					Modes.of(Arrays.stream(Mode.values()) //
							.map(mode -> new Modes.Mode<>(mode, false, null)) //
							.collect(toImmutableList()))) //
				.setOptimizationContext(goc -> {
					var config = configSupplier.get();
					if (config == null) {
						return null;
					}
					var firstPeriodTime = goc.periods().getFirst().time();
					var lastPeriodTime = goc.periods().getLast().time();
					var tasks = config.tasks().getOneTasksBetween(firstPeriodTime, lastPeriodTime);
					return new OptimizationContext(tasks, config.defaultMode(), config.maxHeatPower());
				})
				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					if (coc == null) {
						return mode;
					}
					var payload = coc.oneTasks().getPayloadAt(period.time());
					var activeMode = payload != null ? payload.mode() : coc.defaultMode();
					var maxHeatEnergy = period.duration().convertPowerToEnergy(coc.maxHeatPower());
					var energy = switch (activeMode) {
						case FAST_HEAT -> maxHeatEnergy;
						case SURPLUS -> clamp(ef.getSurplus(), 0, maxHeatEnergy);
						case OFF -> 0;
					};
					ef.addManagedConsumption(id, energy);
					return activeMode;
				}) //
				.build();
	}
}
