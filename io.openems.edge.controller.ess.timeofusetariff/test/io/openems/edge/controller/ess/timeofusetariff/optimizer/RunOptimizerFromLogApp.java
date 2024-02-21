package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.logSchedule;
import static java.lang.Integer.parseInt;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

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

	/** Insert the full log lines including Params header. */
	private static final String LOG = """
			""";

	private static final Pattern PARAMS_PATTERN = Pattern.compile("^" //
			+ ".*essTotalEnergy=(?<essTotalEnergy>\\d+)" //
			+ ".*essMinSocEnergy=(?<essMinSocEnergy>\\d+)" //
			+ ".*essMaxSocEnergy=(?<essMaxSocEnergy>\\d+)" //
			+ ".*essInitialEnergy=(?<essInitialEnergy>\\d+)" //
			+ ".*essMaxEnergyPerPeriod=(?<essMaxEnergyPerPeriod>\\d+)" //
			+ ".*maxBuyFromGrid=(?<maxBuyFromGrid>\\d+)" //
			+ ".*states=\\[(?<states>[A-Z_, ]+)\\]" //
			+ ".*$");
	private static final Pattern PERIOD_PATTERN = Pattern.compile("^.*(?<log>\\d{2}:\\d{2}\s+.*$)");

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		var paramsMatcher = LOG.lines() //
				.findFirst() //
				.map(PARAMS_PATTERN::matcher) //
				.get();
		paramsMatcher.find();
		final var essTotalEnergy = parseInt(paramsMatcher.group("essTotalEnergy"));
		final var essMinSocEnergy = parseInt(paramsMatcher.group("essMinSocEnergy"));
		final var essMaxSocEnergy = parseInt(paramsMatcher.group("essMaxSocEnergy"));
		final var essInitialEnergy = parseInt(paramsMatcher.group("essInitialEnergy"));
		final var essMaxEnergyPerPeriod = parseInt(paramsMatcher.group("essMaxEnergyPerPeriod"));
		final var maxBuyFromGrid = parseInt(paramsMatcher.group("maxBuyFromGrid"));
		final var states = Stream.of(paramsMatcher.group("states").split(", ")) //
				.map(StateMachine::valueOf) //
				.toArray(StateMachine[]::new);

		var periods = LOG.lines() //
				.skip(1) //
				.map(l -> {
					if (l.contains(" Time ")) { // remove header
						return null;
					}
					var matcher = PERIOD_PATTERN.matcher(l);
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
				.essTotalEnergy(essTotalEnergy) //
				.essMinSocEnergy(essMinSocEnergy) //
				.essMaxSocEnergy(essMaxSocEnergy) //
				.essInitialEnergy(essInitialEnergy) //
				.essMaxEnergyPerPeriod(essMaxEnergyPerPeriod) //
				.maxBuyFromGrid(maxBuyFromGrid) //
				.productions(periods.stream().mapToInt(Period::production).toArray()) //
				.consumptions(periods.stream().mapToInt(Period::consumption).toArray()) //
				.prices(periods.stream().mapToDouble(Period::price).toArray()) //
				.states(states) //
				.existingSchedule() //
				.build();
		var schedule = getBestSchedule(params, EXECUTION_LIMIT_SECONDS);

		logSchedule(params, schedule);
	}
}