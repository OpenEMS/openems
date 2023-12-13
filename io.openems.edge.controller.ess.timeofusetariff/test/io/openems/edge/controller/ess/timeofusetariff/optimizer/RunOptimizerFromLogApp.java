package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.logSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;

import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.google.common.primitives.Floats;

import io.openems.edge.controller.ess.timeofusetariff.ControlMode;

/**
 * This little application allows running the Optimizer from an existing log.
 * Just fill the header data and paste the log lines in
 * {@link RunOptimizerFromLogApp#LOG}.
 */
public class RunOptimizerFromLogApp {

	/** The ESS Capacity in [Wh]. */
	private static final int ESS_CAPACITY = 22000;

	/** The ESS Max Power [W]. */
	private static final int ESS_MAX_POWER = 10000;

	/** The Max Buy-from-Grid Power [W]. */
	private static final int MAX_BUY_FROM_GRID = 6000;

	/** The {@link ControlMode}. */
	private static final ControlMode CONTROL_MODE = ControlMode.CHARGE_CONSUMPTION;

	/** Insert the log lines without header. */
	private static final String LOG = """
			""";

	private static final Pattern PATTERN = Pattern.compile("^.*(?<log>\\d{2}:\\d{2}\s+.*$)");

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		var periods = LOG.lines() //
				.map(l -> {
					var matcher = PATTERN.matcher(l);
					if (!matcher.find()) {
						return null;
					}
					return matcher.group("log"); //
				}) //
				.filter(Objects::nonNull) //
				.map(Period::fromLog) //
				.toList();
		if (periods.isEmpty()) {
			throw new IllegalArgumentException("No Periods");
		}
		var params = Params.create() //
				.time(periods.get(0).time()) //
				.essAvailableEnergy(periods.get(0).essInitial()) //
				.essCapacity(ESS_CAPACITY) //
				.essMaxEnergyPerPeriod(toEnergy(ESS_MAX_POWER)) //
				.maxBuyFromGrid(toEnergy(MAX_BUY_FROM_GRID)) //
				.productions(periods.stream().mapToInt(Period::production).toArray()) //
				.consumptions(periods.stream().mapToInt(Period::consumption).toArray()) //
				.prices(Floats.toArray(periods.stream().map(Period::price).toList())) //
				.states(CONTROL_MODE.states) //
				.existingSchedule(new TreeMap<>()) //
				.build();
		var schedule = getBestSchedule(params);

		logSchedule(params, schedule);
	}
}