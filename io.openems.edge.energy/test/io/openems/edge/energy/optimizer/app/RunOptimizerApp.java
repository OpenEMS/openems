package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.edge.energy.EnergySchedulerTestUtils.Controller;

/**
 * This little application allows running the Optimizer from a mocked JSON
 * definition.
 */
public class RunOptimizerApp {

	private static final long EXECUTION_LIMIT_SECONDS = 30;

	private static final JsonObject JSON = buildJsonObject() //
			.addProperty("startTime", ZonedDateTime.parse("2025-03-17T07:45:00Z")) //
			.add("grid", buildJsonObject() //
					.addProperty("maxBuy", 40000) //
					.addProperty("maxSell", 20000) //
					.build()) //
			.add("ess", buildJsonObject() //
					.addProperty("currentEnergy", 10120) //
					.addProperty("totalEnergy", 22000) //
					.addProperty("maxChargeEnergy", 2499) //
					.addProperty("maxDischargeEnergy", 2749) //
					.build()) //
			.add("eshs", buildJsonArray() //
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_FIX_ACTIVE_POWER.factoryPid) //
							.addProperty("id", "ctrlFixActivePower0") //
							.build())
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_EMERGENCY_CAPACITY_RESERVE.factoryPid) //
							.addProperty("id", "ctrlEmergencyCapacityReserve0") //
							.build())
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_GRID_OPTIMIZED_CHARGE.factoryPid) //
							.addProperty("id", "ctrlGridOptimizedCharge0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "Automatic") //
									.build()) //
							.build())
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.EVSE_SINGLE.factoryPid) //
							.addProperty("id", "ctrlEvseSingle0") //
							.add("source", buildJsonObject() //
									.addProperty("class", "SmartOptimizationConfig") //
									.addProperty("isReadyForCharging", true) //
									.add("chargeParams", buildJsonObject() //
											.add("limit", buildJsonObject() //
													.addProperty("phase", "SINGLE_PHASE") //
													.addProperty("minCurrent", 6000) //
													.addProperty("maxCurrent", 32000) //
													.build()) //
											.add("profiles", buildJsonArray().build()) //
											.build()) //
									.addProperty("smartConfig", ZonedDateTime.parse("2025-03-17T07:45:00Z")) //
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
					.add(buildJsonObject() //
							.addProperty("factoryPid", Controller.ESS_TIME_OF_USE_TARIFF.factoryPid) //
							.addProperty("id", "ctrlEssTimeOfUseTariff0") //
							.add("source", buildJsonObject() //
									.addProperty("controlMode", "CHARGE_CONSUMPTION") //
									.build()) //
							.build())
					.build()) //
			.add("periods", buildJsonArray() //
					.add(buildJsonObject() //
							.addProperty("time", "07:45") //
							.addProperty("production", 291.30) //
							.addProperty("consumption", 305) //
							.addProperty("price", 367) //
							.build())
					.add(buildJsonObject().addProperty("time", "08:00").addProperty("production", 258.40)
							.addProperty("consumption", 351).addProperty("price", 345).build())
					.add(buildJsonObject().addProperty("time", "08:15").addProperty("production", 258.40)
							.addProperty("consumption", 932).addProperty("price", 1490).build())
					.add(buildJsonObject().addProperty("time", "08:30").addProperty("production", 258.40)
							.addProperty("consumption", 1087).addProperty("price", 1523).build())
					.add(buildJsonObject().addProperty("time", "08:45").addProperty("production", 258.40)
							.addProperty("consumption", 1258).addProperty("price", 1535).build())
					.add(buildJsonObject().addProperty("time", "09:00").addProperty("production", 215.40)
							.addProperty("consumption", 1372).addProperty("price", 1480).build())
					.add(buildJsonObject().addProperty("time", "09:15").addProperty("production", 215.40)
							.addProperty("consumption", 1388).addProperty("price", 1567).build())
					.add(buildJsonObject().addProperty("time", "09:30").addProperty("production", 215.40)
							.addProperty("consumption", 1533).addProperty("price", 1065).build())
					.add(buildJsonObject().addProperty("time", "09:45").addProperty("production", 215.40)
							.addProperty("consumption", 1808).addProperty("price", 503).build())
					.add(buildJsonObject().addProperty("time", "10:00").addProperty("production", 192.60)
							.addProperty("consumption", 1951).addProperty("price", 794).build())
					.add(buildJsonObject().addProperty("time", "10:15").addProperty("production", 192.60)
							.addProperty("consumption", 1918).addProperty("price", 906).build())
					.add(buildJsonObject().addProperty("time", "10:30").addProperty("production", 192.60)
							.addProperty("consumption", 2072).addProperty("price", 868).build())
					.add(buildJsonObject().addProperty("time", "10:45").addProperty("production", 192.60)
							.addProperty("consumption", 2222).addProperty("price", 1781).build())
					.add(buildJsonObject().addProperty("time", "11:00").addProperty("production", 180.70)
							.addProperty("consumption", 2370).addProperty("price", 973).build())
					.add(buildJsonObject().addProperty("time", "11:15").addProperty("production", 180.70)
							.addProperty("consumption", 2476).addProperty("price", 853).build())
					.add(buildJsonObject().addProperty("time", "11:30").addProperty("production", 180.70)
							.addProperty("consumption", 2552).addProperty("price", 996).build())
					.add(buildJsonObject().addProperty("time", "11:45").addProperty("production", 180.70)
							.addProperty("consumption", 2581).addProperty("price", 979).build())
					.add(buildJsonObject().addProperty("time", "12:00").addProperty("production", 177.90)
							.addProperty("consumption", 2598).addProperty("price", 1231).build())
					.add(buildJsonObject().addProperty("time", "12:15").addProperty("production", 177.90)
							.addProperty("consumption", 2604).addProperty("price", 1431).build())
					.add(buildJsonObject().addProperty("time", "12:30").addProperty("production", 177.90)
							.addProperty("consumption", 2603).addProperty("price", 1100).build())
					.add(buildJsonObject().addProperty("time", "12:45").addProperty("production", 177.90)
							.addProperty("consumption", 2583).addProperty("price", 895).build())
					.add(buildJsonObject().addProperty("time", "13:00").addProperty("production", 178.20)
							.addProperty("consumption", 2596).addProperty("price", 904).build())
					.add(buildJsonObject().addProperty("time", "13:15").addProperty("production", 178.20)
							.addProperty("consumption", 2603).addProperty("price", 1025).build())
					.add(buildJsonObject().addProperty("time", "13:30").addProperty("production", 178.20)
							.addProperty("consumption", 2594).addProperty("price", 781).build())
					.add(buildJsonObject().addProperty("time", "13:45").addProperty("production", 178.20)
							.addProperty("consumption", 2516).addProperty("price", 838).build())
					.add(buildJsonObject().addProperty("time", "14:00").addProperty("production", 180.70)
							.addProperty("consumption", 2431).addProperty("price", 931).build())
					.add(buildJsonObject().addProperty("time", "14:15").addProperty("production", 180.70)
							.addProperty("consumption", 2325).addProperty("price", 654).build())
					.add(buildJsonObject().addProperty("time", "14:30").addProperty("production", 180.70)
							.addProperty("consumption", 2153).addProperty("price", 613).build())
					.add(buildJsonObject().addProperty("time", "14:45").addProperty("production", 180.70)
							.addProperty("consumption", 2087).addProperty("price", 1117).build())
					.add(buildJsonObject().addProperty("time", "15:00").addProperty("production", 201.30)
							.addProperty("consumption", 2021).addProperty("price", 987).build())
					.add(buildJsonObject().addProperty("time", "15:15").addProperty("production", 201.30)
							.addProperty("consumption", 1864).addProperty("price", 1554).build())
					.add(buildJsonObject().addProperty("time", "15:30").addProperty("production", 201.30)
							.addProperty("consumption", 1653).addProperty("price", 1692).build())
					.add(buildJsonObject().addProperty("time", "15:45").addProperty("production", 201.30)
							.addProperty("consumption", 1581).addProperty("price", 970).build())
					.add(buildJsonObject().addProperty("time", "16:00").addProperty("production", 288.40)
							.addProperty("consumption", 1360).addProperty("price", 803).build())
					.add(buildJsonObject().addProperty("time", "16:15").addProperty("production", 288.40)
							.addProperty("consumption", 1262).addProperty("price", 676).build())
					.add(buildJsonObject().addProperty("time", "16:30").addProperty("production", 288.40)
							.addProperty("consumption", 1148).addProperty("price", 1395).build())
					.add(buildJsonObject().addProperty("time", "16:45").addProperty("production", 288.40)
							.addProperty("consumption", 985).addProperty("price", 975).build())
					.add(buildJsonObject().addProperty("time", "17:00").addProperty("production", 330.20)
							.addProperty("consumption", 743).addProperty("price", 397).build())
					.add(buildJsonObject().addProperty("time", "17:15").addProperty("production", 330.20)
							.addProperty("consumption", 516).addProperty("price", 604).build())
					.add(buildJsonObject().addProperty("time", "17:30").addProperty("production", 330.20)
							.addProperty("consumption", 224).addProperty("price", 892).build())
					.add(buildJsonObject().addProperty("time", "17:45").addProperty("production", 330.20)
							.addProperty("consumption", 68).addProperty("price", 1113).build())
					.add(buildJsonObject().addProperty("time", "18:00").addProperty("production", 341.70)
							.addProperty("consumption", 13).addProperty("price", 590).build())
					.add(buildJsonObject().addProperty("time", "18:15").addProperty("production", 341.70)
							.addProperty("consumption", 2).addProperty("price", 832).build())
					.add(buildJsonObject().addProperty("time", "18:30").addProperty("production", 341.70)
							.addProperty("consumption", 0).addProperty("price", 726).build())
					.add(buildJsonObject().addProperty("time", "18:45").addProperty("production", 341.70)
							.addProperty("consumption", 0).addProperty("price", 394).build())
					.add(buildJsonObject().addProperty("time", "19:00").addProperty("production", 343.30)
							.addProperty("consumption", 0).addProperty("price", 950).build())
					.add(buildJsonObject().addProperty("time", "19:15").addProperty("production", 343.30)
							.addProperty("consumption", 0).addProperty("price", 662).build())
					.add(buildJsonObject().addProperty("time", "19:30").addProperty("production", 343.30)
							.addProperty("consumption", 0).addProperty("price", 394).build())
					.add(buildJsonObject().addProperty("time", "19:45").addProperty("production", 343.30)
							.addProperty("consumption", 0).addProperty("price", 728).build())
					.add(buildJsonObject().addProperty("time", "20:00").addProperty("production", 335.60)
							.addProperty("consumption", 0).addProperty("price", 492).build())
					.add(buildJsonObject().addProperty("time", "20:15").addProperty("production", 335.60)
							.addProperty("consumption", 0).addProperty("price", 1320).build())
					.add(buildJsonObject().addProperty("time", "20:30").addProperty("production", 335.60)
							.addProperty("consumption", 0).addProperty("price", 663).build())
					.add(buildJsonObject().addProperty("time", "20:45").addProperty("production", 335.60)
							.addProperty("consumption", 0).addProperty("price", 432).build())
					.add(buildJsonObject().addProperty("time", "21:00").addProperty("production", 326.60)
							.addProperty("consumption", 0).addProperty("price", 579).build())
					.add(buildJsonObject().addProperty("time", "21:15").addProperty("production", 326.60)
							.addProperty("consumption", 0).addProperty("price", 430).build())
					.add(buildJsonObject().addProperty("time", "21:30").addProperty("production", 326.60)
							.addProperty("consumption", 0).addProperty("price", 421).build())
					.add(buildJsonObject().addProperty("time", "21:45").addProperty("production", 326.60)
							.addProperty("consumption", 0).addProperty("price", 542).build())
					.add(buildJsonObject().addProperty("time", "22:00").addProperty("production", 314.90)
							.addProperty("consumption", 0).addProperty("price", 703).build())
					.add(buildJsonObject().addProperty("time", "22:15").addProperty("production", 314.90)
							.addProperty("consumption", 0).addProperty("price", 656).build())
					.add(buildJsonObject().addProperty("time", "22:30").addProperty("production", 314.90)
							.addProperty("consumption", 0).addProperty("price", 476).build())
					.add(buildJsonObject().addProperty("time", "22:45").addProperty("production", 314.90)
							.addProperty("consumption", 0).addProperty("price", 446).build())
					.add(buildJsonObject().addProperty("time", "23:00").addProperty("production", 307.70)
							.addProperty("consumption", 0).addProperty("price", 450).build())
					.add(buildJsonObject().addProperty("time", "23:15").addProperty("production", 307.70)
							.addProperty("consumption", 0).addProperty("price", 487).build())
					.add(buildJsonObject().addProperty("time", "23:30").addProperty("production", 307.70)
							.addProperty("consumption", 0).addProperty("price", 554).build())
					.add(buildJsonObject().addProperty("time", "23:45").addProperty("production", 307.70)
							.addProperty("consumption", 0).addProperty("price", 434).build())
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