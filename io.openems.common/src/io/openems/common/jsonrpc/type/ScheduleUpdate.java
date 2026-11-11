package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.time.Instant;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

/**
 * Represents the current Energy Schedule together with the current optimization
 * status.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {
 *   "status": "OPTIMIZING_FUTURE",
 *   "startedAt": "2026-09-17T08:00:00Z",
 *   "reason": "Start of new period",
 *   "data": [
 *     {
 *       "timestamp": "2026-09-17T08:00:00Z",
 *       "type": "PREDICTION",
 *       "_sum": {
 *         "GridBuyPrice": 123.45,
 *         "ProductionActivePower": 1000,
 *         "ConsumptionActivePower": 2000,
 *         "UnmanagedConsumptionActivePower": 1500,
 *         "EssDischargePower": 500,
 *         "EssSoc": 70,
 *         "GridActivePower": 500
 *       },
 *       "eshs": [
 *         {
 *           "id": "ctrlEssTimeOfUseTariff0",
 *           "mode": 1
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public record ScheduleUpdate(//
		OptimizationStatus status, //
		Instant startedAt, //
		String reason, //
		ImmutableList<Period> periods) {

	/**
	 * Public status of the current optimization.
	 */
	public enum OptimizationStatus {

		/**
		 * No optimization is currently running or no optimization result is available
		 * yet.
		 */
		INACTIVE, //

		/**
		 * The current period is being reoptimized and may still change.
		 */
		REOPTIMIZING_CURRENT, //

		/**
		 * The current period is fixed and only future periods are being optimized.
		 */
		OPTIMIZING_FUTURE, //

		/**
		 * The current optimization run has finished.
		 */
		OPTIMIZED
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link ScheduleUpdate}.
	 *
	 * @return the serializer
	 */
	public static JsonSerializer<ScheduleUpdate> serializer() {
		return jsonObjectSerializer(ScheduleUpdate.class, //
				json -> new ScheduleUpdate(//
						json.getEnum("status", OptimizationStatus.class), //
						json.getInstantOrNull("startedAt"), //
						json.getStringOrNull("reason"), //
						json.getImmutableList("data", Period.serializer())), //
				obj -> buildJsonObject()//
						.addProperty("status", obj.status())//
						.addPropertyIfNotNull("startedAt", obj.startedAt())//
						.addPropertyIfNotNull("reason", obj.reason())//
						.add("data", obj.periods().stream()//
								.map(Period.serializer()::serialize)//
								.collect(toJsonArray()))//
						.build());
	}

	/**
	 * Identifies whether a {@link Period} represents historic or predicted data.
	 */
	public enum PeriodType {
		HISTORY, //
		PREDICTION
	}

	/**
	 * Represents one period in the Energy Schedule.
	 *
	 * @param timestamp timestamp of the period
	 * @param type      type of the period
	 * @param sumData   aggregated _sum data
	 * @param eshsData  schedule data of individual EnergyScheduleHandlers
	 */
	public record Period(//
			Instant timestamp, //
			PeriodType type, //
			SumData sumData, //
			ImmutableList<EshData> eshsData) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Period}.
		 *
		 * @return the serializer
		 */
		public static JsonSerializer<Period> serializer() {
			return jsonObjectSerializer(Period.class, //
					json -> new Period(//
							json.getInstant("timestamp"), //
							json.getEnum("type", PeriodType.class), //
							json.getObject("_sum", SumData.serializer()), //
							json.getImmutableList("eshs", EshData.serializer())), //
					obj -> buildJsonObject()//
							.addProperty("timestamp", obj.timestamp())//
							.addProperty("type", obj.type())//
							.add("_sum", SumData.serializer().serialize(obj.sumData()))//
							.add("eshs", obj.eshsData().stream()//
									.map(EshData.serializer()::serialize)//
									.collect(toJsonArray()))//
							.build());
		}
	}

	/**
	 * Aggregated _sum values for one {@link Period}.
	 *
	 * @param gridBuyPrice                    grid-buy price
	 * @param gridSellPrice                   grid-sell price
	 * @param productionActivePower           production active power
	 * @param consumptionActivePower          total consumption active power
	 * @param unmanagedConsumptionActivePower unmanaged consumption active power
	 * @param essDischargePower               ESS discharge power
	 * @param essSoc                          ESS state of charge
	 * @param gridActivePower                 grid active power
	 */
	public record SumData(//
			Double gridBuyPrice, //
			Double gridSellPrice, //
			Integer productionActivePower, //
			Integer consumptionActivePower, //
			Integer unmanagedConsumptionActivePower, //
			Integer essDischargePower, //
			Integer essSoc, //
			Integer gridActivePower) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SumData}.
		 *
		 * @return the serializer
		 */
		public static JsonSerializer<SumData> serializer() {
			return jsonObjectSerializer(SumData.class, //
					json -> new SumData(//
							json.getOptionalDouble("GridBuyPrice").orElse(null), //
							json.getOptionalDouble("GridSellPrice").orElse(null), //
							json.getOptionalInt("ProductionActivePower").orElse(null), //
							json.getOptionalInt("ConsumptionActivePower").orElse(null), //
							json.getOptionalInt("UnmanagedConsumptionActivePower").orElse(null), //
							json.getOptionalInt("EssDischargePower").orElse(null), //
							json.getOptionalInt("EssSoc").orElse(null), //
							json.getOptionalInt("GridActivePower").orElse(null)), //
					obj -> buildJsonObject()//
							.addPropertyIfNotNull("GridBuyPrice", obj.gridBuyPrice())//
							.addPropertyIfNotNull("GridSellPrice", obj.gridSellPrice())//
							.addPropertyIfNotNull("ProductionActivePower", obj.productionActivePower())//
							.addPropertyIfNotNull("ConsumptionActivePower", obj.consumptionActivePower())//
							.addPropertyIfNotNull("UnmanagedConsumptionActivePower",
									obj.unmanagedConsumptionActivePower())//
							.addPropertyIfNotNull("EssDischargePower", obj.essDischargePower())//
							.addPropertyIfNotNull("EssSoc", obj.essSoc())//
							.addPropertyIfNotNull("GridActivePower", obj.gridActivePower())//
							.build());
		}
	}

	/**
	 * Schedule data for one EnergyScheduleHandler.
	 *
	 * @param id                 component id
	 * @param mode               active or scheduled mode
	 * @param managedConsumption managed consumption active power
	 */
	public record EshData(//
			String id, //
			Integer mode, //
			Integer managedConsumption) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link EshData}.
		 *
		 * @return the serializer
		 */
		public static JsonSerializer<EshData> serializer() {
			return jsonObjectSerializer(EshData.class, //
					json -> new EshData(//
							json.getString("id"), //
							json.getOptionalInt("mode").orElse(null), //
							json.getOptionalInt("managedConsumption").orElse(null)), //
					obj -> buildJsonObject() //
							.addProperty("id", obj.id()) //
							.addPropertyIfNotNull("mode", obj.mode()) //
							.addPropertyIfNotNull("managedConsumption", obj.managedConsumption()) //
							.build());
		}
	}
}
