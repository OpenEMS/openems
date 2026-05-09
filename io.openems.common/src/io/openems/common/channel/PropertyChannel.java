package io.openems.common.channel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PropertyChannel {

	/**
	 * The {@link PersistencePriority} for local persistence. Used by e.g. Edge
	 * Timedatas like RRD4J or (Local-)InfluxDb. Defines how often the value gets
	 * stored in the local databases.
	 * 
	 * @return the {@link PersistencePriority}
	 */
	PersistencePriority localPersistencePriority() default PersistencePriority.LOW;

	/**
	 * The {@link PersistencePriority} for remote persistence. Used by e.g.
	 * BackendApi (does not define how the value is stored in the backend only how
	 * often the value gets send).
	 * 
	 * @return the {@link PersistencePriority}
	 */
	PersistencePriority remotePersistencePriority() default PersistencePriority.LOW;

}
