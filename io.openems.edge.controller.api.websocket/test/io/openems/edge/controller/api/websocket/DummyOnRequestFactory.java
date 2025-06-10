package io.openems.edge.controller.api.websocket;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils.ReflectionException;

public class DummyOnRequestFactory extends OnRequest.Factory {

	public DummyOnRequestFactory() throws ReflectionException {
		super();
		setAttributeViaReflection(this, "cso", new DummyOnRequestCso());
	}

	private static class DummyOnRequestCso implements ComponentServiceObjects<OnRequest> {

		@Override
		public OnRequest getService() {
			return new OnRequest();
		}

		@Override
		public void ungetService(OnRequest service) {
			// empty for tests
		}

		@Override
		public ServiceReference<OnRequest> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
