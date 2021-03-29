package io.openems.edge.common.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * Simulates a {@link ComponentContext} for the OpenEMS Component test
 * framework.
 */
public class DummyComponentContext implements ComponentContext {

	public static DummyComponentContext from(AbstractComponentConfig configuration)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return new DummyComponentContext(configuration.getAsProperties());
	}

	private final Dictionary<String, Object> properties;

	private DummyComponentContext(Dictionary<String, Object> properties) {
		this.properties = properties;
		// TODO create DummyBundleContext
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		return this.properties;
	}

	@Override
	public <S> S locateService(String name) {
		return null;
	}

	@Override
	public <S> S locateService(String name, ServiceReference<S> reference) {
		return null;
	}

	@Override
	public Object[] locateServices(String name) {
		return new Object[] {};
	}

	@Override
	public BundleContext getBundleContext() {
		return null;
	}

	@Override
	public Bundle getUsingBundle() {
		return null;
	}

	@Override
	public <S> ComponentInstance<S> getComponentInstance() {
		return null;
	}

	@Override
	public void enableComponent(String name) {

	}

	@Override
	public void disableComponent(String name) {

	}

	@Override
	public ServiceReference<?> getServiceReference() {
		return null;
	}

}
