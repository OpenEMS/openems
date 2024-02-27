package io.openems.edge.timedata.rrd4j;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.component.ComponentManager;

public class DummyRecordWorkerFactory extends RecordWorkerFactory {

	public DummyRecordWorkerFactory(ComponentManager componentManager)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super();
		ReflectionUtils.setAttribute(RecordWorkerFactory.class, this, "cso",
				new DummyRecordWorkerCso(componentManager));
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
			try {
				ReflectionUtils.setAttribute(RecordWorker.class, worker, "componentManager", this.componentManager);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
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
