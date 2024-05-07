package io.openems.edge.controller.api.websocket;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils;

public class DummyOnRequestFactory extends OnRequest.Factory {

	public DummyOnRequestFactory() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super();
		ReflectionUtils.setAttribute(OnRequest.Factory.class, this, "cso", new DummyOnRequestCso());
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
