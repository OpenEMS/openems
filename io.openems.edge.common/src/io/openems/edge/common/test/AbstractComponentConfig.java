package io.openems.edge.common.test;

import java.lang.annotation.Annotation;

/**
 * Helper class for implementing a @Config-annotation within a Component-Test.
 */
public class AbstractComponentConfig {

	public static final String DEFAULT_SERVICE_PID = "service_pid";
	public static final boolean DEFAULT_ENABLED = true;

	private final Class<? extends Annotation> annotation;
	private final String id;

	public AbstractComponentConfig(Class<? extends Annotation> annotation, String id) {
		this.annotation = annotation;
		this.id = id;
	}

	public Class<? extends Annotation> annotationType() {
		return this.annotation;
	}

	public String service_pid() {
		return DEFAULT_SERVICE_PID;
	}

	public String id() {
		return this.id;
	}

	public boolean enabled() {
		return DEFAULT_ENABLED;
	}

	public String webconsole_configurationFactory_nameHint() {
		return "";
	}
	
}
