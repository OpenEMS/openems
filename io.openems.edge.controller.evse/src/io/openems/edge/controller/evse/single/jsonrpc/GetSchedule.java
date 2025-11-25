package io.openems.edge.controller.evse.single.jsonrpc;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.function.BiFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.controller.evse.single.EnergyScheduler.EshEvseSingle;
import io.openems.edge.controller.evse.single.jsonrpc.GetSchedule.Response;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.evse.api.chargepoint.Mode;

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
public class GetSchedule implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getSchedule";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(ImmutableList<Period> schedule) {

		public record Period(ZonedDateTime timestamp, double price, int mode, int grid, int production, int consumption,
				int managedConsumption) {

			/**
			 * Returns a {@link JsonSerializer} for a {@link GetSchedule.Response.Period}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<GetSchedule.Response.Period> serializer() {
				return jsonObjectSerializer(GetSchedule.Response.Period.class, json -> {
					return new Period(//
							json.getZonedDateTime("timestamp"), //
							json.getDouble("price"), //
							json.getInt("mode"), //
							json.getInt("grid"), //
							json.getInt("production"), //
							json.getInt("consumption"), //
							json.getInt("managedConsumption") //
					);
				}, obj -> {
					return buildJsonObject() //
							.addProperty("timestamp", obj.timestamp) //
							.addProperty("price", obj.price) //
							.addProperty("mode", obj.mode) //
							.addProperty("grid", obj.grid) //
							.addProperty("production", obj.production) //
							.addProperty("consumption", obj.consumption) //
							.addProperty("managedConsumption", obj.managedConsumption) //
							.build();
				});
			}
		}

		/**
		 * Creates a {@link GetSchedule.Response}.
		 * 
		 * @param eshEvseSingle the {@link EshEvseSingle}
		 * @return the created {@link GetSchedule.Response}
		 */
		public static Response create(EshEvseSingle eshEvseSingle) {
			final Stream<Period> future;
			if (eshEvseSingle.smartEnergyScheduleHandler() != null) {
				// TODO historic
				future = toPeriodsStream(//
						eshEvseSingle.smartEnergyScheduleHandler().getParentId(), //
						eshEvseSingle.smartEnergyScheduleHandler().getSchedule(), //
						(p, managedCons) -> managedCons > 0 //
								? p.mode() //
								: Mode.Actual.ZERO);

			} else if (eshEvseSingle.manualEnergyScheduleHandler() != null) {
				future = toPeriodsStream(//
						eshEvseSingle.manualEnergyScheduleHandler().getParentId(), //
						eshEvseSingle.manualEnergyScheduleHandler().getSchedule(), //
						(p, managedCons) -> managedCons > 0 && p.coc().combinedAbilities().isReadyForCharging() //
								? p.coc().mode() //
								: Mode.Actual.ZERO);

			} else {
				future = Stream.of();
			}

			return new Response(future.collect(toImmutableList()));
		}

		private static <PERIOD extends EnergyScheduleHandler.Period<?>> Stream<Response.Period> toPeriodsStream(
				String parentId, ImmutableSortedMap<ZonedDateTime, PERIOD> schedule,
				BiFunction<PERIOD, Integer, Mode.Actual> modeFunction) {
			return schedule.entrySet().stream() //
					.map(e -> {
						var p = e.getValue();
						final IntUnaryOperator convertEnergyToPower = i -> p.duration().convertEnergyToPower(i);
						var managedCons = p.energyFlow().getManagedConsumption(parentId);
						return new Response.Period(e.getKey(), p.price(), //
								modeFunction.apply(p, managedCons).getValue(), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getGrid()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getProduction()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getConsumption()), //
								convertEnergyToPower.applyAsInt(managedCons));
					});
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetSchedule.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetSchedule.Response> serializer() {
			return jsonObjectSerializer(GetSchedule.Response.class, //
					json -> new GetSchedule.Response(//
							json.getImmutableList("schedule", Period.serializer())),
					obj -> buildJsonObject() //
							.add("schedule", Period.serializer().toListSerializer().serialize(obj.schedule())) //
							.build());
		}
	}
}
