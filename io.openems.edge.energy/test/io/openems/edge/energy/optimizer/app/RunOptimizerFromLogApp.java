package io.openems.edge.energy.optimizer.app;

import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.edge.energy.EnergySchedulerTestUtils.eshEmergencyCapacityReserve;
import static io.openems.edge.energy.EnergySchedulerTestUtils.eshFixActivePower;
import static io.openems.edge.energy.EnergySchedulerTestUtils.eshGridOptimizedChargeManual;
import static io.openems.edge.energy.EnergySchedulerTestUtils.eshLimitTotalDischarge;
import static io.openems.edge.energy.EnergySchedulerTestUtils.eshTimeOfUseTariff;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.app.AppUtils.parseGlobalOptimizationContextFromLogString;
import static java.time.Duration.ofSeconds;

import java.time.LocalTime;

import com.google.common.collect.ImmutableList;

import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.optimizer.Simulator;
import io.openems.edge.energy.optimizer.Utils;
import io.openems.edge.ess.power.api.Relationship;

/**
 * This little application allows running the Optimizer from an existing log.
 * Just fill the header data and paste the log lines in
 * {@link RunOptimizerFromLogApp#LOG}.
 * 
 * <p>
 * To get the log from a system running with systemd, execute the following
 * query:
 * 
 * <p>
 * <code>
 * journalctl -lu openems --since="20 minutes ago" | grep OPTIMIZER
 * </code>
 */
public class RunOptimizerFromLogApp {

	private static final long EXECUTION_LIMIT_SECONDS = 30;

	private static final ImmutableList<EnergyScheduleHandler> ESHS = ImmutableList.of(//
			eshEmergencyCapacityReserve("ctrlEmergencyCapacityReserve0", 0), //
			eshLimitTotalDischarge("ctrlLimitTotalDischarge0", 0), //
			eshFixActivePower("ctrlFixActivePower0", -1000, Relationship.GREATER_OR_EQUALS),
			eshGridOptimizedChargeManual("ctrlGridOptimizedCharge0", LocalTime.of(10, 00)),
			eshTimeOfUseTariff("ctrlEssTimeOfUseTariff0", ControlMode.CHARGE_CONSUMPTION));

	/** Insert the full log lines including GlobalOptimizationContext header. */
	private static final String LOG = """
			""";

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		var simulator = new Simulator(parseGlobalOptimizationContextFromLogString(LOG, ESHS));

		var simulationResult = simulator.getBestSchedule(EMPTY_SIMULATION_RESULT, //
				false /* isCurrentPeriodFixed */, null, //
				stream -> stream //
						.limit(byExecutionTime(ofSeconds(EXECUTION_LIMIT_SECONDS))));

		Utils.logSimulationResult(simulator, simulationResult);
	}
}