package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.optimizer.app.AppUtils.period;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.edge.energy.EnergySchedulerTestUtils.Controller;
import io.openems.edge.energy.api.RiskLevel;

/**
 * This little application allows running the Optimizer from a mocked JSON
 * definition.
 */
public class RunOptimizerApp {

	// TODO log when only one-mode ctrls
	private static final long EXECUTION_LIMIT_SECONDS = 10;

	private static final JsonObject JSON = buildJsonObject() //
			.addProperty("startTime", ZonedDateTime.parse("2025-03-17T07:45:00Z")) //
			.addProperty("riskLevel", RiskLevel.MEDIUM) //
			.add("grid", buildJsonObject() //
					.addProperty("maxBuyPower", 10000) //
					.addProperty("maxSellPower", 5000) //
					.build()) //
			.add("ess", buildJsonObject() //
					.addProperty("currentEnergy", 11000) //
					.addProperty("totalEnergy", 22000) //
					.addProperty("maxChargePower", 6000) //
					.addProperty("maxDischargePower", 6000) //
					.build()) //
			.add("eshs", buildJsonArray() //
					// ESS Fix-Active-Power
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_FIX_ACTIVE_POWER.factoryPid) //
							.addProperty("id", "ctrlFixActivePower0") //
							.add("source", buildJsonObject() //
									.addProperty("power", 500) //
									.addProperty("relationship", "EQUALS") //
									.build()) //
							.build())

					// ESS Limit-Total-Discharge
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_LIMIT_TOTAL_DISCHARGE.factoryPid) //
							.addProperty("id", "ctrlLimitTotalDischarge0") //
							.add("source", buildJsonObject() //
									.addProperty("minSoc", 10) //
									.build()) //
							.build())

					// ESS Emergency-Capacity-Reserve
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_EMERGENCY_CAPACITY_RESERVE.factoryPid) //
							.addProperty("id", "ctrlEmergencyCapacityReserve0") //
							.add("source", buildJsonObject() //
									.addProperty("minSoc", 100) //
									.build()) //
							.build())

					// ESS Grid-Optimized-Charge in MANUAL mode
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_GRID_OPTIMIZED_CHARGE.factoryPid) //
							.addProperty("id", "ctrlGridOptimizedCharge0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "Manual") //
									.addProperty("targetTime", "13:00") //
									.build()) //
							.build())

					// ESS Grid-Optimized-Charge in AUTOMATIC mode
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_GRID_OPTIMIZED_CHARGE.factoryPid) //
							.addProperty("id", "ctrlGridOptimizedCharge0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "Automatic") //
									.build()) //
							.build())

					// EVSE in MANUAL mode
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.EVSE_SINGLE.factoryPid) //
							.addProperty("id", "ctrlEvseSingle0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "ManualOptimizationContext") //
									.addProperty("isReadyForCharging", true) //
									.addProperty("appearsToBeFullyCharged", false) //
									.add("limit", buildJsonObject() //
											.addProperty("phase", "SINGLE_PHASE") //
											.addProperty("minCurrent", 6000) //
											.addProperty("maxCurrent", 32000) //
											.build()) //
									.addProperty("mode", "MINIMUM") //
									.addProperty("sessionEnergy", 0) //
									.addProperty("sessionEnergyLimit", 15000) //
									.build()) //
							.build())

					// EVSE in SMART mode
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.EVSE_SINGLE.factoryPid) //
							.addProperty("id", "ctrlEvseSingle0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "SmartOptimizationConfig") //
									.addProperty("isReadyForCharging", true) //
									.addProperty("appearsToBeFullyCharged", false) //
									.add("limit", buildJsonObject() //
											.addProperty("phase", "SINGLE_PHASE") //
											.addProperty("minCurrent", 6000) //
											.addProperty("maxCurrent", 32000) //
											.build()) //
									.add("smartConfig", buildJsonArray() //
											.add(buildJsonObject() //
													.addProperty("@type", "Task") //
													.addProperty("start", "12:00:00") //
													.add("recurrenceRules", buildJsonArray() //
															.add(buildJsonObject() //
																	.addProperty("frequency", "daily") //
																	.build())
															.build())
													.add("openems.io:payload", buildJsonObject() //
															.addProperty("sessionEnergyMinimum", 10000) //
															.build())
													.build())
											.build()) //
									.add("targetPayload", buildJsonObject() //
											.addProperty("sessionEnergyMinimum", 15000) //
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

			.add("periods", buildJsonArray() //
					// time | production | consumption | price
					.add(period("07:45", 291.3, 305.0, 367.0)) //
					.add(period("08:00", 258.4, 351.0, 345.0)) //
					.add(period("08:15", 258.4, 932.0, 1490.0)) //
					.add(period("08:30", 258.4, 1087.0, 1523.0)) //
					.add(period("08:45", 258.4, 1258.0, 1535.0)) //
					.add(period("09:00", 215.4, 1372.0, 1480.0)) //
					.add(period("09:15", 215.4, 1388.0, 1567.0)) //
					.add(period("09:30", 215.4, 1533.0, 1065.0)) //
					.add(period("09:45", 215.4, 1808.0, 503.0)) //
					.add(period("10:00", 192.6, 1951.0, 794.0)) //
					.add(period("10:15", 192.6, 1918.0, 906.0)) //
					.add(period("10:30", 192.6, 2072.0, 868.0)) //
					.add(period("10:45", 192.6, 2222.0, 1781.0)) //
					.add(period("11:00", 180.7, 2370.0, 973.0)) //
					.add(period("11:15", 180.7, 2476.0, 853.0)) //
					.add(period("11:30", 180.7, 2552.0, 996.0)) //
					.add(period("11:45", 180.7, 2581.0, 979.0)) //
					.add(period("12:00", 177.9, 2598.0, 1231.0)) //
					.add(period("12:15", 177.9, 2604.0, 1431.0)) //
					.add(period("12:30", 177.9, 2603.0, 1100.0)) //
					.add(period("12:45", 177.9, 2583.0, 895.0)) //
					.add(period("13:00", 178.2, 2596.0, 904.0)) //
					.add(period("13:15", 178.2, 2603.0, 1025.0)) //
					.add(period("13:30", 178.2, 2594.0, 781.0)) //
					.add(period("13:45", 178.2, 2516.0, 838.0)) //
					.add(period("14:00", 180.7, 2431.0, 931.0)) //
					.add(period("14:15", 180.7, 2325.0, 654.0)) //
					.add(period("14:30", 180.7, 2153.0, 613.0)) //
					.add(period("14:45", 180.7, 2087.0, 1117.0)) //
					.add(period("15:00", 201.3, 2021.0, 987.0)) //
					.add(period("15:15", 201.3, 1864.0, 1554.0)) //
					.add(period("15:30", 201.3, 1653.0, 1692.0)) //
					.add(period("15:45", 201.3, 1581.0, 970.0)) //
					.add(period("16:00", 288.4, 1360.0, 803.0)) //
					.add(period("16:15", 288.4, 1262.0, 676.0)) //
					.add(period("16:30", 288.4, 1148.0, 1395.0)) //
					.add(period("16:45", 288.4, 985.0, 975.0)) //
					.add(period("17:00", 330.2, 743.0, 397.0)) //
					.add(period("17:15", 330.2, 516.0, 604.0)) //
					.add(period("17:30", 330.2, 224.0, 892.0)) //
					.add(period("17:45", 330.2, 68.0, 1113.0)) //
					.add(period("18:00", 341.7, 13.0, 590.0)) //
					.add(period("18:15", 341.7, 2.0, 832.0)) //
					.add(period("18:30", 341.7, 0.0, 726.0)) //
					.add(period("18:45", 341.7, 0.0, 394.0)) //
					.add(period("19:00", 343.3, 0.0, 950.0)) //
					.add(period("19:15", 343.3, 0.0, 662.0)) //
					.add(period("19:30", 343.3, 0.0, 394.0)) //
					.add(period("19:45", 343.3, 0.0, 728.0)) //
					.add(period("20:00", 335.6, 0.0, 492.0)) //
					.add(period("20:15", 335.6, 0.0, 1320.0)) //
					.add(period("20:30", 335.6, 0.0, 663.0)) //
					.add(period("20:45", 335.6, 0.0, 432.0)) //
					.add(period("21:00", 326.6, 0.0, 579.0)) //
					.add(period("21:15", 326.6, 0.0, 430.0)) //
					.add(period("21:30", 326.6, 0.0, 421.0)) //
					.add(period("21:45", 326.6, 0.0, 542.0)) //
					.add(period("22:00", 314.9, 0.0, 703.0)) //
					.add(period("22:15", 314.9, 0.0, 656.0)) //
					.add(period("22:30", 314.9, 0.0, 476.0)) //
					.add(period("22:45", 314.9, 0.0, 446.0)) //
					.add(period("23:00", 307.7, 0.0, 450.0)) //
					.add(period("23:15", 307.7, 0.0, 487.0)) //
					.add(period("23:30", 307.7, 0.0, 554.0)) //
					.add(period("23:45", 307.7, 0.0, 434.0)) //
					.build())
			.build();

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		AppUtils.simulateFromJson(JSON, EXECUTION_LIMIT_SECONDS);
	}
}