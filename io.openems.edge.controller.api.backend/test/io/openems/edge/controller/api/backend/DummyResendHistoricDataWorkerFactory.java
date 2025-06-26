package io.openems.edge.controller.api.backend;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils.ReflectionException;

public class DummyResendHistoricDataWorkerFactory extends ResendHistoricDataWorkerFactory {

	public DummyResendHistoricDataWorkerFactory() throws ReflectionException {
		super();
		setAttributeViaReflection(this, "cso", new DummyResendHistoricDataWorkerCso());
	}

	private static class DummyResendHistoricDataWorkerCso implements ComponentServiceObjects<ResendHistoricDataWorker> {

		@Override
		public ResendHistoricDataWorker getService() {
			return new ResendHistoricDataWorker();
		}

		@Override
		public void ungetService(ResendHistoricDataWorker service) {
			// empty for tests
		}

		@Override
		public ServiceReference<ResendHistoricDataWorker> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
