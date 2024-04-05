package io.openems.edge.controller.api.backend;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils;

public class DummyResendHistoricDataWorkerFactory extends ResendHistoricDataWorkerFactory {

	public DummyResendHistoricDataWorkerFactory()
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super();
		ReflectionUtils.setAttribute(ResendHistoricDataWorkerFactory.class, this, "cso",
				new DummyResendHistoricDataWorkerCso());
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
