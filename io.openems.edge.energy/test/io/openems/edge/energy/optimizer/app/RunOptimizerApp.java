package io.openems.edge.energy.optimizer.app;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

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
			.add("grid", TestConfig.Grid.WITH_DYNAMIC_GRID_LIMIT) //
			.add("ess", TestConfig.Ess.FENECON_HOME_10) //
			.add("eshs", buildJsonArray() //
					// Electric Vehicle Charging Equipment (EVSE)

					.add(TestConfig.Controller.EVSE_CLUSTER_TWO)

					// Energy Storage System (ESS)

					// .add(TestConfig.Controller.ESS_FIX_ACTIVE_POWER)
					// .add(TestConfig.Controller.ESS_LIMIT_TOTAL_DISCHARGE)
					// .add(TestConfig.Controller.ESS_EMERGENCY_CAPACITY_RESERVE)
					// .add(TestConfig.Controller.ESS_GRID_OPTIMIZED_CHARGE_MANUAL)
					// .add(TestConfig.Controller.ESS_GRID_OPTIMIZED_CHARGE_AUTOMATIC)
					.add(TestConfig.Controller.ESS_TIME_OF_USE_TARIFF)

					.build()) //

			.add("periods", TestConfig.Periods.getTestData()) //
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
}