package io.openems.edge.braiinsos;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static java.lang.Math.max;

import java.time.Clock;
import java.util.Arrays;
import java.util.function.Supplier;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.meter.api.ElectricityMeter;

public final class EnergyScheduler {

	private EnergyScheduler() {
	}

	public record Config(Mode defaultMode, int consumptionW, JSCalendar.Tasks<Payload> tasks) {

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
							json.getInt("consumptionW"), //
							json.getObject("tasks", //
									JSCalendar.Tasks.serializer(clock, Payload.serializer()))), //
					obj -> buildJsonObject() //
							.addProperty("defaultMode", obj.defaultMode())//
							.addProperty("consumptionW", obj.consumptionW())//
							.add("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()) //
									.serialize(obj.tasks()))//
							.build());
		}
	}

	public record OptimizationContext(//
			Mode defaultMode, //
			int consumptionW, //
			JSCalendar.OneTasks<Payload> oneTasks) {
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
	public static EshWithDifferentModes<Mode, OptimizationContext, Void> buildEnergyScheduleHandler(
			OpenemsComponent parent, //
			ClockProvider clockProvider, //
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode, OptimizationContext, Void>create(parent) //
				.setSerializer(Config.serializer(clockProvider.getClock()), configSupplier) //

				.setModes(() -> new DifferentModes.Modes.SingleModes<>(//
						new DifferentModes.Modes.Channels(//
								new ChannelAddress(parent.id(), ControllerBraiinsSingle.ChannelId.EFFECTIVE_MODE.id()),
								new ChannelAddress(parent.id(), ElectricityMeter.ChannelId.ACTIVE_POWER.id())),
						Arrays.stream(Mode.values()) //
								.map(mode -> new DifferentModes.Modes.SingleModes.SingleMode<>(//
										mode, //
										false, //
										null))//
								.collect(toImmutableList())))

				.setOptimizationContext(goc -> {
					final var config = configSupplier.get();
					if (config == null) {
						return null;
					}

					final var firstPeriodTime = goc.periods().getFirst().time();
					final var lastPeriodTime = goc.periods().getLast().time();
					final var oneTasks = config.tasks().getOneTasksBetween(firstPeriodTime, lastPeriodTime);

					return new OptimizationContext(config.defaultMode(), config.consumptionW(), oneTasks);
				})

				.setSimulator((id, period, gsc, coc, csc, ef, scheduledMode, fitness, isFinalRun) -> {
					if (coc == null) {
						return scheduledMode;
					}

					final var effectiveMode = switch (coc.oneTasks().getPayloadAt(period.time())) {
					case Payload.Manual(Mode manualMode) -> manualMode;
					case null -> coc.defaultMode();
					};

					final int consumptionW = switch (effectiveMode) {
					case OFF -> 0;
					case ON -> max(0, coc.consumptionW());
					};

					ef.addManagedConsumption(id, period.duration().convertPowerToEnergy(consumptionW));
					return effectiveMode;
				})

				.build();
	}
}
