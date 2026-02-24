package io.openems.common.logger;

/**
 * A Logger implementation, that prefixes all log messages with a given context.
 */
public class ContextLogger extends PrefixedLogger {
	
	private final String context;
	
	@Override
	protected String prefix(String format) {
	    return this.context + format;
	}
	
	public ContextLogger(Class<?> clazz, String context) {
		super(clazz);
		this.context = "[" + context + "] ";
	}
	
	public ContextLogger(String name, String context) {
		super(name);
		this.context = "[" + context + "] ";
	}
}
