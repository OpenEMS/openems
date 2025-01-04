package io.openems.edge.energy.optimizer.app;

import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY;
import static io.openems.edge.energy.optimizer.app.AppUtils.parseGlobalSimulationsContextFromLogString;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.Duration.ofSeconds;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserveImpl;
import io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl;
import io.openems.edge.controller.ess.gridoptimizedcharge.ControllerEssGridOptimizedChargeImpl;
import io.openems.edge.controller.ess.gridoptimizedcharge.Mode;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischargeImpl;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.controller.evcs.Utils.EshContext.EshSmartContext;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyUtils;
import io.openems.edge.energy.optimizer.Simulator;
import io.openems.edge.energy.optimizer.Utils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

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

	private static final ManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0");

	private static final ImmutableList<EnergyScheduleHandler> ESHS = ImmutableList.of(//
			ControllerEssEmergencyCapacityReserveImpl.buildEnergyScheduleHandler(//
					() -> /* reserveSoc */ 0), //
			ControllerEssLimitTotalDischargeImpl.buildEnergyScheduleHandler(//
					() -> /* minSoc */ 0), //
			ControllerEssFixActivePowerImpl.buildEnergyScheduleHandler(//
					() -> new ControllerEssFixActivePowerImpl.EshContext(
							io.openems.edge.controller.ess.fixactivepower.Mode.MANUAL_ON, //
							EnergyUtils.toEnergy(-1000), Relationship.GREATER_OR_EQUALS)), //
			io.openems.edge.controller.evcs.Utils.buildEshSmart(//
					() -> new EshSmartContext("evcs1", 30000, ImmutableList.of(//
							JSCalendar.Task.<EshSmartContext.Payload>create() //
									.setStart(LocalDateTime.of(2024, 28, 12, 7, 0, 0)) //
									.addRecurrenceRule(b -> b //
											.setFrequency(WEEKLY) //
											.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
									.setPayload(new EshSmartContext.Payload(10000)) //
									.build(), //
							JSCalendar.Task.<EshSmartContext.Payload>create() //
									.setStart(LocalDateTime.of(2024, 28, 12, 7, 0, 0)) //
									.addRecurrenceRule(b -> b //
											.setFrequency(WEEKLY) //
											.addByDay(MONDAY)) //
									.setPayload(new EshSmartContext.Payload(40000)) //
									.build()))), //
			ControllerEssGridOptimizedChargeImpl.buildEnergyScheduleHandler(//
					() -> Mode.MANUAL, //
					() -> LocalTime.of(10, 00)), //
			// TODO EssLimiter
			TimeOfUseTariffControllerImpl.buildEnergyScheduleHandler(//
					() -> ESS, //
					() -> ControlMode.CHARGE_CONSUMPTION, //
					() -> /* maxChargePowerFromGrid */ 20_000) //
	);

	/** Insert the full log lines including GlobalSimulationsContext header. */
	private static final String LOG = """
			""";

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		var simulator = new Simulator(parseGlobalSimulationsContextFromLogString(LOG, ESHS));

		var simulationResult = simulator.getBestSchedule(EMPTY, false /* isCurrentPeriodFixed */, null, //
				stream -> stream //
						.limit(byExecutionTime(ofSeconds(EXECUTION_LIMIT_SECONDS))));

		Utils.logSimulationResult(simulator, simulationResult);
	}
}