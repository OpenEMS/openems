package io.openems.edge.core.componentmanager;

import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.worker.AbstractWorker;

/**
 * This Worker constantly validates if all configured OpenEMS-Components are
 * actually activated. If not it prints a warning message ("Component [ID] is
 * configured but not active!") and sets the
 * {@link ComponentManagerImpl.ChannelId#CONFIG_NOT_ACTIVATED} StateChannel.
 */
public class OsgiValidateWorker extends AbstractWorker {

	private final static int CYCLE_TIME = 60_000; // in ms

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
			for (Configuration config : configs) {
				Dictionary<String, Object> properties = config.getProperties();
				String componentId = (String) properties.get("id");
				if (!this.isComponentActivated(componentId, config.getPid())) {
					this.parent.logWarn(this.log, "Component [" + componentId + "] is configured but not active!");
					allConfigActivated = false;
				}
			}
		} catch (Exception e) {
			this.parent.logError(this.log, e.getMessage());
			e.printStackTrace();
		}
		this.parent.configNotActivatedChannel().setNextValue(!allConfigActivated);
	}

	private boolean isComponentActivated(String componentId, String pid) {
		for (OpenemsComponent component : this.parent.components) {
			if (componentId.equals(component.id()) && pid.equals(component.servicePid())) {
				// Everything Ok
				return true;
			}
		}
		return false;
	}

	@Override
	protected int getCycleTime() {
		return OsgiValidateWorker.CYCLE_TIME;
	}

}
