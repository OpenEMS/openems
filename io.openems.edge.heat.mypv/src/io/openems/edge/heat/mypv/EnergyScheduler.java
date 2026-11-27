package io.openems.edge.heat.mypv;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.lang.Math.clamp;

import java.time.Clock;
import java.util.Arrays;
import java.util.function.Supplier;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.SingleModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.meter.api.ElectricityMeter;

public class EnergyScheduler {

	/**
	 * Serializable configuration snapshot for the energy schedule handler.
	 *
	 * @param defaultMode  the fallback {@link Mode} when no task is active
	 * @param maxHeatPower the maximum heat power [W]
	 * @param tasks        the list of tasks that define the schedule
	 */
	public record Config(Mode defaultMode, int maxHeatPower, JSCalendar.Tasks<HeatMyPvPayload> tasks) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 *
		 * @param clock the {@link Clock} to use for serializing and deserializing the
		 *              tasks
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer(Clock clock) {
			return jsonObjectSerializer(Config.class, //
					json -> new Config(//
							json.getEnum("defaultMode", Mode.class), //
							json.getInt("maxHeatPower"), //
							json.getObject("tasks", //
									JSCalendar.Tasks.serializer(clock, HeatMyPvPayload.serializer()))), //
					obj -> buildJsonObject() //
							.addProperty("defaultMode", obj.defaultMode()) //
							.addProperty("maxHeatPower", obj.maxHeatPower()) //
							.add("tasks", JSCalendar.Tasks.serializer(clock, HeatMyPvPayload.serializer()) //
									.serialize(obj.tasks()))
							.build());
		}
	}

	public record OptimizationContext(JSCalendar.OneTasks<HeatMyPvPayload> oneTasks, //
			Mode defaultMode, //
			int maxHeatPower, //
			Integer remainingHeatEnergy) { //
	}

	public static class ScheduleContext {
		private Integer remainingHeatEnergy;

		public ScheduleContext(Integer remainingHeatEnergy) {
			this.remainingHeatEnergy = remainingHeatEnergy;
		}

		/**
		 * Limits the given energy to the remaining heat energy.
		 *
		 * @param energy the energy
		 * @return the limited energy
		 */
		public int limitToRemainingHeatEnergy(int energy) {
			if (this.remainingHeatEnergy == null) {
				return energy;
			}
			return Math.min(energy, this.remainingHeatEnergy);
		}

		/**
		 * Applies the planned heat energy.
		 *
		 * @param energy the planned energy
		 */
		public void applyHeatEnergy(int energy) {
			if (this.remainingHeatEnergy == null) {
				return;
			}
			this.remainingHeatEnergy = Math.max(0, this.remainingHeatEnergy - energy);
		}
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 *
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 *
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param clockProvider  the {@link ClockProvider} to use for serializing and
	 *                       deserializing the tasks
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<Mode, OptimizationContext, ScheduleContext> buildEnergyScheduleHandler(
			OpenemsComponent parent, //
			ClockProvider clockProvider, //
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode, OptimizationContext, ScheduleContext>create(parent) //
				.setSerializer(Config.serializer(clockProvider.getClock()), configSupplier) //

				.setModes(() -> new SingleModes<>(//
						new Modes.Channels(//
								new ChannelAddress(parent.id(), HeatMyPv.ChannelId.MODE.id()),
								new ChannelAddress(parent.id(), ElectricityMeter.ChannelId.ACTIVE_POWER.id())),
						Arrays.stream(Mode.values()) //
								.map(mode -> new SingleModes.SingleMode<>(mode, false, null)) //
								.collect(toImmutableList())))

				.setOptimizationContext(goc -> {
					var config = configSupplier.get();
					if (config == null) {
						return null;
					}
					var firstPeriodTime = goc.periods().getFirst().time();
					var lastPeriodTime = goc.periods().getLast().time();
					var tasks = config.tasks().getOneTasksBetween(firstPeriodTime, lastPeriodTime);
					return new OptimizationContext(tasks, config.defaultMode(), config.maxHeatPower(),
							getRemainingHeatEnergy(parent));
				})

				.setScheduleContext(coc -> coc == null ? null : new ScheduleContext(coc.remainingHeatEnergy()))

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					if (coc == null) {
						return mode;
					}
					var payload = coc.oneTasks().getPayloadAt(period.time());
					var activeMode = payload != null ? payload.mode() : coc.defaultMode();
					var maxHeatEnergy = period.duration().convertPowerToEnergy(coc.maxHeatPower());
					var energy = switch (activeMode) {
					case FAST_HEAT -> csc.limitToRemainingHeatEnergy(maxHeatEnergy);
					case SURPLUS -> csc.limitToRemainingHeatEnergy(clamp(ef.getSurplus(), 0, maxHeatEnergy));
					case OFF -> 0;
					};
					var actualEnergy = ef.addManagedConsumption(id, energy);
					if (activeMode != Mode.OFF) {
						csc.applyHeatEnergy(actualEnergy);
					}
					return activeMode;
				})

				.build();
	}

	private static Integer getRemainingHeatEnergy(OpenemsComponent parent) {
		Channel<Integer> channel = parent.channelOrNull(Heat.ChannelId.REMAINING_HEAT_ENERGY);
		if (channel == null) {
			return null;
		}
		return channel.value().get();
	}
}
