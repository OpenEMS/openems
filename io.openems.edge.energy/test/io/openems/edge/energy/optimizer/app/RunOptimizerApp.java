package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.DateUtils.QUARTERS_PER_DAY;
import static io.openems.common.utils.DateUtils.TIME_FORMATTER;
import static io.openems.common.utils.DateUtils.toQuarterIndex;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.optimizer.app.AppUtils.period;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.energy.EnergySchedulerTestUtils.Controller;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.optimizer.app.PlotUtils.PlotSettings;

/**
 * This little application allows running the Optimizer from a mocked JSON
 * definition.
 */
public class RunOptimizerApp {

	// TODO log when only one-mode ctrls
	private static final long EXECUTION_LIMIT_SECONDS = 10;

	private static final PlotSettings PLOT_SETTINGS = PlotSettings.SIMULATION_RESULT;

	private static final JsonObject JSON = buildJsonObject() //
			.addProperty("zone", "Europe/Berlin") //
			.addProperty("startTime", ZonedDateTime.parse("2025-03-17T07:45:00Z")) //
			.addProperty("environment", Environment.PRODUCTION) //
			.add("grid", buildJsonObject() //
					.addProperty("maxBuyPower", 100000) //
					.addProperty("maxSellPower", 100000) //
					.add("gridBuySoftLimit", buildJsonArray() //
							.add(buildJsonObject() //
									.addProperty("@type", "Task") //
									.addProperty("start", "08:00:00") //
									.addProperty("duration", "PT12H") //
									.add("recurrenceRules", buildJsonArray() //
											.add(buildJsonObject() //
													.addProperty("frequency", "daily") //
													.build()) //
											.build()) //
									.add("openems.io:payload", buildJsonObject() //
											.addProperty("power", 2000) //
											.build()) //
									.build()) //
							.add(buildJsonObject() //
									.addProperty("@type", "Task") //
									.add("openems.io:payload", buildJsonObject() //
											.addProperty("power", 6000) //
											.build()) //
									.build()) //
							.build()) //
					.build()) //
			.add("ess", buildJsonObject() //
					.addProperty("currentEnergy", 11000) //
					.addProperty("totalEnergy", 22000) //
					.addProperty("maxChargePower", 6000) //
					.addProperty("maxDischargePower", 6000) //
					.build()) //
			.add("eshs", buildJsonArray() //

					// ESS Fix-Active-Power
					// .add(buildJsonObject() //
					// .addProperty("factoryPid", Controller.ESS_FIX_ACTIVE_POWER.factoryPid) //
					// .addProperty("id", "ctrlFixActivePower0") //
					// .add("source", buildJsonObject() //
					// .addProperty("power", 1000) //
					// .addProperty("relationship", "EQUALS") //
					// .build()) //
					// .build())

					// ESS Limit-Total-Discharge
					// .add(buildJsonObject() //
					// .addProperty("factoryPid", Controller.ESS_LIMIT_TOTAL_DISCHARGE.factoryPid)
					// //
					// .addProperty("id", "ctrlLimitTotalDischarge0") //
					// .add("source", buildJsonObject() //
					// .addProperty("minSoc", 10) //
					// .build()) //
					// .build())

					// ESS Emergency-Capacity-Reserve
					// .add(buildJsonObject() //
					// .addProperty("factoryPid",
					// Controller.ESS_EMERGENCY_CAPACITY_RESERVE.factoryPid) //
					// .addProperty("id", "ctrlEmergencyCapacityReserve0") //
					// .add("source", buildJsonObject() //
					// .addProperty("minSoc", 100) //
					// .build()) //
					// .build())

					// ESS Grid-Optimized-Charge in MANUAL mode
					// .add(buildJsonObject() //
					// .addProperty("factoryPid", Controller.ESS_GRID_OPTIMIZED_CHARGE.factoryPid)
					// //
					// .addProperty("id", "ctrlGridOptimizedCharge0") //
					// .add("source", buildJsonObject() //
					// .addProperty("class", "Manual") //
					// .addProperty("targetTime", "13:00") //
					// .build()) //
					// .build())

					// ESS Grid-Optimized-Charge in AUTOMATIC mode
					// .add(buildJsonObject() //
					// .addProperty("factoryPid", Controller.ESS_GRID_OPTIMIZED_CHARGE.factoryPid)
					// //
					// .addProperty("id", "ctrlGridOptimizedCharge0") //
					// .add("source", buildJsonObject() //
					// .addProperty("class", "Automatic") //
					// .build()) //
					// .build())

					// EVSE Cluster
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.EVSE_CLUSTER.factoryPid) //
							.addProperty("id", "ctrlEvseCluster0") //
							.add("source", buildJsonObject() //
									.addProperty("distributionStrategy", "EQUAL_POWER") //
									.add("params", buildJsonArray() //
											.add(buildJsonObject() //
													.addProperty("componentId", "ctrlEvseSingle0") //
													.addProperty("mode", "SURPLUS") //
													.addProperty("activePower", 0) //
													.addProperty("sessionEnergy", 0) //
													.addProperty("sessionEnergyLimit", 10000) //
													.addProperty("history", "") //
													.addProperty("phaseSwitching", "DISABLE") //
													.add("combinedAbilities", buildJsonObject() //
															.add("chargePointAbilities", buildJsonObject() //
																	.add("applySetPoint", buildJsonObject() //
																			.addProperty("class", "MilliAmpere") //
																			.addProperty("phase", "THREE_PHASE") //
																			.addProperty("min", 6000) //
																			.addProperty("max", 16000) //
																			.build()) //
																	.add("phaseSwitch", JsonNull.INSTANCE) //
																	.addProperty("isEvConnected", true) //
																	.addProperty("isReadyForCharging", true) //
																	.build()) //
															.add("electricVehicleAbilities", buildJsonObject() //
																	.add("singlePhaseLimit", buildJsonObject() //
																			.addProperty("class", "Watt") //
																			.addProperty("phase", "SINGLE_PHASE") //
																			.addProperty("min", 1380) //
																			.addProperty("max", 7360) //
																			.addProperty("step", 1) //
																			.build()) //
																	.add("threePhaseLimit", buildJsonObject() //
																			.addProperty("class", "Watt") //
																			.addProperty("phase", "THREE_PHASE") //
																			.addProperty("min", 4140) //
																			.addProperty("max", 11040) //
																			.addProperty("step", 1) //
																			.build()) //
																	.addProperty("canInterrupt", true) //
																	.build()) //
															.addProperty("isReadyForCharging", true) //
															.add("applySetPoint", buildJsonObject() //
																	.addProperty("class", "Watt") //
																	.addProperty("phase", "THREE_PHASE") //
																	.addProperty("min", 4140) //
																	.addProperty("max", 11040) //
																	.addProperty("step", 1) //
																	.build()) //
															.add("phaseSwitch", JsonNull.INSTANCE) //
															.build()) //
													.add("tasks", buildJsonArray() //
															.build()) //
													.build()) //
											.add(buildJsonObject() //
													.addProperty("componentId", "ctrlEvseSingle1") //
													.addProperty("mode", "ZERO") //
													.addProperty("activePower", 0) //
													.addProperty("sessionEnergy", 0) //
													.addProperty("sessionEnergyLimit", 0) //
													.addProperty("history", "") //
													.addProperty("phaseSwitching", "DISABLE") //
													.add("combinedAbilities", buildJsonObject() //
															.add("chargePointAbilities", buildJsonObject() //
																	.add("applySetPoint", buildJsonObject() //
																			.addProperty("class", "MilliAmpere") //
																			.addProperty("phase", "THREE_PHASE") //
																			.addProperty("min", 6000) //
																			.addProperty("max", 16000) //
																			.build()) //
																	.add("phaseSwitch", JsonNull.INSTANCE) //
																	.addProperty("isEvConnected", true) //
																	.addProperty("isReadyForCharging", true) //
																	.build()) //
															.add("electricVehicleAbilities", buildJsonObject() //
																	.add("singlePhaseLimit", buildJsonObject() //
																			.addProperty("class", "Watt") //
																			.addProperty("phase", "SINGLE_PHASE") //
																			.addProperty("min", 1380) //
																			.addProperty("max", 7360) //
																			.addProperty("step", 1) //
																			.build()) //
																	.add("threePhaseLimit", buildJsonObject() //
																			.addProperty("class", "Watt") //
																			.addProperty("phase", "THREE_PHASE") //
																			.addProperty("min", 4140) //
																			.addProperty("max", 11040) //
																			.addProperty("step", 1) //
																			.build()) //
																	.addProperty("canInterrupt", true) //
																	.build()) //
															.addProperty("isReadyForCharging", true) //
															.add("applySetPoint", buildJsonObject() //
																	.addProperty("class", "Watt") //
																	.addProperty("phase", "THREE_PHASE") //
																	.addProperty("min", 4140) //
																	.addProperty("max", 11040) //
																	.addProperty("step", 1) //
																	.build()) //
															.add("phaseSwitch", JsonNull.INSTANCE) //
															.build()) //
													.add("tasks", buildJsonArray() //
															.add(buildJsonObject() //
																	.addProperty("@type", "Task") //
																	.addProperty("start", "13:00:00") //
																	.addProperty("duration", "PT2H") //
																	.add("recurrenceRules", buildJsonArray() //
																			.add(buildJsonObject() //
																					.addProperty("frequency", "daily") //
																					.build())
																			.build())
																	.add("openems.io:payload", buildJsonObject() //
																			.addProperty("class", "Manual") //
																			.addProperty("mode", "FORCE") //
																			.build())
																	.build())
															.build()) //
													.build()) //
											.build()) //
									.build()) //
							.build())

					// ESS Time-of-Use-Tariff-Optimization
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_TIME_OF_USE_TARIFF.factoryPid) //
							.addProperty("id", "ctrlEssTimeOfUseTariff0") //
							.add("source", buildJsonObject() //
									.addProperty("controlMode", "CHARGE_CONSUMPTION") //
									.build()) //
							.build())

					.build()) //

			.add("periods", getTestData()) //
			.build();

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		AppUtils.simulateFromJson(JSON, EXECUTION_LIMIT_SECONDS, PLOT_SETTINGS);
	}

	protected static JsonArray getTestData() {
		var startTimeDayOne = LocalTime.of(0, 0);
		var prodDayOne = ProductionTestData.PRODUCTION_WINTER_CLEAR;
		var consDayOne = ConsumptionTestData.CONSUMPTION;
		var pricesDayOne = PricesTestData.PRICES_TIBBER_WINTER_CLEAR;

		var endTimeDayTwo = LocalTime.of(23, 45);
		var prodDayTwo = ProductionTestData.PRODUCTION_WINTER_CLEAR;
		var consDayTwo = ConsumptionTestData.CONSUMPTION;
		var pricesDayTwo = PricesTestData.PRICES_TIBBER_WINTER_CLEAR;

		var dayOneStream = IntStream.range(toQuarterIndex(startTimeDayOne), QUARTERS_PER_DAY)//
				.mapToObj(i -> {
					var time = LocalTime.MIN.plusMinutes(i * 15L);
					return period(//
							time.format(TIME_FORMATTER), //
							null, //
							prodDayOne[i], //
							consDayOne[i], //
							pricesDayOne[i]);
				});

		var dayTwoStream = IntStream.rangeClosed(0, toQuarterIndex(endTimeDayTwo))//
				.mapToObj(i -> {
					var time = LocalTime.MIN.plusMinutes(i * 15L);
					return period(//
							time.format(TIME_FORMATTER), //
							null, //
							prodDayTwo[i], //
							consDayTwo[i], //
							pricesDayTwo[i]);
				});

		return Stream.concat(dayOneStream, dayTwoStream)//
				.collect(new JsonUtils.JsonArrayCollector());
	}

	public static JsonArray getCustomTestData() {
		return buildJsonArray()//
				// time | gridBuySoftLimit | production | consumption | price
				// Day One
				.add(period("00:00", null, 0.0, 0.0, 210.0)) //
				.add(period("00:15", null, 0.0, 0.0, 210.0)) //
				.add(period("00:30", null, 0.0, 0.0, 210.0)) //
				.add(period("00:45", null, 0.0, 291.3, 210.0)) //
				.add(period("01:00", null, 0.0, 291.3, 210.0)) //
				.add(period("01:15", null, 0.0, 291.3, 210.0)) //
				.add(period("01:30", null, 0.0, 291.3, 210.0)) //
				.add(period("01:45", null, 0.0, 291.3, 210.0)) //
				.add(period("02:00", null, 0.0, 291.3, 210.0)) //
				.add(period("02:15", null, 0.0, 291.3, 210.0)) //
				.add(period("02:30", null, 0.0, 291.3, 210.0)) //
				.add(period("02:45", null, 0.0, 291.3, 210.0)) //
				.add(period("03:00", null, 0.0, 291.3, 210.0)) //
				.add(period("03:15", null, 0.0, 291.3, 210.0)) //
				.add(period("03:30", null, 0.0, 291.3, 210.0)) //
				.add(period("03:45", null, 0.0, 251.3, 210.0)) //
				.add(period("04:00", null, 0.0, 217.3, 210.0)) //
				.add(period("04:15", null, 0.0, 217.3, 210.0)) //
				.add(period("04:30", null, 0.0, 217.3, 210.0)) //
				.add(period("04:45", null, 0.0, 217.3, 210.0)) //
				.add(period("05:00", null, 0.0, 217.3, 210.0)) //
				.add(period("05:15", null, 0.0, 217.3, 210.0)) //
				.add(period("05:30", null, 0.0, 217.3, 210.0)) //
				.add(period("05:45", null, 0.0, 217.3, 210.0)) //
				.add(period("06:00", null, 0.0, 217.3, 210.0)) //
				.add(period("06:15", null, 0.0, 217.3, 210.0)) //
				.add(period("06:30", null, 0.0, 217.3, 210.0)) //
				.add(period("06:45", null, 0.0, 217.3, 310.0)) //
				.add(period("07:00", null, 0.0, 217.3, 310.0)) //
				.add(period("07:15", null, 0.0, 217.3, 310.0)) //
				.add(period("07:30", null, 0.0, 217.3, 310.0)) //
				.add(period("07:45", null, 0.0, 217.3, 310.0)) //
				.add(period("08:00", null, 0.0, 20.4, 310.0)) //
				.add(period("08:15", null, 0.0, 28.4, 310.0)) //
				.add(period("08:30", null, 0.0, 28.4, 310.0)) //
				.add(period("08:45", null, 0.0, 28.4, 310.0)) //
				.add(period("09:00", null, 0.0, 25.4, 310.0)) //
				.add(period("09:15", null, 0.0, 215.4, 310.0)) //
				.add(period("09:30", null, 0.0, 25.4, 310.0)) //
				.add(period("09:45", null, 0.0, 25.4, 310.0)) //
				.add(period("10:00", null, 0.0, 120.6, 310.0)) //
				.add(period("10:15", null, 0.0, 120.6, 310.0)) //
				.add(period("10:30", null, 0.0, 120.6, 310.0)) //
				.add(period("10:45", null, 0.0, 120.6, 310.0)) //
				.add(period("11:00", null, 0.0, 100.7, 310.0)) //
				.add(period("11:15", null, 0.0, 100.7, 310.0)) //
				.add(period("11:30", null, 0.0, 100.7, 310.0)) //
				.add(period("11:45", null, 0.0, 100.7, 310.0)) //
				.add(period("12:00", null, 0.0, 170.9, 310.0)) //
				.add(period("12:15", null, 0.0, 170.9, 310.0)) //
				.add(period("12:30", null, 0.0, 170.9, 310.0)) //
				.add(period("12:45", null, 0.0, 170.9, 310.0)) //
				.add(period("13:00", null, 0.0, 180.2, 310.0)) //
				.add(period("13:15", null, 0.0, 180.2, 310.0)) //
				.add(period("13:30", null, 0.0, 180.2, 310.0)) //
				.add(period("13:45", null, 0.0, 180.2, 310.0)) //
				.add(period("14:00", null, 0.0, 100.7, 310.0)) //
				.add(period("14:15", null, 0.0, 180.7, 310.0)) //
				.add(period("14:30", null, 0.0, 180.7, 310.0)) //
				.add(period("14:45", null, 0.0, 18.7, 310.0)) //
				.add(period("15:00", null, 0.0, 20.3, 310.0)) //
				.add(period("15:15", null, 0.0, 20.3, 310.0)) //
				.add(period("15:30", null, 0.0, 20.3, 310.0)) //
				.add(period("15:45", null, 0.0, 201.3, 310.0)) //
				.add(period("16:00", null, 0.0, 280.4, 310.0)) //
				.add(period("16:15", null, 0.0, 280.4, 310.0)) //
				.add(period("16:30", null, 0.0, 280.4, 310.0)) //
				.add(period("16:45", null, 0.0, 280.4, 310.0)) //
				.add(period("17:00", null, 0.0, 330.2, 310.0)) //
				.add(period("17:15", null, 0.0, 330.2, 310.0)) //
				.add(period("17:30", null, 0.0, 330.2, 310.0)) //
				.add(period("17:45", null, 0.0, 330.2, 310.0)) //
				.add(period("18:00", null, 0.0, 310.7, 310.0)) //
				.add(period("18:15", null, 0.0, 310.7, 310.0)) //
				.add(period("18:30", null, 0.0, 341.7, 310.0)) //
				.add(period("18:45", null, 0.0, 341.7, 310.0)) //
				.add(period("19:00", null, 0.0, 343.3, 310.0)) //
				.add(period("19:15", null, 0.0, 343.3, 310.0)) //
				.add(period("19:30", null, 0.0, 343.3, 310.0)) //
				.add(period("19:45", null, 0.0, 343.3, 310.0)) //
				.add(period("20:00", null, 0.0, 335.6, 310.0)) //
				.add(period("20:15", null, 0.0, 335.6, 310.0)) //
				.add(period("20:30", null, 0.0, 335.6, 310.0)) //
				.add(period("20:45", null, 0.0, 335.6, 310.0)) //
				.add(period("21:00", null, 0.0, 326.6, 310.0)) //
				.add(period("21:15", null, 0.0, 326.6, 310.0)) //
				.add(period("21:30", null, 0.0, 326.6, 310.0)) //
				.add(period("21:45", null, 0.0, 326.6, 310.0)) //
				.add(period("22:00", null, 0.0, 314.9, 310.0)) //
				.add(period("22:15", null, 0.0, 314.9, 310.0)) //
				.add(period("22:30", null, 0.0, 314.9, 310.0)) //
				.add(period("22:45", null, 0.0, 314.9, 310.0)) //
				.add(period("23:00", null, 0.0, 307.7, 310.0)) //
				.add(period("23:15", null, 0.0, 307.7, 310.0)) //
				.add(period("23:30", null, 0.0, 307.7, 310.0)) //
				.add(period("23:45", null, 0.0, 307.7, 310.0)) //
				// Day Two
				.add(period("00:00", null, 0.0, 0.0, 120.0)) //
				.add(period("00:15", null, 0.0, 0.0, 120.0)) //
				.add(period("00:30", null, 0.0, 0.0, 120.0)) //
				.add(period("00:45", null, 0.0, 291.3, 120.0)) //
				.add(period("01:00", null, 0.0, 291.3, 120.0)) //
				.add(period("01:15", null, 0.0, 291.3, 120.0)) //
				.add(period("01:30", null, 0.0, 291.3, 120.0)) //
				.add(period("01:45", null, 0.0, 291.3, 120.0)) //
				.add(period("02:00", null, 0.0, 291.3, 120.0)) //
				.add(period("02:15", null, 0.0, 291.3, 120.0)) //
				.add(period("02:30", null, 0.0, 291.3, 120.0)) //
				.add(period("02:45", null, 0.0, 291.3, 120.0)) //
				.add(period("03:00", null, 0.0, 291.3, 120.0)) //
				.add(period("03:15", null, 0.0, 291.3, 120.0)) //
				.add(period("03:30", null, 0.0, 291.3, 120.0)) //
				.add(period("03:45", null, 0.0, 291.3, 120.0)) //
				.add(period("04:00", null, 0.0, 291.3, 120.0)) //
				.add(period("04:15", null, 0.0, 291.3, 120.0)) //
				.add(period("04:30", null, 0.0, 291.3, 120.0)) //
				.add(period("04:45", null, 0.0, 291.3, 120.0)) //
				.add(period("05:00", null, 0.0, 291.3, 120.0)) //
				.add(period("05:15", null, 0.0, 291.3, 120.0)) //
				.add(period("05:30", null, 0.0, 291.3, 120.0)) //
				.add(period("05:45", null, 0.0, 291.3, 120.0)) //
				.add(period("06:00", null, 0.0, 291.3, 120.0)) //
				.add(period("06:15", null, 0.0, 291.3, 120.0)) //
				.add(period("06:30", null, 0.0, 291.3, 120.0)) //
				.add(period("06:45", null, 0.0, 291.3, 310.0)) //
				.add(period("07:00", null, 0.0, 291.3, 310.0)) //
				.add(period("07:15", null, 0.0, 291.3, 310.0)) //
				.add(period("07:30", null, 0.0, 291.3, 310.0)) //
				.add(period("07:45", null, 0.0, 291.3, 310.0)) //
				.add(period("08:00", null, 0.0, 250.4, 310.0)) //
				.add(period("08:15", null, 10.0, 258.4, 310.0)) //
				.add(period("08:30", null, 20.0, 258.4, 310.0)) //
				.add(period("08:45", null, 30.0, 258.4, 310.0)) //
				.add(period("09:00", null, 40.0, 215.4, 310.0)) //
				.add(period("09:15", null, 60.0, 215.4, 310.0)) //
				.add(period("09:30", null, 80.0, 215.4, 310.0)) //
				.add(period("09:45", null, 110.0, 215.4, 310.0)) //
				.add(period("10:00", null, 120.0, 192.6, 310.0)) //
				.add(period("10:15", null, 120.0, 192.6, 310.0)) //
				.add(period("10:30", null, 130.0, 192.6, 310.0)) //
				.add(period("10:45", null, 130.0, 192.6, 310.0)) //
				.add(period("11:00", null, 170.0, 180.7, 310.0)) //
				.add(period("11:15", null, 190.0, 180.7, 310.0)) //
				.add(period("11:30", null, 220.0, 180.7, 310.0)) //
				.add(period("11:45", null, 180.0, 180.7, 310.0)) //
				.add(period("12:00", null, 210.0, 177.9, 310.0)) //
				.add(period("12:15", null, 220.0, 177.9, 310.0)) //
				.add(period("12:30", null, 220.0, 177.9, 310.0)) //
				.add(period("12:45", null, 240.0, 177.9, 310.0)) //
				.add(period("13:00", null, 220.0, 178.2, 310.0)) //
				.add(period("13:15", null, 220.0, 178.2, 310.0)) //
				.add(period("13:30", null, 210.0, 178.2, 310.0)) //
				.add(period("13:45", null, 210.0, 178.2, 310.0)) //
				.add(period("14:00", null, 200.0, 180.7, 310.0)) //
				.add(period("14:15", null, 180.0, 180.7, 310.0)) //
				.add(period("14:30", null, 160.0, 180.7, 310.0)) //
				.add(period("14:45", null, 140.0, 1800.7, 310.0)) //
				.add(period("15:00", null, 120.0, 2010.3, 310.0)) //
				.add(period("15:15", null, 100.0, 2010.3, 310.0)) //
				.add(period("15:30", null, 80.0, 2010.3, 310.0)) //
				.add(period("15:45", null, 80.0, 201.3, 310.0)) //
				.add(period("16:00", null, 60.0, 280.4, 310.0)) //
				.add(period("16:15", null, 50.0, 280.4, 310.0)) //
				.add(period("16:30", null, 40.0, 2880.4, 310.0)) //
				.add(period("16:45", null, 20.0, 2880.4, 310.0)) //
				.add(period("17:00", null, 0.0, 330.2, 310.0)) //
				.add(period("17:15", null, 0.0, 330.2, 310.0)) //
				.add(period("17:30", null, 0.0, 330.2, 310.0)) //
				.add(period("17:45", null, 0.0, 330.2, 310.0)) //
				.add(period("18:00", null, 0.0, 310.7, 310.0)) //
				.add(period("18:15", null, 0.0, 310.7, 310.0)) //
				.add(period("18:30", null, 0.0, 341.7, 310.0)) //
				.add(period("18:45", null, 0.0, 341.7, 310.0)) //
				.add(period("19:00", null, 0.0, 343.3, 310.0)) //
				.add(period("19:15", null, 0.0, 343.3, 310.0)) //
				.add(period("19:30", null, 0.0, 343.3, 310.0)) //
				.add(period("19:45", null, 0.0, 343.3, 310.0)) //
				.add(period("20:00", null, 0.0, 335.6, 310.0)) //
				.add(period("20:15", null, 0.0, 335.6, 310.0)) //
				.add(period("20:30", null, 0.0, 335.6, 310.0)) //
				.add(period("20:45", null, 0.0, 335.6, 310.0)) //
				.add(period("21:00", null, 0.0, 326.6, 310.0)) //
				.add(period("21:15", null, 0.0, 326.6, 310.0)) //
				.add(period("21:30", null, 0.0, 326.6, 310.0)) //
				.add(period("21:45", null, 0.0, 326.6, 310.0)) //
				.add(period("22:00", null, 0.0, 314.9, 310.0)) //
				.add(period("22:15", null, 0.0, 314.9, 310.0)) //
				.add(period("22:30", null, 0.0, 314.9, 310.0)) //
				.add(period("22:45", null, 0.0, 314.9, 310.0)) //
				.add(period("23:00", null, 0.0, 307.7, 310.0)) //
				.add(period("23:15", null, 0.0, 307.7, 310.0)) //
				.add(period("23:30", null, 0.0, 307.7, 310.0)) //
				.add(period("23:45", null, 0.0, 307.7, 310.0)) //
				.build();
	}
}