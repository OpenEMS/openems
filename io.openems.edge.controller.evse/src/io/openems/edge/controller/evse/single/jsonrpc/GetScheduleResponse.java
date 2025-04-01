package io.openems.edge.controller.evse.single.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.api.EnergyUtils.toPower;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.evse.single.EnergyScheduler.SmartOptimizationContext;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.handler.EshWithOnlyOneMode;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Mode.Actual;

/**
 * Represents a JSON-RPC Response for 'getSchedule'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'schedule': [{
 *      'timestamp':...,
 *      'price':...,
 *      'mode':...,
 *      'grid':...,
 *      'production':...,
 *      'consumption':...,
 *      'managedConsumption':...,
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetScheduleResponse extends JsonrpcResponseSuccess {

	private final JsonObject result;

	public GetScheduleResponse(UUID id, JsonObject result) {
		super(id);
		this.result = result;
	}

	@Override
	public JsonObject getResult() {
		return this.result;
	}

	/**
	 * Builds a {@link GetScheduleResponse} with last three hours data and current
	 * Schedule.
	 * 
	 * @param requestId                   the JSON-RPC request-id
	 * @param smartEnergyScheduleHandler  the {@link EshWithDifferentModes}
	 * @param manualEnergyScheduleHandler the {@link EshWithOnlyOneMode}
	 * @return the {@link GetScheduleResponse}
	 */
	public static GetScheduleResponse from(UUID requestId,
			EshWithDifferentModes<Actual, SmartOptimizationContext, ScheduleContext> smartEnergyScheduleHandler,
			EshWithOnlyOneMode<ManualOptimizationContext, ScheduleContext> manualEnergyScheduleHandler) {
		final Stream<JsonObject> future;
		if (smartEnergyScheduleHandler != null) {
			// TODO historic
			future = toJsonObjectStream(smartEnergyScheduleHandler.getParentId(),
					smartEnergyScheduleHandler.getSchedule(), (b, p) -> b //
							.addProperty("mode", p.mode().getValue()));

		} else if (manualEnergyScheduleHandler != null) {
			future = toJsonObjectStream(manualEnergyScheduleHandler.getParentId(),
					manualEnergyScheduleHandler.getSchedule(), (b, p) -> b //
							.addProperty("mode", (p.coc().isReadyForCharging() //
									? p.coc().mode() //
									: Mode.Actual.ZERO).getValue()));

		} else {
			future = Stream.of();
		}
		var schedule = future.collect(JsonUtils.toJsonArray());

		return new GetScheduleResponse(requestId, //
				buildJsonObject() //
						.add("schedule", schedule) //
						.build());
	}

	private static <PERIOD extends EnergyScheduleHandler.Period<?>> Stream<JsonObject> toJsonObjectStream(
			String parentId, ImmutableSortedMap<ZonedDateTime, PERIOD> schedule,
			BiConsumer<JsonUtils.JsonObjectBuilder, PERIOD> builder) {
		return schedule.entrySet().stream() //
				.map(e -> {
					var p = e.getValue();
					var b = buildJsonObject() //
							.addProperty("timestamp", e.getKey()) //
							.addProperty("price", p.price()) //
							.addProperty("grid", toPower(p.energyFlow().getGrid())) //
							.addProperty("production", toPower(p.energyFlow().getProd())) //
							.addProperty("consumption", toPower(p.energyFlow().getCons())) //
							.addProperty("managedConsumption", toPower(p.energyFlow().getManagedCons(parentId))) //
					;
					builder.accept(b, p);
					return b.build();
				});
	}
}
