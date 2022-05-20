package io.openems.edge.core.appmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class AppInstallWorker extends AbstractWorker {

	/**
	 * Time to wait before doing the check. This allows the system to completely
	 * boot and read configurations. And enough time to allow the user to delete the
	 * ReadOnly App and let him install the ReadWrite one.
	 */
	private static final int INITIAL_WAIT_TIME = 60_000; // in ms

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final AppManagerImpl parent;

	public AppInstallWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	private void installFreeApps() {

		// install ModbusTcp.ReadOnly
		if (this.parent.getInstantiatedApps().stream().noneMatch(
				t -> t.appId.equals("App.Api.ModbusTcp.ReadOnly") || t.appId.equals("App.Api.ModbusTcp.ReadWrite"))) {

			// TODO this is only required if the ReadWrite controller exists without an App
			if (this.parent.componentManager.getEdgeConfig()
					.getComponentIdsByFactory("Controller.Api.ModbusTcp.ReadWrite").size() == 0) {

				try {
					this.parent.handleAddAppInstanceRequest(null, new AddAppInstance.Request(
							"App.Api.ModbusTcp.ReadOnly", "", JsonUtils.buildJsonObject().build()));
				} catch (OpenemsNamedException e) {
					this.log.info("Unable to install free App[ModbusTcp.ReadOnly]");
				}

			} else {
				this.log.warn("Unable to create App[App.Api.ModbusTcp.ReadOnly] because a "
						+ "Component with the FactoryId[Controller.Api.ModbusTcp.ReadWrite] exists!");
			}
		}

		// install RestJson.ReadOnly
		if (this.parent.getInstantiatedApps().stream().noneMatch(
				t -> t.appId.equals("App.Api.RestJson.ReadOnly") || t.appId.equals("App.Api.RestJson.ReadWrite"))) {

			// TODO this is only required if the ReadWrite controller exists without an App
			if (this.parent.componentManager.getEdgeConfig().getComponentIdsByFactory("Controller.Api.Rest.ReadWrite")
					.size() == 0) {

				try {
					this.parent.handleAddAppInstanceRequest(null, new AddAppInstance.Request(
							"App.Api.RestJson.ReadOnly", "", JsonUtils.buildJsonObject().build()));
				} catch (OpenemsNamedException e) {
					this.log.info("Unable to install free App[RestJson.ReadOnly]");
				}
			} else {
				this.log.warn("Unable to create App[App.Api.RestJson.ReadOnly] because a "
						+ "Component with the FactoryId[Controller.Api.Rest.ReadWrite] exists!");
			}
		}

	}

	@Override
	protected void forever() throws Throwable {
		this.installFreeApps();
	}

	@Override
	protected int getCycleTime() {
		return INITIAL_WAIT_TIME;
	}

}
