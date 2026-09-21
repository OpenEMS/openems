package io.openems.edge.core.appmanager;

import java.util.List;

import io.openems.edge.app.enums.EMobilityArchitectureType;
import io.openems.edge.core.appmanager.validator.CheckEvcsNotInstalled;
import io.openems.edge.core.appmanager.validator.CheckEvseNotInstalled;
import io.openems.edge.core.appmanager.validator.Checkable;

/**
 * Marks an app as part of the e-mobility architecture.
 *
 * <p>
 * This is mostly used by {@link Checkable} to validate installability
 * constraints between different e-mobility app architectures, e.g.
 * {@link CheckEvcsNotInstalled} and {@link CheckEvseNotInstalled}.
 */
public interface EMobilityApp {

	/**
	 * Returns the architecture types supported by this app.
	 *
	 * @return a non-null list of {@link EMobilityArchitectureType}
	 */
	List<EMobilityArchitectureType> supportedArchitectureTypes();
}
