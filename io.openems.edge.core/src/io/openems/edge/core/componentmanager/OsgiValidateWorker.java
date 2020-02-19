package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Worker constantly validates if all configured OpenEMS-Components are
 * actually activated. If not it prints a warning message ("Component [ID] is
 * configured but not active!") and sets the
 * {@link ComponentManagerImpl.ChannelId#CONFIG_NOT_ACTIVATED} StateChannel.
 */
public class OsgiValidateWorker extends AbstractWorker {

	/*
	 * For INITIAL_CYCLES cycles the distance between two checks is
	 * INITIAL_CYCLE_TIME, afterwards the check runs every REGULAR_CYCLE_TIME
	 * milliseconds.
	 * 
	 * Why? In the beginning it takes a while till all components are up and
	 * running. So it is likely, that in the beginning not all are immediately
	 * running.
	 */
	private final static int INITIAL_CYCLES = 60;
	private final static int INITIAL_CYCLE_TIME = 5_000; // in ms
	private final static int REGULAR_CYCLE_TIME = 60_000; // in ms

	private final static int RESTART_PERIOD = 10; // in minutes
	private final static int FIRST_RESTART_PERIOD = RESTART_PERIOD - 1; // in minutes
	private final static int ANNOUNCE_FAULT_AFTER = 5; // in minutes

	private final Logger log = LoggerFactory.getLogger(OsgiValidateWorker.class);

	private static class DefectiveComponent {
		private final LocalDateTime defectiveSince;
		private LocalDateTime lastTryToRestart = LocalDateTime.MIN;

		public DefectiveComponent() {
			this.defectiveSince = LocalDateTime.now();
			// wait only 1 minute till first restart
			this.lastTryToRestart = this.defectiveSince.minusMinutes(FIRST_RESTART_PERIOD);
		}

		public void updateLastTryToRestart() {
			this.lastTryToRestart = LocalDateTime.now();
		}
	}

	private final Map<String, DefectiveComponent> componentDefectiveSince = new HashMap<>();
	private final ComponentManagerImpl parent;

	public OsgiValidateWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		/*
		 * Compare all Configuration Admin Configurations with actually existing and
		 * active OpenEMS Components
		 */
		try {
			ConfigurationAdmin cm = this.parent.cm;
			Configuration[] configs = cm.listConfigurations("(enabled=true)");
			if (configs != null) {
				for (Configuration config : configs) {
					Dictionary<String, Object> properties = config.getProperties();
					String componentId = (String) properties.get("id");
					if (this.isComponentActivated(componentId)) {
						this.componentDefectiveSince.remove(componentId);
					} else {
						this.componentDefectiveSince.putIfAbsent(componentId, new DefectiveComponent());
					}
				}
			}
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
		}

		/*
		 * If there are inactive Components: print log and set Warning State-Channel and
		 * try to restart them.
		 */
		boolean announceConfigNotActivated = false;

		if (!this.componentDefectiveSince.isEmpty()) {
			this.parent.logWarn(this.log, "Component(s) configured but not active: "
					+ String.join(",", this.componentDefectiveSince.keySet()));

			this.parent.configNotActivatedChannel().setNextValue(true);

			Iterator<Entry<String, DefectiveComponent>> iterator = this.componentDefectiveSince.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, DefectiveComponent> entry = iterator.next();
				DefectiveComponent defectiveComponent = entry.getValue();

				if (defectiveComponent.lastTryToRestart.isBefore(LocalDateTime.now().minusMinutes(RESTART_PERIOD))) {
					this.parent.logInfo(this.log, "Trying to restart Component [" + entry.getKey() + "]");
					Configuration config;
					try {
						config = this.parent.getExistingConfigForId(entry.getKey());
					} catch (OpenemsNamedException e) {
						// Component with this ID is not existing (anymore?!)
						iterator.remove();
						continue;
					}
					try {
						Dictionary<String, Object> properties = config.getProperties();
						config.update(properties);
						defectiveComponent.updateLastTryToRestart();
					} catch (IOException e) {
						this.parent.logError(this.log, e.getMessage());
						e.printStackTrace();
					}
				}

				if (defectiveComponent.defectiveSince
						.isBefore(LocalDateTime.now().minusMinutes(ANNOUNCE_FAULT_AFTER))) {
					announceConfigNotActivated = true;
				}
			}
		}

		this.parent.configNotActivatedChannel().setNextValue(announceConfigNotActivated);
	}

	private boolean isComponentActivated(String componentId) {
		for (OpenemsComponent component : this.parent.getEnabledComponents()) {
			if (componentId.equals(component.id())) {
				// Everything Ok
				return true;
			}
		}
		return false;
	}

	private int cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;

	@Override
	protected int getCycleTime() {
		if (this.cycleCountDown > 0) {
			this.cycleCountDown--;
			return OsgiValidateWorker.INITIAL_CYCLE_TIME;
		} else {
			return OsgiValidateWorker.REGULAR_CYCLE_TIME;
		}
	}

	@Override
	public void triggerNextRun() {
		// Reset Cycle-Counter on explicit run
		this.cycleCountDown = OsgiValidateWorker.INITIAL_CYCLES;
		super.triggerNextRun();
	}

}
