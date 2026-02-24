package io.openems.common.logger;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A Logger implementation, that prefixes all log messages with a context provided by a Supplier. 
 * This allows for dynamic contexts that can change over time, such as the current session ID or user ID.
 */
public class LazyContextLogger extends PrefixedLogger {
		
	private final Supplier<String> context;
	
	public LazyContextLogger(Class<?> clazz, Supplier<String> context) {
		super(clazz);
		this.context = Objects.requireNonNull(context, "Context supplier must not be null");
	}
	
	public LazyContextLogger(String name, Supplier<String> context) {
		super(name);
		this.context = Objects.requireNonNull(context, "Context supplier must not be null");
	}
	
	@Override
	protected String prefix(String format) {
		String contextValue = this.context.get();
		if (contextValue == null || contextValue.isBlank()) {
			return format;
		}
		return "[" + contextValue + "] " + format;
	}
}
