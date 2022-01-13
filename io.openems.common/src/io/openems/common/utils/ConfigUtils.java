package io.openems.common.utils;

public class ConfigUtils {

	private ConfigUtils() {
	}

	/**
	 * Generates a target filter for a Declarative Service @Reference member.
	 *
	 * <p>
	 * Usage:
	 *
	 * <pre>
	 * generateReferenceTargetFilter(config.service_pid(), "Controllers", controllersIds);
	 * </pre>
	 *
	 * <p>
	 * Generates a 'target' filter on the 'Controllers' member so, that the the
	 * expected service to be injected needs to fulfill:
	 * <ul>
	 * <li>the service must be enabled
	 * <li>the service must not have the same PID as the calling component
	 * <li>the service "id" must be one of the provided "ids"
	 * </ul>
	 *
	 * @param pid PID of the calling component (use 'config.service_pid()' or
	 *            '(String)prop.get(Constants.SERVICE_PID)'; if null, PID filter is
	 *            not added to the resulting target filter
	 * @param ids Component IDs to be filtered for; for empty list, no ids are added
	 *            to the target filter
	 * @return the target filter
	 */
	public static String generateReferenceTargetFilter(String pid, String... ids) {
		// target component must be enabled
		var targetBuilder = new StringBuilder("(&(enabled=true)");
		if (pid != null && !pid.isEmpty()) {
			// target component must not be the same as the calling component
			targetBuilder.append("(!(service.pid=" + pid + "))");
		}
		// add filter for given Component-IDs
		if (ids.length > 0) {
			targetBuilder.append("(|");
			for (String id : ids) {
				targetBuilder.append("(id=" + id + ")");
			}
			targetBuilder.append(")");
		}
		targetBuilder.append(")");
		return targetBuilder.toString();
	}

}
