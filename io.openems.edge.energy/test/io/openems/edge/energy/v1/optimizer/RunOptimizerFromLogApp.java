package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.energy.v1.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.energy.v1.optimizer.Simulator.simulate;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.logSchedule;

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
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		var params = IntegrationTests.parseParams(LOG);
		var schedule = getBestSchedule(params, EXECUTION_LIMIT_SECONDS);
		var periods = simulate(params, schedule);

		logSchedule(params, periods);
	}
}