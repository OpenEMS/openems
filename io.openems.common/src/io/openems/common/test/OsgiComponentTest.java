package io.openems.common.test;

import static io.openems.common.utils.ReflectionUtils.invokeMethodViaReflection;
import static io.openems.common.utils.ReflectionUtils.invokeMethodWithoutArgumentsViaReflection;
import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.utils.ReflectionUtils;
import io.openems.common.utils.StringUtils;

public class OsgiComponentTest<SUT> {

	public final SUT sut;

	/**
	 * References added by {@link #addReference}.
	 */
	public final Set<Object> references = new HashSet<>();

	protected Object config;

	public OsgiComponentTest(SUT sut) {
		this.sut = sut;
	}

	/**
	 * Calls the 'activate()' method of the 'system-under-test'.
	 *
	 * <p>
	 * If 'activate()' changes the configuration, the OSGi behavior is simulated by
	 * calling 'deactivate()' and then again 'activate()'
	 *
	 * @param config the configuration
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public OsgiComponentTest<SUT> activate(Object config) throws Exception {
		var configChangeCount = this.getConfigChangeCount();
		this.config = config;
		this.callActivate();

		if (configChangeCount != this.getConfigChangeCount()) {
			// Config change detected
			this.callModified();
		}

		return this;
	}

	/**
	 * Calls the 'modified()' method of the system-under-test with the given
	 * configuration.
	 *
	 * @param config the configuration
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public OsgiComponentTest<SUT> modified(AbstractComponentConfig config) throws Exception {
		// Add the configuration to ConfigurationAdmin
		for (Object object : this.references) {
			if (object instanceof DummyConfigurationAdmin cm) {
				cm.addConfig(config);
			}
		}

		this.config = config;
		this.callModified();

		return this;
	}

	/**
	 * Calls the 'deactivate()' method of the 'system-under-test'.
	 *
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public OsgiComponentTest<SUT> deactivate() throws Exception {
		this.callDeactivate();
		return this;
	}

	private int getConfigChangeCount() throws IOException, InvalidSyntaxException {
		var result = 0;
		for (var object : this.references) {
			if (object instanceof ConfigurationAdmin cm) {
				for (var config : cm.listConfigurations(null)) {
					result += config.getChangeCount();
				}
			}
		}
		return result;
	}

	private void callActivate() throws Exception {
		this.callActivateOrModified("activate");
	}

	protected Object resolveActivateArg(Parameter parameter) throws Exception {
		if (BundleContext.class.isAssignableFrom(parameter.getType())) {
			// BundleContext
			return null;
		}
		if (parameter.getType().isInstance(this.config)) {
			// Config
			return this.config;
		}

		throw new IllegalArgumentException("Unknown activate() parameter " + parameter);
	}

	private boolean callActivateOrModified(String methodName) throws Exception {
		Class<?> clazz = this.sut.getClass();
		var methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (!method.getName().equals(methodName)) {
				continue;
			}
			var args = new Object[method.getParameterCount()];
			for (var i = 0; i < method.getParameterCount(); i++) {
				var parameter = method.getParameters()[i];
				args[i] = this.resolveActivateArg(parameter);
			}
			invokeMethodViaReflection(this.sut, method, args);
			return true;
		}
		return false;
	}

	private void callModified() throws Exception {
		var hasModified = this.callActivateOrModified("modified");
		if (!hasModified) {
			// Has no modified() method -> Deactivate + recursive activate
			this.callDeactivate();
			this.activate(this.config);
		}
	}

	private void callDeactivate() throws IllegalArgumentException, SecurityException {
		invokeMethodWithoutArgumentsViaReflection(this.sut, "deactivate");
	}

	private boolean invokeSingleArgMethod(Class<?> clazz, String methodName, Object arg)
			throws ReflectionUtils.ReflectionException {
		var methods = ReflectionUtils.findMethod(clazz, methodName);
		for (var method : methods) {
			if (method.getParameterCount() != 1) {
				continue;
			}

			var parameter = method.getParameters()[0];
			if (!parameter.getType().isAssignableFrom(arg.getClass())) {
				continue;
			}

			invokeMethodViaReflection(this.sut, method, arg);
			return true;
		}

		// Unable to find matching method
		return false;
	}

	/**
	 * Adds a OSGi Declarative Services @Reference via java reflection.
	 *
	 * <p>
	 * Can also be used to set any other private field via reflection.
	 *
	 * @param memberName the name of the field or method
	 * @param object     the reference object
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public OsgiComponentTest<SUT> addReference(String memberName, Object object) throws Exception {
		// Set the reference recursively via reflection
		if (!this.addReference(this.sut.getClass(), memberName, object)) {
			throw new Exception("Unable to add reference on field or method [" + memberName + "]");
		}

		// Store reference
		this.references.add(object);

		return this;
	}

	private boolean addReference(Class<?> clazz, String memberName, Object object)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (this.bindValueToReferenceField(clazz, memberName, object)) {
			return true;
		}

		// Try method.
		if (this.invokeSingleArgMethod(clazz, memberName, object)) {
			return true;
		}

		// If we are here, no matching field or method was found. Search in parent
		// classes.
		Class<?> parent = clazz.getSuperclass();
		if (parent == null) {
			return false; // reached 'java.lang.Object'
		}
		return this.addReference(parent, memberName, object);
	}

	private boolean bindValueToReferenceField(Class<?> clazz, String memberName, Object value)
			throws IllegalArgumentException {
		Field field;
		try {
			field = ReflectionUtils.getField(clazz, memberName);
		} catch (ReflectionUtils.ReflectionException e) {
			return false;
		}

		setAttributeViaReflection(this.sut, memberName, value);

		var osgiReference = field.getAnnotation(Reference.class);
		if (osgiReference != null) {
			var bindMethodName = StringUtils.isNullOrEmpty(osgiReference.bind())
					? "bind" + StringUtils.capitalizeFirstLetter(memberName)
					: osgiReference.bind();
			this.invokeSingleArgMethod(clazz, bindMethodName, value);
		}

		return true;
	}
}
