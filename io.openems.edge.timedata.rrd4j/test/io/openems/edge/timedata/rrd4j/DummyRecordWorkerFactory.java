package io.openems.edge.timedata.rrd4j;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.component.ComponentManager;

public class DummyRecordWorkerFactory extends RecordWorkerFactory {

	public DummyRecordWorkerFactory(ComponentManager componentManager) throws ReflectionException {
		super();
		setAttributeViaReflection(this, "cso", new DummyRecordWorkerCso(componentManager));
	}

	private static class DummyRecordWorkerCso implements ComponentServiceObjects<RecordWorker> {

		private final ComponentManager componentManager;

		public DummyRecordWorkerCso(ComponentManager componentManager) {
			super();
			this.componentManager = componentManager;
		}

		@Override
		public RecordWorker getService() {
			final var worker = new RecordWorker();
			setAttributeViaReflection(worker, "componentManager", this.componentManager);
			return worker;
		}

		@Override
		public void ungetService(RecordWorker service) {
			// empty for tests
		}

		@Override
		public ServiceReference<RecordWorker> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
