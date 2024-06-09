package io.openems.edge.bridge.http.dummy;

import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;

import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;

public class DummyBridgeHttpFactory extends BridgeHttpFactory {

	public final DummyBridgeHttp bridge = new DummyBridgeHttp();

	public DummyBridgeHttpFactory() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		super();
		ReflectionUtils.setAttribute(BridgeHttpFactory.class, this, "csoBridgeHttp",
				new DummyBridgeHttpCso(this.bridge));
	}

	private static class DummyBridgeHttpCso implements ComponentServiceObjects<BridgeHttp> {

		private final DummyBridgeHttp bridge;

		public DummyBridgeHttpCso(DummyBridgeHttp bridge) {
			this.bridge = bridge;
		}

		@Override
		public BridgeHttp getService() {
			return this.bridge;
		}

		@Override
		public void ungetService(BridgeHttp service) {
			// empty for tests
		}

		@Override
		public ServiceReference<BridgeHttp> getServiceReference() {
			// empty for tests
			return null;
		}
	}

}
