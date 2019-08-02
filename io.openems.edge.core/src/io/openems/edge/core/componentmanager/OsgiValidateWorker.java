package io.openems.edge.core.componentmanager;

import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final static int INITIAL_CYCLE_TIME = 1_000; // in ms
	private final static int REGULAR_CYCLE_TIME = 60_000; // in ms

	private final Logger log = LoggerFactory.getLogger(OsgiValidateWorker.class);

	private final ComponentManagerImpl parent;

	public OsgiValidateWorker(ComponentManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() {
		boolean allConfigActivated = true;
		try {
			ConfigurationAdmin cm = this.parent.cm;
			Configuration[] configs = cm.listConfigurations("(enabled=true)");
			if (configs != null) {
				for (Configuration config : configs) {
					Dictionary<String, Object> properties = config.getProperties();
					String componentId = (String) properties.get("id");
					if (!this.isComponentActivated(componentId)) {
						this.parent.logWarn(this.log, "Component [" + componentId + "] is configured but not active!");
						allConfigActivated = false;
					}
				}
			}
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
		}
		this.parent.configNotActivatedChannel().setNextValue(!allConfigActivated);
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
