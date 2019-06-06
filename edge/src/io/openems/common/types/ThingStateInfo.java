package io.openems.common.types;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ TYPE })
public @interface ThingStateInfo {
	Class<?>[] reference();
	/* TODO Change to Class<? extends Thing> after migration to OSGi */
}
