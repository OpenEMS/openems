package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.edge.energy.EnergySchedulerTestUtils.Controller;

public class TestConfigs {
	private TestConfigs() {
	}

	protected static final JsonObject ESS_EVSE = buildJsonObject() //
			.addProperty("startTime", ZonedDateTime.parse("2025-03-17T22:30:00Z")) //
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
							.addProperty("factoryPid", Controller.ESS_TIME_OF_USE_TARIFF.factoryPid) //
							.addProperty("id", "ctrlEssTimeOfUseTariff0") //
							.add("source", buildJsonObject() //
									.addProperty("controlMode", "CHARGE_CONSUMPTION") //
									.build()) //
							.build())
					.build()) //
			.add("periods", buildJsonArray() //
					.add(buildJsonObject() //
							.addProperty("time", "22:30") //
							.addProperty("production", 0) //
							.addProperty("consumption", 747) //
							.addProperty("price", 309.00) //
							.build())
					.add(buildJsonObject() //
							.addProperty("time", "22:45") //
							.addProperty("production", 0) //
							.addProperty("consumption", 743) //
							.addProperty("price", 309.00) //
							.build())
					.build()) //
			.build();
}
