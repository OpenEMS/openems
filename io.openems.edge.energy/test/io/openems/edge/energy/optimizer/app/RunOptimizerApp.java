package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.optimizer.app.AppUtils.period;

import java.time.ZonedDateTime;

import com.google.gson.JsonNull;
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
					.addProperty("maxBuyPower", 100000) //
					.addProperty("maxSellPower", 100000) //
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
									.addProperty("power", 1000) //
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
									.addProperty("mode", "MINIMUM") //
									.add("abilities", buildJsonObject() //
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
									.addProperty("appearsToBeFullyCharged", false) //
									.addProperty("sessionEnergy", 0) //
									.addProperty("sessionEnergyLimit", 10000) //
									.build()) //
							.build())

					// EVSE in SMART mode
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.EVSE_SINGLE.factoryPid) //
							.addProperty("id", "ctrlEvseSingle0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "SmartOptimizationConfig") //
									.add("abilities", buildJsonObject() //
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
									.addProperty("appearsToBeFullyCharged", false) //
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
					.add(period("07:45", 305.0, 291.3, 367.0)) //
					.add(period("08:00", 351.0, 258.4, 345.0)) //
					.add(period("08:15", 932.0, 258.4, 1490.0)) //
					.add(period("08:30", 1087.0, 258.4, 1523.0)) //
					.add(period("08:45", 1258.0, 258.4, 1535.0)) //
					.add(period("09:00", 1372.0, 215.4, 1480.0)) //
					.add(period("09:15", 1388.0, 215.4, 1567.0)) //
					.add(period("09:30", 1533.0, 215.4, 1065.0)) //
					.add(period("09:45", 1808.0, 215.4, 503.0)) //
					.add(period("10:00", 1951.0, 192.6, 794.0)) //
					.add(period("10:15", 1918.0, 192.6, 906.0)) //
					.add(period("10:30", 2072.0, 192.6, 868.0)) //
					.add(period("10:45", 2222.0, 192.6, 1781.0)) //
					.add(period("11:00", 2370.0, 180.7, 973.0)) //
					.add(period("11:15", 2476.0, 180.7, 853.0)) //
					.add(period("11:30", 2552.0, 180.7, 996.0)) //
					.add(period("11:45", 2581.0, 180.7, 979.0)) //
					.add(period("12:00", 2598.0, 177.9, 1231.0)) //
					.add(period("12:15", 2604.0, 177.9, 1431.0)) //
					.add(period("12:30", 2603.0, 177.9, 1100.0)) //
					.add(period("12:45", 2583.0, 177.9, 895.0)) //
					.add(period("13:00", 2596.0, 178.2, 904.0)) //
					.add(period("13:15", 2603.0, 178.2, 1025.0)) //
					.add(period("13:30", 2594.0, 178.2, 781.0)) //
					.add(period("13:45", 2516.0, 178.2, 838.0)) //
					.add(period("14:00", 2431.0, 180.7, 931.0)) //
					.add(period("14:15", 2325.0, 180.7, 654.0)) //
					.add(period("14:30", 2153.0, 180.7, 613.0)) //
					.add(period("14:45", 2087.0, 180.7, 1117.0)) //
					.add(period("15:00", 2021.0, 201.3, 987.0)) //
					.add(period("15:15", 1864.0, 201.3, 1554.0)) //
					.add(period("15:30", 1653.0, 201.3, 1692.0)) //
					.add(period("15:45", 1581.0, 201.3, 970.0)) //
					.add(period("16:00", 1360.0, 288.4, 803.0)) //
					.add(period("16:15", 1262.0, 288.4, 676.0)) //
					.add(period("16:30", 1148.0, 288.4, 1395.0)) //
					.add(period("16:45", 985.0, 288.4, 975.0)) //
					.add(period("17:00", 743.0, 330.2, 397.0)) //
					.add(period("17:15", 516.0, 330.2, 604.0)) //
					.add(period("17:30", 224.0, 330.2, 892.0)) //
					.add(period("17:45", 68.0, 330.2, 1113.0)) //
					.add(period("18:00", 13.0, 341.7, 590.0)) //
					.add(period("18:15", 2.0, 341.7, 832.0)) //
					.add(period("18:30", 0.0, 341.7, 726.0)) //
					.add(period("18:45", 0.0, 341.7, 394.0)) //
					.add(period("19:00", 0.0, 343.3, 950.0)) //
					.add(period("19:15", 0.0, 343.3, 662.0)) //
					.add(period("19:30", 0.0, 343.3, 394.0)) //
					.add(period("19:45", 0.0, 343.3, 728.0)) //
					.add(period("20:00", 0.0, 335.6, 492.0)) //
					.add(period("20:15", 0.0, 335.6, 1320.0)) //
					.add(period("20:30", 0.0, 335.6, 663.0)) //
					.add(period("20:45", 0.0, 335.6, 432.0)) //
					.add(period("21:00", 0.0, 326.6, 579.0)) //
					.add(period("21:15", 0.0, 326.6, 430.0)) //
					.add(period("21:30", 0.0, 326.6, 421.0)) //
					.add(period("21:45", 0.0, 326.6, 542.0)) //
					.add(period("22:00", 0.0, 314.9, 703.0)) //
					.add(period("22:15", 0.0, 314.9, 656.0)) //
					.add(period("22:30", 0.0, 314.9, 476.0)) //
					.add(period("22:45", 0.0, 314.9, 446.0)) //
					.add(period("23:00", 0.0, 307.7, 450.0)) //
					.add(period("23:15", 0.0, 307.7, 487.0)) //
					.add(period("23:30", 0.0, 307.7, 554.0)) //
					.add(period("23:45", 0.0, 307.7, 434.0)) //
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