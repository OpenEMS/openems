package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.SimulatorTest.logSchedule;

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

	/**
	 * Run the Application.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		var params = IntegrationTests.parseParams(LOG);
		var schedule = getBestSchedule(params, EXECUTION_LIMIT_SECONDS);

		logSchedule(params, schedule);
	}
}