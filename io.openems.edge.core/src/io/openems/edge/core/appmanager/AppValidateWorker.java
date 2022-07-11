package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;

/**
 * This Worker constantly validates:.
 *
 * <ul>
 * <li>that all enabled OpenEMS Apps are properly configured, including required
 * OpenEMS Components, IP addresses, Scheduler settings, etc.
 * </ul>
 */
public class AppValidateWorker extends AbstractWorker {

	/*
	 * For INITIAL_CYCLES cycles the distance between two checks is
	 * INITIAL_CYCLE_TIME, afterwards the check runs every REGULAR_CYCLE_TIME
	 * milliseconds.
	 *
	 * Why? In the beginning it takes a while till all components are up and
	 * running. So it is likely, that in the beginning not all are immediately
	 * running.
	 */
	private static final int INITIAL_CYCLES = 60;
	private static final int INITIAL_CYCLE_TIME = 10_000; // in ms
	private static final int REGULAR_CYCLE_TIME = 60 * 60_000; // in ms

	private final AppManagerImpl parent;

	/**
	 * Map from App to defect details.
	 */
	protected final Map<String, String> defectiveApps = new HashMap<>();

	private int cycleCountDown = AppValidateWorker.INITIAL_CYCLES;

	public AppValidateWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	/**
	 * Called by {@link ConfigurationListener}.
	 *
	 * @param event a {@link ConfigurationEvent}
	 */
	protected void configurationEvent(ConfigurationEvent event) {
		// trigger immediate validation on configuration event
		this.triggerNextRun();
	}

	/**
	 * Called by parent debugLog.
	 *
	 * @return a debug log String or null
	 */
	protected String debugLog() {
		var defectiveApps = this.defectiveApps.entrySet().stream() //
				.map(e -> e.getKey() + "[" + e.getValue() + "]") //
				.collect(Collectors.joining(" "));

		if (defectiveApps.isEmpty()) {
			return null;

		}
		return defectiveApps;
	}

	@Override
	protected void forever() {
		this.validateApps();

		this.parent._setDefectiveApp(!this.defectiveApps.isEmpty());
	}

	@Override
	protected int getCycleTime() {
		if (this.cycleCountDown > 0) {
			this.cycleCountDown--;
			return AppValidateWorker.INITIAL_CYCLE_TIME;
		}
		return AppValidateWorker.REGULAR_CYCLE_TIME;
	}

	@Override
	public void triggerNextRun() {
		// Reset Cycle-Counter on explicit run
		this.cycleCountDown = AppValidateWorker.INITIAL_CYCLES;
		super.triggerNextRun();
	}

	/**
	 * Validates all Apps.
	 *
	 * <p>
	 * 'protected' so that it can be used in a JUnit test.
	 *
	 * @param app      the {@link OpenemsApp}
	 * @param instance the App instance
	 */
	protected void validateApp(OpenemsApp app, OpenemsAppInstance instance) {
		// Found correct OpenemsApp -> validate
		var key = app.getAppId();
		try {
			app.validate(instance);
		} catch (OpenemsNamedException e1) {
			this.defectiveApps.put(key, e1.getMessage());
		}
	}

	/**
	 * Validates all Apps.
	 *
	 * <p>
	 * 'protected' so that it can be used in a JUnit test.
	 */
	protected void validateApps() {
		var instances = new ArrayList<>(this.parent.instantiatedApps);
		for (OpenemsApp app : this.parent.availableApps) {
			this.defectiveApps.remove(app.getAppId());
			var removingInstances = new LinkedList<>();
			for (var instantiatedApp : instances) {
				if (app.getAppId().equals(instantiatedApp.appId)) {
					this.validateApp(app, instantiatedApp);
					removingInstances.add(instantiatedApp);
				}
			}
			instances.removeAll(removingInstances);
		}
		final var unknownApps = "UNKNOWAPPS";
		if (!instances.isEmpty()) {
			this.defectiveApps.put(unknownApps, instances.stream().map(t -> t.appId).collect(Collectors.joining("|")));
		} else {
			this.defectiveApps.remove(unknownApps);
		}
	}

}
