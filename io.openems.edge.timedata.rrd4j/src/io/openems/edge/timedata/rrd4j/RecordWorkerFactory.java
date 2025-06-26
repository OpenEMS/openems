package io.openems.edge.timedata.rrd4j;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RecordWorkerFactory.class)
public class RecordWorkerFactory {

	@Reference
	private ComponentServiceObjects<RecordWorker> cso;

	/**
	 * Returns a new {@link RecordWorker} service object.
	 * 
	 * @return the created {@link RecordWorker} object
	 * @see #unget(RecordWorker)
	 */
	public RecordWorker get() {
		return this.cso.getService();
	}

	/**
	 * Releases the {@link RecordWorker} service object.
	 * 
	 * @param service a {@link RecordWorker} provided by this factory
	 * @see #get()
	 */
	public void unget(RecordWorker service) {
		if (service == null) {
			return;
		}
		this.cso.ungetService(service);
	}

}
