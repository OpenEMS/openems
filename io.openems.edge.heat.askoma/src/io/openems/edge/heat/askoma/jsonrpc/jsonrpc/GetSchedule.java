package io.openems.edge.heat.askoma.jsonrpc.jsonrpc;

//TODO replace with io.openems.edge.energy/src/io/openems/edge/energy/GetSchedule.java

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.heat.askoma.EnergyScheduler;
import io.openems.edge.heat.askoma.Mode;

/**
 * Gets a Schedule.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "componentId": string
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
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
public class GetSchedule implements EndpointRequestType<GetSchedule.Request, GetSchedule.Response> {

	@Override
	public String getMethod() {
		return "getSchedule";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String componentId //
	) {

		/**
		 * serializer.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("componentId")), //
					obj -> buildJsonObject() //
							.addProperty("componentId", obj.componentId()) //
							.build());
		}

	}

	public record Response(ImmutableList<Period> schedule) {

		public record Period(ZonedDateTime timestamp, Double price, int mode, int grid, int production, int consumption,
				int managedConsumption) {

			/**
			 * Returns a {@link JsonSerializer} for a {@link Period}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<Period> serializer() {
				return jsonObjectSerializer(Period.class, json -> new Period(//
						json.getZonedDateTime("timestamp"), //
						json.getDouble("price"), //
						json.getInt("mode"), //
						json.getInt("grid"), //
						json.getInt("production"), //
						json.getInt("consumption"), //
						json.getInt("managedConsumption") //
				), obj -> buildJsonObject() //
						.addProperty("timestamp", obj.timestamp) //
						.addProperty("price", obj.price) //
						.addProperty("mode", obj.mode) //
						.addProperty("grid", obj.grid) //
						.addProperty("production", obj.production) //
						.addProperty("consumption", obj.consumption) //
						.addProperty("managedConsumption", obj.managedConsumption) //
						.build());
			}
		}

		/**
		 * Creates a {@link Response}.
		 * 
		 * @param request the {@link Request}
		 * @param esh     the {@link EnergyScheduleHandler}
		 * 
		 * @return the created {@link Response}
		 */
		public static Response create(Request request,
				EshWithDifferentModes<Mode, EnergyScheduler.OptimizationContext, Void> esh) {
			return new Response(esh.getSchedule().entrySet().stream() //
					.map(e -> {
						final var componentId = request.componentId;
						final var p = e.getValue();
						final IntUnaryOperator convertEnergyToPower = i -> p.duration().convertEnergyToPower(i);
						final var mode = Optional.ofNullable(//
								p.mode())
								.orElse(p.coc().defaultMode());

						return new Period(e.getKey(), //
								p.gridBuyPrice(), //
								mode.ordinal(), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getGrid()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getProduction()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getConsumption()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getManagedConsumption(componentId)));
					}) //
					.collect(toImmutableList()));
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getImmutableList("schedule", Period.serializer())),
					obj -> buildJsonObject() //
							.add("schedule", Period.serializer().toListSerializer().serialize(obj.schedule())) //
							.build());
		}
	}
}
