package io.openems.edge.ess.core.power.data;

import java.util.List;

import org.slf4j.Logger;

import io.openems.edge.ess.power.api.Constraint;

public class LogUtil {

	/**
	 * Prints all Constraints to the system log.
	 *
	 * @param log         a logger instance
	 * @param title       the log title
	 * @param constraints a list of Constraints
	 */
	public static void debugLogConstraints(Logger log, String title, List<Constraint> constraints) {
		log.info(title);
		for (Constraint c : constraints) {
			log.info("- " + c);
		}
	}
}
