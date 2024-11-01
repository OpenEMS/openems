package io.openems.edge.controller.api.backend;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.controller.api.backend.handler.BindingRoutesJsonApiHandler;
import io.openems.edge.controller.api.backend.handler.RootRequestHandler;
import io.openems.edge.controller.api.common.handler.RoutesJsonApiHandler;

public class DummyBackendOnRequestFactory extends BackendOnRequest.Factory {

	public DummyBackendOnRequestFactory() throws ReflectionException {
		super();
		setAttributeViaReflection(this, "cso", new DummyBackendOnRequestCso());
	}

	private static class DummyBackendOnRequestCso implements ComponentServiceObjects<BackendOnRequest> {

		@Override
		public BackendOnRequest getService() {
			return new BackendOnRequest(
					new RootRequestHandler(new BindingRoutesJsonApiHandler(new RoutesJsonApiHandler())));
		}

		@Override
		public void ungetService(BackendOnRequest service) {
			// empty for tests
		}

		@Override
		public ServiceReference<BackendOnRequest> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
