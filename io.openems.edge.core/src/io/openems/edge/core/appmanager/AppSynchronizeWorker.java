package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.AppCenterGetInstalledAppsResponse.Instance;
import io.openems.common.worker.AbstractWorker;

public class AppSynchronizeWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	// usual cycle time if not explicit triggered
	private static final int CYCLE_TIME = 15 * 60_000;

	// try to get a valid response from the backend every 5 minutes
	private static final int NO_BACKEND_CONNECTION_CYCLE_TIME = 5 * 60_000;

	private final AppManagerImpl parent;
	private boolean shouldRun = true;
	private boolean validBackendResponse = false;

	private static final int WAIT_AFTER_TRIGGER_TIME = 3 * 60_000;
	private boolean wasTriggered = false;
	private boolean isTriggered = false;

	private final Object synchronizationLock = new Object();

	protected AppSynchronizeWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws Throwable {
		if (this.isTriggered) {
			this.isTriggered = false;
			this.wasTriggered = true;
			return;
		}
		this.syncAppsWithBackend();
	}

	/**
	 * Synchronizes the apps with the backend.
	 * 
	 * @return true if successful else false
	 */
	public boolean syncAppsWithBackend() throws InterruptedException {
		synchronized (this.synchronizationLock) {
			List<Instance> installedAppsOfBackend;
			try {
				installedAppsOfBackend = this.parent.backendUtil.getInstalledApps();
			} catch (OpenemsNamedException ex) {
				this.validBackendResponse = false;
				return false;
			}
			var installedAppsOfEdge = this.parent.getInstantiatedApps();

			var appsWhichNotExist = new ArrayList<Instance>();
			// only validates the installed apps from the backend; not validating instance
			// that are installed on the edge and are not logged in the backend
			for (var installedBackendApp : installedAppsOfBackend) {
				if (installedAppsOfEdge.stream() //
						.anyMatch(t -> t.appId.equals(installedBackendApp.appId)
								&& t.instanceId.equals(installedBackendApp.instanceId))) {
					// app exists like it should
					continue;
				} else {
					// app does not exist
					appsWhichNotExist.add(installedBackendApp);
				}
			}

			this.parent._setAppsNotSyncedWithBackend(!appsWhichNotExist.isEmpty());

			if (appsWhichNotExist.isEmpty()) {
				this.shouldRun = false;
			} else {
				for (var instance : appsWhichNotExist) {
					try {
						this.parent.backendUtil.addDeinstallAppInstanceHistory(null, instance.appId,
								instance.instanceId);
					} catch (OpenemsNamedException ex) {
						this.log.error("Can not add deintall app instance entry to database!", ex);
					}
				}
			}
			return true;
		}
	}

	/**
	 * Gets the lock for synchronizing apps with the backend. Should be used when
	 * apps get installed/updated/removed.
	 * 
	 * @return the lock
	 */
	public final Object getSynchronizationLock() {
		return this.synchronizationLock;
	}

	@Override
	protected int getCycleTime() {
		if (!this.shouldRun) {
			return ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
		}
		if (this.wasTriggered) {
			this.wasTriggered = false;
			return WAIT_AFTER_TRIGGER_TIME;
		}
		if (!this.validBackendResponse) {
			return NO_BACKEND_CONNECTION_CYCLE_TIME;
		}
		return CYCLE_TIME;
	}

	/**
	 * Sets if the last request was a valid response from the backend.
	 * 
	 * @param validBackendResponse if the last response was valid
	 */
	public void setValidBackendResponse(boolean validBackendResponse) {
		this.validBackendResponse = validBackendResponse;
	}

	@Override
	public void triggerNextRun() {
		this.shouldRun = true;
		this.isTriggered = true;
		super.triggerNextRun();
	}

}
