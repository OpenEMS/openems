package io.openems.edge.energy.optimizer.app;

import static io.openems.edge.energy.optimizer.app.AppUtils.simulateFromLog;

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
 * journalctl -lu openems --since="20 minutes ago" | grep OPTIMIZER | sed 's/.*OPTIMIZER/OPTIMIZER/'
 * </code>
 */
public class RunOptimizerFromLogApp {

	private static final long EXECUTION_LIMIT_SECONDS = 5;

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
		simulateFromLog(LOG, EXECUTION_LIMIT_SECONDS);
	}
}
