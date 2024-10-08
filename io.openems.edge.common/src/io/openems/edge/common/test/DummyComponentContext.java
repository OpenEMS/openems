package io.openems.edge.common.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

import io.openems.common.test.AbstractComponentConfig;

/**
 * Simulates a {@link ComponentContext} for the OpenEMS Component test
 * framework.
 */
public class DummyComponentContext implements ComponentContext {

	/**
	 * Build a {@link DummyComponentContext} from a configuration.
	 * 
	 * @param configuration the {@link AbstractComponentConfig}
	 * @return the DummyComponentContextn
	 * @throws IllegalAccessException    on error
	 * @throws IllegalArgumentException  on error
	 * @throws InvocationTargetException on error
	 */
	public static DummyComponentContext from(AbstractComponentConfig configuration)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return new DummyComponentContext(configuration.getAsProperties());
	}

	private final Dictionary<String, Object> properties;
	private final ComponentInstance<?> instance;

	public DummyComponentContext(Dictionary<String, Object> properties, ComponentInstance<?> instance) {
		super();
		this.properties = properties;
		this.instance = instance;
	}

	public DummyComponentContext(Dictionary<String, Object> properties) {
		this(properties, null);
	}

	public DummyComponentContext() {
		this(new Hashtable<>());
	}

	/**
	 * Add a configuration property.
	 * 
	 * @param key   the property key
	 * @param value the property value
	 * @return myself
	 */
	public DummyComponentContext addProperty(String key, Object value) {
		this.properties.put(key, value);
		return this;
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
	@SuppressWarnings("unchecked")
	public <S> ComponentInstance<S> getComponentInstance() {
		return (ComponentInstance<S>) this.instance;
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
