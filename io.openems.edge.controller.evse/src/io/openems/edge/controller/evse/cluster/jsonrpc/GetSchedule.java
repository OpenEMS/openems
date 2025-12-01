package io.openems.edge.controller.evse.cluster.jsonrpc;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.ClusterScheduleContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.cluster.jsonrpc.GetSchedule.Request;
import io.openems.edge.controller.evse.cluster.jsonrpc.GetSchedule.Response;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;

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
public class GetSchedule implements EndpointRequestType<Request, Response> {

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

	public static record Request(//
			String componentId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
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
		 * @param request the {@link Request}
		 * @param esh     the {@link EnergyScheduleHandler}
		 * 
		 * @return the created {@link GetSchedule.Response}
		 */
		public static Response create(Request request,
				EshWithDifferentModes<SingleModes, OptimizationContext, ClusterScheduleContext> esh) {
			return new Response(esh.getSchedule().entrySet().stream() //
					.map(e -> {
						final var componentId = request.componentId;
						final var p = e.getValue();
						final IntUnaryOperator convertEnergyToPower = i -> p.duration().convertEnergyToPower(i);
						final var mode = Optional.ofNullable(//
								// Mode from Schedule
								p.mode().getMode(componentId))
								// Mode configured in Evse.Controller.Single
								.orElse(p.coc().clusterConfig().getSingleParams(componentId).mode().actual);
						return new Response.Period(e.getKey(), p.price(), //
								mode.getValue(), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getGrid()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getProduction()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getConsumption()), //
								convertEnergyToPower.applyAsInt(p.energyFlow().getManagedConsumption(componentId)));
					}) //
					.collect(toImmutableList()));
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
