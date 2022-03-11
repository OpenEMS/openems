package io.openems.edge.core.appmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import com.google.gson.JsonObject;

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

	public AppValidateWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		this.validateApps();

		this.parent._setDefectiveApp(!this.defectiveApps.isEmpty());
	}

	/**
	 * Validates all Apps.
	 *
	 * <p>
	 * 'protected' so that it can be used in a JUnit test.
	 */
	protected void validateApps() {
		for (var instantiatedApp : this.parent.instantiatedApps) {
			for (OpenemsApp app : this.parent.availableApps) {
				if (app.getAppId().equals(instantiatedApp.appId)) {
					this.validateApp(app, instantiatedApp.properties);
				}
			}
		}
	}

	/**
	 * Validates all Apps.
	 *
	 * <p>
	 * 'protected' so that it can be used in a JUnit test.
	 *
	 * @param app        the {@link OpenemsApp}
	 * @param properties the App properties
	 */
	protected void validateApp(OpenemsApp app, JsonObject properties) {
		// Found correct OpenemsApp -> validate
		var key = app.getName();
		try {
			app.validate(properties);

			this.defectiveApps.remove(key);
		} catch (OpenemsNamedException e1) {
			this.defectiveApps.put(key, e1.getMessage());
		}
	}

	private int cycleCountDown = AppValidateWorker.INITIAL_CYCLES;

	@Override
	protected int getCycleTime() {
		if (this.cycleCountDown > 0) {
			this.cycleCountDown--;
			return AppValidateWorker.INITIAL_CYCLE_TIME;
		}
		return AppValidateWorker.REGULAR_CYCLE_TIME;
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

	@Override
	public void triggerNextRun() {
		// Reset Cycle-Counter on explicit run
		this.cycleCountDown = AppValidateWorker.INITIAL_CYCLES;
		super.triggerNextRun();
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

}
