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
	 * ReadOnly App and let him install the ReadWrite ones.
	 */
	private static final int INITIAL_WAIT_TIME = 60_000; // in ms

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final AppManagerImpl parent;

	public AppInstallWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	private void installFreeApps() {
		this.installReadOnlyApi("App.Api.ModbusTcp.ReadOnly", "App.Api.ModbusTcp.ReadWrite",
				"Controller.Api.ModbusTcp.ReadWrite");
		this.installReadOnlyApi("App.Api.RestJson.ReadOnly", "App.Api.RestJson.ReadWrite",
				"Controller.Api.Rest.ReadWrite");
	}

	private final void installReadOnlyApi(String readOnly, String readWrite, String readWriteController) {
		if (this.parent.getInstantiatedApps().stream()
				.noneMatch(t -> t.appId.equals(readOnly) || t.appId.equals(readWrite))) {

			// TODO this is only required if the ReadWrite controller exists without an App
			if (this.parent.componentManager.getEdgeConfig().getComponentIdsByFactory(readWriteController)
					.size() == 0) {

				try {
					this.parent.handleAddAppInstanceRequest(null,
							new AddAppInstance.Request(readOnly, "", JsonUtils.buildJsonObject().build()));
				} catch (OpenemsNamedException e) {
					this.log.info("Unable to install free App[" + readOnly + "]");
				}
			} else {
				this.log.warn("Unable to create App[" + readOnly + "] because a " + "Component with the FactoryId["
						+ readWrite + "] exists!");
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
