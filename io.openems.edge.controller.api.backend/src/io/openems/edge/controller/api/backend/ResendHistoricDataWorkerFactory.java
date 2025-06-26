package io.openems.edge.controller.api.backend;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ResendHistoricDataWorkerFactory.class)
public class ResendHistoricDataWorkerFactory {

	@Reference
	private ComponentServiceObjects<ResendHistoricDataWorker> cso;

	/**
	 * Returns a new {@link ResendHistoricDataWorker} service object.
	 * 
	 * @return the created {@link ResendHistoricDataWorker} object
	 * @see #unget(ResendHistoricDataWorker)
	 */
	public ResendHistoricDataWorker get() {
		return this.cso.getService();
	}

	/**
	 * Releases the {@link ResendHistoricDataWorker} service object.
	 * 
	 * @param service a {@link ResendHistoricDataWorker} provided by this factory
	 * @see #get()
	 */
	public void unget(ResendHistoricDataWorker service) {
		if (service == null) {
			return;
		}
		this.cso.ungetService(service);
	}

}
