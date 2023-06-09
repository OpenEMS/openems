package io.openems.backend.timedata.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Helper class for implementing a @Config-annotation within a Component-Test.
 */
public class AbstractComponentConfig {

	public static final boolean DEFAULT_ENABLED = true;

	private final Class<? extends Annotation> annotation;
	private final String id;

	public AbstractComponentConfig(Class<? extends Annotation> annotation, String id) {
		this.annotation = annotation;
		this.id = id;
	}

	/**
	 * Gets the annotation Type.
	 * 
	 * @return the {@link Class}
	 */
	public Class<? extends Annotation> annotationType() {
		return this.annotation;
	}

	/**
	 * Gets the Component-ID.
	 * 
	 * @return the Component-ID
	 */
	public String id() {
		return this.id;
	}

	/**
	 * Gets the Component Alias.
	 * 
	 * @return the alias
	 */
	public String alias() {
		return this.id;
	}

	/**
	 * Is this Component enabled?.
	 * 
	 * @return whether this component is enabled
	 */
	public boolean enabled() {
		return DEFAULT_ENABLED;
	}

	/**
	 * Gets the Apache Felix WebConsole configuration factory name hint.
	 * 
	 * @return a {@link String}
	 */
	public String webconsole_configurationFactory_nameHint() {
		return "";
	}

	/**
	 * Gets the configuration attributes in a format suitable for
	 * {@link ConfigurationAdmin} properties.
	 *
	 * @return the properties
	 * @throws IllegalAccessException    on error
	 * @throws IllegalArgumentException  on error
	 * @throws InvocationTargetException on error
	 */
	public Dictionary<String, Object> getAsProperties()
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Dictionary<String, Object> result = new Hashtable<>();

		// Default values
		result.put(Constants.SERVICE_PID, this.id);

		// Parse all class methods to get configuration properties
		for (Method method : this.getClass().getMethods()) {
			if (method.getDeclaringClass() != this.getClass()) {
				switch (method.getName()) {
				case "id", "alias", "enabled" -> {
					// these methods are specifically allowed
				}
				default -> {
					// This method is inherited, e.g. from java.lang.Object and not interesting
					continue;
				}
				}
			}
			if (method.getParameterCount() > 0) {
				// We are looking for methods with zero parameters
				continue;
			}
			if (Modifier.isStatic(method.getModifiers())) {
				// We are looking for non-static methods
				continue;
			}

			var key = method.getName().replace("_", ".");
			var value = method.invoke(this);
			if (value == null) {
				throw new IllegalArgumentException("Configuration for [" + key + "] is null");
			}
			result.put(key, value);
		}
		return result;
	}

}
