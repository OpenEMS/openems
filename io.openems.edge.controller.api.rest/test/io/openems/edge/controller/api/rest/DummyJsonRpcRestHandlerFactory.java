package io.openems.edge.controller.api.rest;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import com.google.common.base.Supplier;

import io.openems.common.utils.ReflectionUtils;

public class DummyJsonRpcRestHandlerFactory extends JsonRpcRestHandler.Factory {

	public DummyJsonRpcRestHandlerFactory(Supplier<JsonRpcRestHandler> factoryMethod)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super();
		ReflectionUtils.setAttribute(JsonRpcRestHandler.Factory.class, this, "cso",
				new DummyJsonRpcRestHandlerCso(factoryMethod));
	}

	private static class DummyJsonRpcRestHandlerCso implements ComponentServiceObjects<JsonRpcRestHandler> {

		private final Supplier<JsonRpcRestHandler> factoryMethod;

		public DummyJsonRpcRestHandlerCso(Supplier<JsonRpcRestHandler> factoryMethod) {
			super();
			this.factoryMethod = factoryMethod;
		}

		@Override
		public JsonRpcRestHandler getService() {
			return this.factoryMethod.get();
		}

		@Override
		public void ungetService(JsonRpcRestHandler service) {
			// empty for tests
		}

		@Override
		public ServiceReference<JsonRpcRestHandler> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
