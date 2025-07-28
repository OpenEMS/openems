package io.openems.edge.common.test;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class DummyServiceRegistration<S> implements ServiceRegistration<S> {

	private final S service;
	private Dictionary<String, ?> properties;

	public DummyServiceRegistration(S service, Dictionary<String, ?> properties) {
		this.service = service;
		this.properties = properties;
	}

	@Override
	public ServiceReference<S> getReference() {
		return null;
	}

	@Override
	public void setProperties(Dictionary<String, ?> properties) {
		this.properties = properties;
	}

	@Override
	public void unregister() {

	}
}
